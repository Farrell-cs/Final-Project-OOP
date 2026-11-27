package com.Ruinfall.frontend;

import com.Ruinfall.frontend.objects.Interactables.Barrels;
import com.Ruinfall.frontend.objects.Interactables.GrapplePoint;
import com.Ruinfall.frontend.objects.Interactables.RegeneratingStalactite;
import com.Ruinfall.frontend.objects.Interactables.Rock;
import com.Ruinfall.frontend.objects.Interactables.Stalactite;
import com.Ruinfall.frontend.objects.bullets.Bullet;
import com.Ruinfall.frontend.objects.enemies.WaveMantis;
import com.Ruinfall.frontend.objects.enemies.WormBoss;
import com.Ruinfall.frontend.objects.hazards.SlimeHazard;
import com.Ruinfall.frontend.objects.items.Item;
import com.Ruinfall.frontend.objects.items.ItemType;
import com.Ruinfall.frontend.states.SwingState;
import com.Ruinfall.frontend.systems.AssetManager;
import com.Ruinfall.frontend.systems.BulletManager;
import com.Ruinfall.frontend.systems.GameEventBus;
import com.Ruinfall.frontend.visuals.CameraFollow;
import com.Ruinfall.frontend.visuals.CaveAtmosphereRenderer;
import com.Ruinfall.frontend.visuals.EffectManager;
import com.Ruinfall.frontend.visuals.ParticleSystem;
import com.Ruinfall.frontend.visuals.PlayerVisuals;
import com.Ruinfall.frontend.visuals.ProceduralTextures;
import com.Ruinfall.frontend.visuals.VfxManager;
import com.Ruinfall.frontend.visuals.WorldHintRenderer;
import com.Ruinfall.frontend.world.BoxRoom;
import com.Ruinfall.frontend.world.LushCaveStage;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Main extends ApplicationAdapter {
    private static final float FIXED_DT = 1.0f / 60.0f;

    private SpriteBatch batch;
    private Texture blockTexture;

    private OrthographicCamera camera;
    private Viewport viewport;

    private PlayerController player;
    private PlayerVisuals visuals;
    private CameraFollow cameraFollow;
    private LushCaveStage stage;
    private WaveMantis waveMantis;
    private VfxManager vfx;
    private ParticleSystem particles;
    private CaveAtmosphereRenderer atmosphere;
    private WorldHintRenderer hintRenderer;

    // Stage 2 (Gilchrist's WormBoss / Fake Dragon arena, entered via Lava Shaft drop)
    private boolean inStage2 = false;
    private BoxRoom stage2Arena;
    private WormBoss wormBoss;
    private BulletManager bulletManager;
    private final List<GrapplePoint> stage2Grapples = new ArrayList<>();
    private final List<RegeneratingStalactite> stage2Stalactites = new ArrayList<>();
    private final List<Barrels> stage2Barrels = new ArrayList<>();
    private float stage2SwingBossCooldown = 0f;

    private final List<Item> activeItems = new ArrayList<>();
    private final Map<Stalactite, Float> arenaStalactiteRespawnTimers = new HashMap<>();

    private float accumulator;
    private float playerHurtCooldown = 0f;
    private float bossParticleTimer = 0f;
    private float stalactiteDustTimer = 0f;
    private boolean wasGrappling = false;
    private GrapplePoint lastEngagedGrapple = null;
    private boolean handlingControlledRespawn = false;

    // Banner state for boss defeat / reward pickup / retry
    private String bannerTitle = "";
    private String bannerSubtitle = "";
    private float bannerTimer = 0f;
    private float bannerDuration = 3.5f;

    @Override
    public void create() {
        batch = new SpriteBatch();
        blockTexture = ProceduralTextures.getBlockTexture();

        camera = new OrthographicCamera();
        viewport = new ExtendViewport(24.0f, 14.0f, camera);

        stage = new LushCaveStage();
        player = new PlayerController();
        LushCaveStage.Room startRoom = stage.findRoomAt(-10.5f, 5.2f);
        player.getMotor().getPosition().set(startRoom.spawnX, startRoom.spawnY);
        player.setSpawnPosition(startRoom.spawnX, startRoom.spawnY);
        player.resetHealth();
        player.getMotor().setSolids(stage.getBlocks());

        waveMantis = new WaveMantis(92.0f, 0.0f);
        vfx = new VfxManager();
        particles = new ParticleSystem();
        atmosphere = new CaveAtmosphereRenderer();
        hintRenderer = new WorldHintRenderer();

        // Subscribe to GameEventBus
        GameEventBus bus = GameEventBus.getInstance();
        bus.subscribe(GameEventBus.GameEvent.ROCK_SHATTERED, (event, payload) -> {
            if (payload instanceof Item item) {
                activeItems.add(item);
                emitShatterBurst(item.getX() + 0.3f, item.getY() + 0.3f, new Color(0.55f, 0.58f, 0.54f, 0.9f), 12);
            }
        });
        bus.subscribe(GameEventBus.GameEvent.CRYSTAL_SHATTERED, (event, payload) -> {
            if (payload instanceof Item item) {
                activeItems.add(item);
                emitSparkBurst(item.getX() + 0.3f, item.getY() + 0.3f, new Color(0.35f, 0.96f, 0.92f, 1f), Color.WHITE, 16);
            }
        });
        bus.subscribe(GameEventBus.GameEvent.STALACTITE_TRIGGERED, (event, payload) -> {
            if (payload instanceof Stalactite s) {
                emitShatterBurst(s.getX() + s.getWidth() * 0.5f, s.getY() + s.getHeight() * 0.8f,
                        new Color(0.52f, 0.60f, 0.56f, 0.85f), 6);
            }
        });
        bus.subscribe(GameEventBus.GameEvent.STALACTITE_LANDED, (event, payload) -> {
            if (payload instanceof Stalactite s) {
                emitShatterBurst(s.getX() + s.getWidth() * 0.5f, Math.max(0.1f, s.getY()),
                        new Color(0.45f, 0.52f, 0.48f, 0.95f), 14);
            }
        });
        bus.subscribe(GameEventBus.GameEvent.ROCK_BROKEN, (event, payload) -> {
            if (payload instanceof String hitInfo && hitInfo.startsWith("hit:")) {
                String[] parts = hitInfo.substring(4).split(",");
                if (parts.length == 2) {
                    try {
                        float hx = Float.parseFloat(parts[0]) + 0.6f;
                        float hy = Float.parseFloat(parts[1]) + 0.6f;
                        emitShatterBurst(hx, hy, new Color(0.58f, 0.62f, 0.56f, 0.9f), 6);
                    } catch (NumberFormatException ignored) {}
                }
            } else if (payload instanceof Rock rock) {
                activeItems.addAll(rock.getDrops());
                emitShatterBurst(rock.getX() + rock.getWidth() * 0.5f, rock.getY() + rock.getHeight() * 0.5f,
                        new Color(0.54f, 0.58f, 0.52f, 0.95f), 16);
            }
        });
        bus.subscribe(GameEventBus.GameEvent.BARREL_EXPLODED, (event, payload) -> {
            if (payload instanceof Barrels.BarrelExplosionPayload p) {
                activeItems.addAll(p.items);
                emitShatterBurst(p.x + 0.7f, p.y + 0.8f, new Color(0.62f, 0.42f, 0.20f, 0.95f), 14);
                emitSparkBurst(p.x + 0.7f, p.y + 0.8f, new Color(1.0f, 0.62f, 0.18f, 1f), new Color(1.0f, 0.25f, 0.08f, 0f), 16);
            }
        });
        bus.subscribe(GameEventBus.GameEvent.SWING_EXECUTED, (event, payload) -> {
            if (payload instanceof SwingState swing) {
                Vector2 pos = swing.getHitboxOffset();
                Vector2 dir = swing.getSwingVector();
                float baseAngle = MathUtils.atan2(dir.y, dir.x);
                for (int i = 0; i < 7; i++) {
                    float angle = baseAngle + MathUtils.random(-0.75f, 0.75f);
                    float spd = MathUtils.random(3.5f, 8.0f);
                    particles.emitSpark(
                            pos.x + MathUtils.random(-0.25f, 0.25f),
                            pos.y + MathUtils.random(-0.25f, 0.25f),
                            MathUtils.cos(angle) * spd, MathUtils.sin(angle) * spd,
                            MathUtils.random(0.10f, 0.17f),
                            MathUtils.random(0.14f, 0.24f),
                            new Color(1.0f, 0.95f, 0.45f, 0.95f),
                            new Color(1.0f, 1.0f, 1.0f, 0f),
                            true
                    );
                }
            }
        });
        bus.subscribe(GameEventBus.GameEvent.POGO_BOUNCE, (event, payload) -> {
            float ppx = player.getMotor().getPosition().x;
            float ppy = player.getMotor().getPosition().y;
            emitSparkBurst(ppx, ppy, new Color(1.0f, 0.92f, 0.42f, 1f), new Color(0.42f, 0.96f, 0.95f, 0f), 12);
        });
        bus.subscribe(GameEventBus.GameEvent.BOSS_SHOCKWAVE, (event, payload) -> {
            if (payload instanceof WaveMantis boss) {
                cameraFollow.addTrauma(0.38f);
                vfx.triggerImpactRing(boss.getCenterX(), 0.4f, new Color(0.3f, 0.95f, 0.85f, 1f), 4.2f, 0.35f);
                emitSparkBurst(boss.getCenterX(), 0.4f, new Color(0.32f, 0.96f, 0.88f, 1f), Color.WHITE, 14);
            }
        });
        bus.subscribe(GameEventBus.GameEvent.BOSS_PARRIED, (event, payload) -> {
            if (payload instanceof WaveMantis boss) {
                vfx.triggerParryFlash(boss.getCenterX(), boss.getCenterY());
                cameraFollow.addTrauma(0.68f);
                emitSparkBurst(boss.getCenterX(), boss.getCenterY(), new Color(1.0f, 0.92f, 0.42f, 1f), Color.WHITE, 26);
            }
        });
        bus.subscribe(GameEventBus.GameEvent.BOSS_DAZED, (event, payload) -> {
            if (payload instanceof WaveMantis boss) {
                cameraFollow.addTrauma(0.55f);
                vfx.triggerImpactRing(boss.getCenterX(), boss.getCenterY(), new Color(0.85f, 0.65f, 1.0f, 1f), 3.0f, 0.3f);
                emitShatterBurst(boss.getCenterX(), boss.getCenterY(), new Color(0.45f, 0.52f, 0.48f, 0.9f), 12);
                emitSparkBurst(boss.getCenterX(), boss.getCenterY(), new Color(0.85f, 0.68f, 1.0f, 1f), new Color(1f, 0.95f, 0.45f, 0f), 14);
            }
        });
        bus.subscribe(GameEventBus.GameEvent.BOSS_DEFEATED, (event, payload) -> {
            if (payload instanceof WaveMantis boss) {
                vfx.triggerHitStop(0.18f);
                vfx.triggerScreenFlash(new Color(1.0f, 0.95f, 0.7f, 1.0f), 0.65f, 0.35f);
                vfx.triggerImpactRing(boss.getCenterX(), boss.getCenterY(), Color.GOLD, 6.0f, 0.5f);
                cameraFollow.addTrauma(1.0f);
                emitSparkBurst(boss.getCenterX(), boss.getCenterY(), Color.GOLD, new Color(0.38f, 0.96f, 0.72f, 0f), 38);

                // Drop MANTIS_CLAW reward & open Lava Shaft
                activeItems.add(new Item(boss.getCenterX() - 0.3f, 1.4f, ItemType.MANTIS_CLAW));
                stage.unlockArenaAndOpenLavaShaft();
                player.getMotor().setSolids(stage.getBlocks());
                showBanner("WAVE MANTIS DEFEATED", "CLAIM THE MANTIS CLAW", 3.5f);
            }
        });
        bus.subscribe(GameEventBus.GameEvent.ITEM_COLLECTED, (event, payload) -> {
            if (payload instanceof Item item) {
                Color c = (item.getItemTypeEnum() == ItemType.MANTIS_CLAW)
                        ? new Color(0.35f, 1.0f, 0.72f, 1.0f)
                        : (item.getItemTypeEnum() == ItemType.LIFE ? new Color(1.0f, 0.42f, 0.52f, 1.0f) : Color.GOLD);
                emitSparkBurst(item.getX() + 0.3f, item.getY() + 0.3f, c, Color.WHITE, 16);
                if (item.getItemTypeEnum() == ItemType.MANTIS_CLAW) {
                    vfx.triggerHitStop(0.08f);
                    vfx.triggerScreenFlash(new Color(0.45f, 1.0f, 0.78f, 1.0f), 0.45f, 0.3f);
                    cameraFollow.addTrauma(0.4f);
                    showBanner("MANTIS CLAW ACQUIRED", "LAVA SHAFT UNSEALED - DROP DOWN TO CONTINUE", 4.5f);
                }
            }
        });

        visuals = new PlayerVisuals(player);
        visuals.setParticles(particles);
        cameraFollow = new CameraFollow(camera, player);
        cameraFollow.setInitialRoom(startRoom);
        EffectManager.getInstance().bindRuntime(particles, vfx, cameraFollow);

        player.setEventListener(new PlayerController.PlayerEventListener() {
            @Override
            public void onJumped(PlayerController.JumpKind kind) {
                visuals.onJumped(kind);
            }

            @Override
            public void onDashed(Vector2 dir) {
                visuals.onDashed(dir);
                cameraFollow.triggerShake(dir);
                float px = player.getMotor().getPosition().x;
                float py = player.getMotor().getPosition().y + player.getStats().normalColliderSize.y * 0.5f;
                vfx.triggerDashRipple(px, py);
            }

            @Override
            public void onLanded(float impactSpeed) {
                visuals.onLanded(impactSpeed);
                if (impactSpeed > 18.0f) {
                    cameraFollow.addTrauma(0.22f);
                }
            }

            @Override
            public void onRespawned() {
                if (!handlingControlledRespawn) {
                    float rpx = player.getMotor().getPosition().x;
                    float rpy = player.getMotor().getPosition().y + player.getStats().normalColliderSize.y * 0.5f;
                    LushCaveStage.Room room = stage.findRoomAt(rpx, rpy);
                    resetRoomState(room);
                    if (room != null && room.minX >= LushCaveStage.ARENA_MIN_X && waveMantis.isAlive()
                            && waveMantis.getState() != WaveMantis.State.DEFEATED) {
                        waveMantis.reset();
                        stage.resetArena();
                        player.getMotor().setSolids(stage.getBlocks());
                        player.resetHealth();
                        playerHurtCooldown = 0f;
                    }
                    if (room != null && room != cameraFollow.getCurrentRoom()) {
                        cameraFollow.setInitialRoom(room);
                    }
                }
                visuals.onRespawned();
                cameraFollow.snapToTarget();
            }
        });
    }

    private void showBanner(String title, String subtitle, float duration) {
        this.bannerTitle = title;
        this.bannerSubtitle = subtitle;
        this.bannerDuration = duration;
        this.bannerTimer = duration;
    }

    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();
        if (!vfx.isDeathActive()) {
            player.updateInput();
        }

        if (bannerTimer > 0f) {
            bannerTimer = Math.max(0f, bannerTimer - dt);
        }
        if (playerHurtCooldown > 0f) {
            playerHurtCooldown = Math.max(0f, playerHurtCooldown - dt);
        }

        // Hit-stop, room transition, or death burst freezes simulation while keeping camera & VFX crisp
        if (!vfx.updateHitStop(dt) && !cameraFollow.isTransitioning() && !vfx.isDeathActive()) {
            accumulator += Math.min(dt, 0.25f);
            while (accumulator >= FIXED_DT) {
                if (inStage2) {
                    stepStage2Simulation(FIXED_DT);
                } else {
                    stepSimulation(FIXED_DT);
                }
                accumulator -= FIXED_DT;
            }
        }

        vfx.update(dt);
        EffectManager.getInstance().update(dt);
        if (!inStage2) {
            stage.update(dt);
        }
        atmosphere.update(dt);
        atmosphere.updateParticles(dt, camera, stage, particles);
        particles.update(dt);
        hintRenderer.update(dt);
        visuals.update(dt);
        cameraFollow.update(dt);

        // Deep forest-teal cavern backdrop
        ScreenUtils.clear(0.03f, 0.08f, 0.10f, 1.0f);

        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // Procedural parallax cave silhouettes, glowing crystals & hanging vines
        atmosphere.render(batch, blockTexture, camera);

        float px = player.getMotor().getPosition().x;
        float py = player.getMotor().getPosition().y + player.getStats().normalColliderSize.y * 0.5f;

        if (inStage2) {
            renderStage2World(px, py);
        } else {
            stage.render(batch, blockTexture);

            for (GrapplePoint gp : stage.getGrapplePoints()) {
                gp.render(batch, blockTexture);
                gp.renderTether(batch, blockTexture, px, py);
            }

            // Render stalactites
            for (Stalactite s : stage.getStalactites()) {
                if (s.isActive()) {
                    batch.setColor(s.getColor() != null ? s.getColor() : Color.GRAY);
                    batch.draw(blockTexture, s.getX(), s.getY(), s.getWidth(), s.getHeight());
                }
            }

            // Render breakables
            for (Rock r : stage.getBreakables()) {
                if (r.isActive()) {
                    batch.setColor(r.getColor() != null ? r.getColor() : Color.DARK_GRAY);
                    batch.draw(blockTexture, r.getX(), r.getY(), r.getWidth(), r.getHeight());
                }
            }

            // Render items
            for (Item item : activeItems) {
                if (item.isActive()) {
                    Color itemColor = (item.getItemTypeEnum() == ItemType.MANTIS_CLAW)
                            ? new Color(0.35f, 1.0f, 0.72f, 1.0f)
                            : (item.getColor() != null ? item.getColor() : Color.YELLOW);
                    batch.setColor(itemColor);
                    batch.draw(blockTexture, item.getX(), item.getY(), item.getWidth(), item.getHeight());
                }
            }
            batch.setColor(Color.WHITE);

            // Render Wave Mantis boss & shockwaves
            waveMantis.render(batch, blockTexture);
        }

        // Render player (hidden during Celeste death burst)
        if (!vfx.isDeathActive()) {
            visuals.render(batch, blockTexture);
        }

        // Render pooled procedural Celeste-style particles (spores, dust, sparks, embers)
        particles.render(batch, camera);

        batch.end();

        // Warm golden-amber diagonal light shafts across the mossy cavern
        atmosphere.renderLightShafts(camera);

        // Render procedural world-space VFX (parry starburst, shockwave rings)
        vfx.renderWorld(camera);

        // Render world-space tutorial hints (Stage 1)
        if (!inStage2) {
            hintRenderer.render(batch, camera, stage.getHints(), px, py);
        }

        // Render Player HP & Active Boss HP HUD
        boolean bossAwake = inStage2
                ? (wormBoss != null && wormBoss.isAlive())
                : (waveMantis.isAlive() && waveMantis.getState() != WaveMantis.State.SLEEPING);
        int bossHp = inStage2 ? (wormBoss != null ? wormBoss.getHp() : 0) : waveMantis.getHp();
        int bossMaxHp = inStage2 ? (wormBoss != null ? wormBoss.getMaxHP() : 1) : waveMantis.getMaxHP();
        String bossName = inStage2 ? (wormBoss != null ? wormBoss.getName() : "Fake Dragon") : waveMantis.getName();
        hintRenderer.renderHud(
                batch,
                player.getHp(), player.getMaxHp(),
                player.getDashes().getCurrentDashes(), player.getDashes().getMaxDashes(),
                bossAwake, bossHp, bossMaxHp, bossName
        );

        // Render banner if active
        if (bannerTimer > 0f) {
            float alpha = Math.min(1.0f, bannerTimer / Math.min(0.6f, bannerDuration));
            hintRenderer.renderBanner(batch, bannerTitle, bannerSubtitle, alpha);
        }

        // Render full-screen parry / impact flash overlay
        vfx.renderScreenOverlay();
    }

    private void stepSimulation(float dt) {
        float px = player.getMotor().getPosition().x;
        float py = player.getMotor().getPosition().y + player.getStats().normalColliderSize.y * 0.5f;

        // Discrete Celeste room transition check & spawn sync
        LushCaveStage.Room currentRoom = stage.findRoomAt(px, py);
        if (currentRoom != null) {
            player.setSpawnPosition(currentRoom.spawnX, currentRoom.spawnY);
            if (currentRoom != cameraFollow.getCurrentRoom()) {
                cameraFollow.transitionToRoom(currentRoom);
            }
        }

        // Trigger boss arena when player steps inside
        if (!stage.isArenaLocked() && waveMantis.getState() == WaveMantis.State.SLEEPING && px >= LushCaveStage.ARENA_TRIGGER_X) {
            stage.lockArena();
            player.getMotor().setSolids(stage.getBlocks());
            waveMantis.activate();
            cameraFollow.addTrauma(0.35f);
            showBanner("WAVE MANTIS", "GUARDIAN OF THE LUSH CAVE", 2.6f);
        }

        // Update grapple points & select nearest in-range
        GrapplePoint nearestGrapple = null;
        float bestDist = Float.MAX_VALUE;
        for (GrapplePoint gp : stage.getGrapplePoints()) {
            gp.update(dt);
            gp.checkPlayerRange(px, py);
            if (gp.canGrapple()) {
                float d = (float) Math.hypot(px - gp.getX(), py - gp.getY());
                if (d < bestDist) {
                    bestDist = d;
                    nearestGrapple = gp;
                }
            }
        }
        if (nearestGrapple != null) {
            player.setNearbyGrapplePoint(nearestGrapple);
        } else if (player.getNearbyGrapplePoint() != null
                && player.getNearbyGrapplePoint().getState() != GrapplePoint.GrappleState.ACTIVE) {
            player.setNearbyGrapplePoint(null);
        }

        // Track grapple latch & release particle bursts
        boolean isGrappling = player.getStateMachine().getCurrentState() == player.getGrappleState();
        if (isGrappling && !wasGrappling) {
            lastEngagedGrapple = player.getGrappleState().getActiveTarget();
            emitSparkBurst(px, py, new Color(0.32f, 0.96f, 1.0f, 1f), Color.WHITE, 12);
            if (lastEngagedGrapple != null) {
                emitSparkBurst(lastEngagedGrapple.getCenter().x, lastEngagedGrapple.getCenter().y,
                        new Color(1.0f, 0.92f, 0.42f, 1f), new Color(0.32f, 0.96f, 1.0f, 0f), 10);
            }
        } else if (!isGrappling && wasGrappling) {
            emitSparkBurst(px, py, new Color(0.55f, 1.0f, 0.85f, 1f), Color.WHITE, 10);
            lastEngagedGrapple = null;
        }
        wasGrappling = isGrappling;

        // Update stalactites, warning dust trickle & respawn arena stalactites while boss is alive
        stalactiteDustTimer -= dt;
        boolean emitStalactiteDust = stalactiteDustTimer <= 0f;
        if (emitStalactiteDust) stalactiteDustTimer = 0.055f;

        for (Stalactite s : stage.getStalactites()) {
            if (!s.isActive()) {
                if (s.getInitialX() >= LushCaveStage.ARENA_MIN_X && waveMantis.isAlive()) {
                    float timer = arenaStalactiteRespawnTimers.getOrDefault(s, 4.5f) - dt;
                    if (timer <= 0f) {
                        s.reset();
                        arenaStalactiteRespawnTimers.remove(s);
                    } else {
                        arenaStalactiteRespawnTimers.put(s, timer);
                    }
                }
                continue;
            }
            if (!s.isTriggered() && Math.abs(px - (s.getX() + s.getWidth() * 0.5f)) < 1.8f && py < s.getY()) {
                s.trigger();
            }
            s.update(dt);
            if (emitStalactiteDust && s.isTriggered() && !s.isFalling()) {
                particles.emitDust(
                        s.getX() + MathUtils.random(0.05f, s.getWidth() - 0.05f),
                        s.getY(),
                        MathUtils.random(-0.5f, 0.5f), MathUtils.random(-2.2f, -0.8f),
                        MathUtils.random(0.10f, 0.16f), MathUtils.random(0.25f, 0.42f),
                        new Color(0.55f, 0.62f, 0.58f, 0.78f)
                );
            }
        }

        // Update breakables
        for (Rock r : stage.getBreakables()) {
            if (r.isActive()) {
                r.update(dt);
            }
        }

        // Update items
        for (Item item : activeItems) {
            if (item.isActive()) {
                item.update(dt);
            }
        }

        // Update Wave Mantis & emit boss state particles
        waveMantis.updateBoss(dt, px, py);
        emitWaveMantisParticles(dt);

        // Update player physics
        player.stepPhysics(dt);

        // Check Lava Shaft transition -> Stage 2 (Gilchrist's WormBoss / Fake Dragon arena)
        if (stage.isLavaShaftOpen() && player.getMotor().getPosition().y < -7.5f) {
            enterStage2WormBoss();
            return;
        }

        checkCollisions();
    }

    private void enterStage2WormBoss() {
        inStage2 = true;
        stage2Arena = new BoxRoom();
        wormBoss = new WormBoss("Fake Dragon", 300);
        bulletManager = new BulletManager();

        stage2Grapples.clear();
        stage2Grapples.addAll(stage2Arena.createGrappleNetwork());

        stage2Stalactites.clear();
        float[] xs = {-12.0f, 0.0f, 12.0f};
        for (float sx : xs) {
            RegeneratingStalactite rs = new RegeneratingStalactite(sx, 12.6f);
            rs.configureRandomization(1.1f, 0.25f, 0.90f, 3.5f, 6.5f, 1.0f);
            stage2Stalactites.add(rs);
        }

        stage2Barrels.clear();
        stage2Barrels.add(new Barrels(-8.0f, 0.0f));
        stage2Barrels.add(new Barrels(8.0f, 0.0f));

        activeItems.clear();
        particles.clear();
        EffectManager.getInstance().clearAll();

        player.getMotor().setSolids(stage2Arena.getBlocks());
        player.resetHealth();
        player.respawnAt(0.0f, 3.2f);
        player.setSpawnPosition(0.0f, 3.2f);
        playerHurtCooldown = 0.6f;

        Rectangle bounds = stage2Arena.getArenaBounds();
        LushCaveStage.Room stage2Room = new LushCaveStage.Room(
                "Crystal Cave",
                bounds.x, bounds.x + bounds.width,
                bounds.y, bounds.y + bounds.height,
                0.0f, 3.2f, false
        );
        cameraFollow.setInitialRoom(stage2Room);
        cameraFollow.snapToTarget();
        vfx.triggerScreenFlash(new Color(0.85f, 0.55f, 1.0f, 1f), 0.45f, 0.30f);
        showBanner("CRYSTAL CAVE", "FAKE DRAGON AWAKENS", 3.5f);
    }

    private void stepStage2Simulation(float dt) {
        if (stage2SwingBossCooldown > 0f) {
            stage2SwingBossCooldown = Math.max(0f, stage2SwingBossCooldown - dt);
        }

        float px = player.getMotor().getPosition().x;
        float py = player.getMotor().getPosition().y + player.getStats().normalColliderSize.y * 0.5f;

        // Nearest grapple point across Gilchrist's grapple network
        GrapplePoint nearest = null;
        float bestDist = Float.MAX_VALUE;
        for (GrapplePoint gp : stage2Grapples) {
            gp.update(dt);
            gp.checkPlayerRange(px, py);
            if (gp.canGrapple()) {
                float d = (float) Math.hypot(px - gp.getX(), py - gp.getY());
                if (d < bestDist) {
                    bestDist = d;
                    nearest = gp;
                }
            }
        }
        if (nearest != null) {
            player.setNearbyGrapplePoint(nearest);
        } else if (player.getNearbyGrapplePoint() != null
                && player.getNearbyGrapplePoint().getState() != GrapplePoint.GrappleState.ACTIVE) {
            player.setNearbyGrapplePoint(null);
        }

        for (RegeneratingStalactite rs : stage2Stalactites) {
            if (rs.isActive() && !rs.isTriggered()
                    && Math.abs(px - (rs.getX() + rs.getWidth() * 0.5f)) < 1.8f && py < rs.getY()) {
                rs.trigger();
            }
            rs.update(dt);
        }

        for (Barrels b : stage2Barrels) {
            if (b.isActive()) b.update(dt);
        }
        for (Item item : activeItems) {
            if (item.isActive()) item.update(dt);
        }

        if (wormBoss != null && wormBoss.isAlive()) {
            wormBoss.updateBoss(dt, player, bulletManager);
        }
        bulletManager.update(dt);
        player.stepPhysics(dt);

        checkStage2Collisions();
    }

    private void checkStage2Collisions() {
        Rectangle playerCore = player.getCoreHitbox();
        boolean isSwinging = player.getStateMachine().getCurrentState() == player.getSwingState();
        Vector2 swingHitboxPos = isSwinging ? player.getSwingState().getHitboxOffset() : null;
        Rectangle swingRect = isSwinging
                ? new Rectangle(swingHitboxPos.x - 0.7f, swingHitboxPos.y - 0.7f, 1.4f, 1.4f)
                : null;

        // Slime hazards
        boolean inSlime = false;
        if (wormBoss != null) {
            for (SlimeHazard slime : wormBoss.getSlimeHazards()) {
                if (slime.isActive() && slime.getCoreHitbox().overlaps(playerCore)) {
                    slime.applyTo(player);
                    inSlime = true;
                }
            }
        }
        if (!inSlime) {
            SlimeHazard.clearFrom(player);
        }

        // Stalactites vs WormBoss & Player
        for (RegeneratingStalactite rs : stage2Stalactites) {
            if (!rs.isActive()) continue;
            Rectangle sBounds = rs.getCoreHitbox();
            if (rs.isFalling() && wormBoss != null && wormBoss.isAlive()) {
                boolean hitBoss = false;
                for (Rectangle hb : wormBoss.getActiveHitboxes()) {
                    if (sBounds.overlaps(hb)) {
                        hitBoss = true;
                        break;
                    }
                }
                if (hitBoss) {
                    boolean killed = wormBoss.takeDamage(35);
                    vfx.triggerHitStop(0.10f);
                    cameraFollow.addTrauma(0.65f);
                    rs.onGroundHit();
                    if (killed) {
                        wormBoss.cleanupHazards(bulletManager, player);
                        showBanner("FAKE DRAGON DEFEATED", "STAGE 2 COMPLETE", 4.5f);
                    }
                    continue;
                }
            }
            if (swingRect != null && swingRect.overlaps(sBounds)) {
                if (!rs.isTriggered()) rs.trigger();
                else if (rs.isPogoable()) player.getSwingState().onSwingHit(true);
            }
        }

        // Swing vs Barrels & WormBoss
        for (Barrels b : stage2Barrels) {
            if (b.isActive() && swingRect != null && swingRect.overlaps(b.getCoreHitbox())) {
                b.onCollision(player);
            }
        }
        if (swingRect != null && stage2SwingBossCooldown <= 0f && wormBoss != null && wormBoss.isAlive()) {
            for (Rectangle hb : wormBoss.getActiveHitboxes()) {
                if (swingRect.overlaps(hb)) {
                    stage2SwingBossCooldown = 0.25f;
                    boolean killed = wormBoss.takeDamage(12);
                    vfx.triggerHitStop(0.04f);
                    cameraFollow.addTrauma(0.25f);
                    player.getSwingState().onSwingHit(false);
                    if (killed) {
                        wormBoss.cleanupHazards(bulletManager, player);
                        showBanner("FAKE DRAGON DEFEATED", "STAGE 2 COMPLETE", 4.5f);
                    }
                    break;
                }
            }
        }

        // Enemy bullets & WormBoss body vs Player
        if (playerHurtCooldown <= 0f) {
            for (Bullet b : new ArrayList<>(bulletManager.getActiveBullets())) {
                if (b.isActive() && !b.isPlayerBullet() && playerCore.overlaps(b.getCoreHitbox())) {
                    bulletManager.recycleBullet(b);
                    onStage2PlayerHit(b.getX());
                    return;
                }
            }
            if (wormBoss != null && wormBoss.isAlive()) {
                for (Rectangle hb : wormBoss.getActiveHitboxes()) {
                    if (playerCore.overlaps(hb)) {
                        onStage2PlayerHit(hb.x + hb.width * 0.5f);
                        return;
                    }
                }
            }
        }

        // Items
        for (Item item : activeItems) {
            if (item.isActive() && playerCore.overlaps(item.getCoreHitbox())) {
                item.onCollision(player);
            }
        }
    }

    private void onStage2PlayerHit(float sourceX) {
        playerHurtCooldown = 0.85f;
        player.takeDamage(1);
        boolean died = !player.isAlive();
        vfx.triggerHitStop(0.07f);
        vfx.triggerScreenFlash(new Color(0.95f, 0.2f, 0.25f, 1.0f), 0.35f, 0.2f);
        cameraFollow.addTrauma(0.55f);
        if (died) {
            showBanner("DEFEATED", "RETRYING FAKE DRAGON", 2.2f);
            vfx.triggerDeathBurst(player.getMotor().getPosition().x, player.getMotor().getPosition().y + 0.7f,
                    this::enterStage2WormBoss);
        } else {
            float pushDir = (player.getMotor().getPosition().x < sourceX) ? -1.0f : 1.0f;
            player.getVelocity().set(pushDir * 9.0f, 7.5f);
        }
    }

    private void renderStage2World(float px, float py) {
        stage2Arena.render(batch, blockTexture);

        for (GrapplePoint gp : stage2Grapples) {
            gp.render(batch, blockTexture);
            gp.renderTether(batch, blockTexture, px, py);
        }
        for (RegeneratingStalactite rs : stage2Stalactites) {
            if (rs.isActive()) {
                batch.setColor(rs.getColor() != null ? rs.getColor() : Color.GRAY);
                batch.draw(blockTexture, rs.getX(), rs.getY(), rs.getWidth(), rs.getHeight());
            }
        }
        for (Barrels b : stage2Barrels) {
            if (b.isActive()) {
                batch.setColor(b.getColor() != null ? b.getColor() : Color.BROWN);
                batch.draw(blockTexture, b.getX(), b.getY(), b.getWidth(), b.getHeight());
            }
        }
        for (Item item : activeItems) {
            if (item.isActive()) {
                batch.setColor(item.getColor() != null ? item.getColor() : Color.YELLOW);
                batch.draw(blockTexture, item.getX(), item.getY(), item.getWidth(), item.getHeight());
            }
        }
        if (wormBoss != null) {
            for (SlimeHazard slime : wormBoss.getSlimeHazards()) {
                if (slime.isActive()) {
                    batch.setColor(0.35f, 0.92f, 0.28f, 0.75f);
                    batch.draw(blockTexture, slime.getX(), slime.getY(), slime.getWidth(), slime.getHeight());
                }
            }
            if (wormBoss.isAlive()) {
                wormBoss.render(batch);
            }
        }
        if (bulletManager != null) {
            for (Bullet b : bulletManager.getActiveBullets()) {
                if (b.isActive()) {
                    batch.setColor(b.getColor() != null ? b.getColor() : Color.ORANGE);
                    batch.draw(blockTexture, b.getX(), b.getY(), b.getWidth(), b.getHeight());
                }
            }
        }
        batch.setColor(Color.WHITE);
    }

    private void emitWaveMantisParticles(float dt) {
        if (!waveMantis.isAlive() || waveMantis.getState() == WaveMantis.State.SLEEPING) return;

        bossParticleTimer -= dt;
        boolean tick = bossParticleTimer <= 0f;
        if (tick) bossParticleTimer = 0.045f;

        WaveMantis.State st = waveMantis.getState();
        if (tick) {
            if (st == WaveMantis.State.DASH_WINDUP) {
                float angle = MathUtils.random(0f, MathUtils.PI2);
                float dist = MathUtils.random(1.2f, 2.4f);
                float sx = waveMantis.getCenterX() + MathUtils.cos(angle) * dist;
                float sy = waveMantis.getCenterY() + MathUtils.sin(angle) * dist;
                particles.emitSpark(
                        sx, sy,
                        (waveMantis.getCenterX() - sx) * 3.5f, (waveMantis.getCenterY() - sy) * 3.5f,
                        MathUtils.random(0.11f, 0.18f), 0.25f,
                        new Color(1.0f, 0.85f, 0.28f, 0.95f), Color.WHITE, true
                );
            } else if (st == WaveMantis.State.DASH_ATTACK) {
                for (int i = 0; i < 2; i++) {
                    particles.emitSpark(
                            waveMantis.getCenterX() + MathUtils.random(-0.5f, 0.5f),
                            waveMantis.getY() + MathUtils.random(0.2f, waveMantis.getHeight() - 0.2f),
                            MathUtils.random(-3.5f, 3.5f), MathUtils.random(0.5f, 2.5f),
                            MathUtils.random(0.13f, 0.22f), 0.24f,
                            new Color(1.0f, 0.48f, 0.18f, 0.95f), new Color(1.0f, 0.90f, 0.35f, 0f),
                            true
                    );
                }
            } else if (st == WaveMantis.State.DAZED) {
                float orbitAngle = (System.nanoTime() * 1e-9f) * 6.5f;
                float ox = waveMantis.getCenterX() + MathUtils.cos(orbitAngle) * 0.85f;
                float oy = waveMantis.getY() + waveMantis.getHeight() + 0.45f + MathUtils.sin(orbitAngle) * 0.18f;
                particles.emitSpark(
                        ox, oy,
                        MathUtils.random(-0.4f, 0.4f), MathUtils.random(0.4f, 1.0f),
                        MathUtils.random(0.10f, 0.16f), 0.26f,
                        new Color(1.0f, 0.94f, 0.38f, 0.95f), new Color(0.78f, 0.55f, 1.0f, 0f),
                        false
                );
            }

            // Trailing ground sparks behind active shockwaves
            for (WaveMantis.Shockwave sw : waveMantis.getShockwaves()) {
                if (!sw.active) continue;
                particles.emitSpark(
                        sw.x + sw.width * 0.5f, sw.y + 0.18f,
                        -Math.signum(sw.vx) * MathUtils.random(1.0f, 3.0f), MathUtils.random(1.2f, 3.4f),
                        MathUtils.random(0.10f, 0.17f), 0.25f,
                        new Color(0.32f, 0.96f, 0.88f, 0.95f), Color.WHITE, true
                );
            }
        }
    }

    private void checkCollisions() {
        Rectangle playerCore = player.getCoreHitbox();
        boolean isSwinging = player.getStateMachine().getCurrentState() == player.getSwingState();
        Vector2 swingHitboxPos = isSwinging ? player.getSwingState().getHitboxOffset() : null;
        Rectangle swingRect = isSwinging
                ? new Rectangle(swingHitboxPos.x - 0.7f, swingHitboxPos.y - 0.7f, 1.4f, 1.4f)
                : null;

        // Player vs Spikes -> Celeste-style instant death burst + quick wipe + full room reset
        for (LushCaveStage.SpikeHazard spike : stage.getSpikes()) {
            if (playerCore.overlaps(spike.bounds)) {
                cameraFollow.addTrauma(0.55f);
                float deathX = player.getMotor().getPosition().x;
                float deathY = player.getMotor().getPosition().y + player.getStats().normalColliderSize.y * 0.5f;
                LushCaveStage.Room room = stage.findRoomAt(deathX, deathY);
                float spawnX = (room != null) ? room.spawnX : -10.5f;
                float spawnY = (room != null) ? room.spawnY : 5.2f;
                emitSparkBurst(deathX, deathY, new Color(0.35f, 0.96f, 0.92f, 1f), Color.WHITE, 18);
                vfx.triggerDeathBurst(deathX, deathY, () -> {
                    resetRoomState(room);
                    handlingControlledRespawn = true;
                    player.respawnAt(spawnX, spawnY);
                    handlingControlledRespawn = false;
                });
                return;
            }
        }

        // Player & Boss vs Stalactites
        for (Stalactite s : stage.getStalactites()) {
            if (!s.isActive()) continue;
            Rectangle sBounds = s.getCoreHitbox();

            // Falling stalactite hits Wave Mantis!
            if (s.isFalling() && waveMantis.isAlive() && sBounds.overlaps(waveMantis.getCoreHitbox())) {
                if (waveMantis.onStalactiteHit()) {
                    vfx.triggerHitStop(0.12f);
                    cameraFollow.addTrauma(0.85f);
                    vfx.triggerImpactRing(waveMantis.getCenterX(), waveMantis.getCenterY(), Color.ORANGE, 3.8f, 0.32f);
                    emitShatterBurst(waveMantis.getCenterX(), waveMantis.getCenterY(), new Color(0.50f, 0.52f, 0.48f, 0.95f), 14);
                    emitSparkBurst(waveMantis.getCenterX(), waveMantis.getCenterY(), Color.ORANGE, Color.YELLOW, 18);
                }
                s.onGroundHit();
                continue;
            }

            if (swingRect != null && swingRect.overlaps(sBounds)) {
                if (!s.isTriggered()) {
                    s.trigger();
                } else if (s.isPogoable()) {
                    player.getSwingState().onSwingHit(true);
                }
            } else if (s.isFalling() && playerCore.overlaps(sBounds)) {
                s.onCollision(player);
                if (playerHurtCooldown <= 0f) {
                    onPlayerHit(s.getX() + s.getWidth() * 0.5f);
                }
            }
        }

        // Player vs Wave Mantis (Parry, Dazed Swing, or Contact/Shockwave)
        if (waveMantis.isAlive() && waveMantis.getState() != WaveMantis.State.SLEEPING) {
            if (swingRect != null) {
                if (waveMantis.tryParry(swingRect)) {
                    player.getSwingState().onSwingHit(false);
                } else if (swingRect.overlaps(waveMantis.getCoreHitbox()) && waveMantis.onPlayerSwingHit()) {
                    vfx.triggerHitStop(0.045f);
                    cameraFollow.addTrauma(0.28f);
                    emitSparkBurst(waveMantis.getCenterX(), waveMantis.getCenterY(), Color.WHITE, new Color(0.75f, 0.55f, 1.0f, 0f), 10);
                    player.getSwingState().onSwingHit(false);
                }
            }

            // Shockwaves vs Player
            if (playerHurtCooldown <= 0f) {
                for (WaveMantis.Shockwave sw : waveMantis.getShockwaves()) {
                    if (playerCore.overlaps(sw.getBounds())) {
                        onPlayerHit(sw.x + sw.width * 0.5f);
                        break;
                    }
                }
                if (playerHurtCooldown <= 0f && waveMantis.getState() != WaveMantis.State.DAZED
                        && playerCore.overlaps(waveMantis.getCoreHitbox())) {
                    onPlayerHit(waveMantis.getCenterX());
                }
            }
        }

        // Player vs Breakables (Rock, Barrel)
        for (Rock r : stage.getBreakables()) {
            if (!r.isActive()) continue;
            Rectangle rBounds = r.getCoreHitbox();
            if (swingRect != null && swingRect.overlaps(rBounds)) {
                r.onCollision(player);
                vfx.triggerHitStop(0.03f);
                cameraFollow.addTrauma(0.2f);
            } else if (playerCore.overlaps(rBounds)) {
                r.onCollision(player);
            }
        }

        // Player vs Items
        for (Item item : activeItems) {
            if (!item.isActive()) continue;
            if (playerCore.overlaps(item.getCoreHitbox())) {
                item.onCollision(player);
            }
        }
    }

    private void resetRoomState(LushCaveStage.Room room) {
        if (room == null) return;
        stage.resetRoomInteractables(room);

        Iterator<Map.Entry<Stalactite, Float>> timerIt = arenaStalactiteRespawnTimers.entrySet().iterator();
        while (timerIt.hasNext()) {
            Stalactite s = timerIt.next().getKey();
            if (s.getInitialX() >= room.minX && s.getInitialX() < room.maxX) {
                timerIt.remove();
            }
        }

        Iterator<Item> itemIt = activeItems.iterator();
        while (itemIt.hasNext()) {
            Item item = itemIt.next();
            if (item.getItemTypeEnum() != ItemType.MANTIS_CLAW
                    && item.getX() >= room.minX && item.getX() < room.maxX) {
                itemIt.remove();
            }
        }
    }

    private void onPlayerHit(float sourceX) {
        playerHurtCooldown = 0.85f;
        player.takeDamage(1);
        vfx.triggerHitStop(0.07f);
        vfx.triggerScreenFlash(new Color(0.95f, 0.2f, 0.25f, 1.0f), 0.35f, 0.2f);
        cameraFollow.addTrauma(0.55f);

        float px = player.getMotor().getPosition().x;
        float py = player.getMotor().getPosition().y + player.getStats().normalColliderSize.y * 0.5f;
        emitSparkBurst(px, py, new Color(1.0f, 0.32f, 0.32f, 1f), Color.WHITE, 14);

        // Knockback away from threat
        float pushDir = (px < sourceX) ? -1.0f : 1.0f;
        player.getVelocity().set(pushDir * 9.0f, 7.5f);

        if (player.getHp() <= 0) {
            cameraFollow.addTrauma(0.85f);
            emitSparkBurst(px, py, new Color(1.0f, 0.38f, 0.42f, 1f), new Color(0.35f, 0.96f, 0.92f, 0f), 26);
            LushCaveStage.Room currentRoom = stage.findRoomAt(px, py);
            boolean inBossArena = stage.isArenaLocked()
                    || (currentRoom != null && currentRoom.minX >= LushCaveStage.ARENA_MIN_X);
            if (inBossArena) {
                showBanner("DEFEATED", "RETRYING WAVE MANTIS ARENA", 1.15f);
                vfx.triggerDeathBurst(px, py, 0.85f, this::executeArenaRetry);
            } else {
                LushCaveStage.Room respawnRoom = (currentRoom != null) ? currentRoom : stage.getRooms().get(0);
                showBanner("DEFEATED", "RESPAWNING IN " + respawnRoom.name.toUpperCase(), 1.15f);
                vfx.triggerDeathBurst(px, py, 0.85f, () -> executeRoomHealthRespawn(respawnRoom));
            }
        }
    }

    private void executeRoomHealthRespawn(LushCaveStage.Room room) {
        LushCaveStage.Room targetRoom = (room != null) ? room : stage.getRooms().get(0);
        resetRoomState(targetRoom);
        player.resetHealth();
        playerHurtCooldown = 0f;
        handlingControlledRespawn = true;
        player.respawnAt(targetRoom.spawnX, targetRoom.spawnY);
        handlingControlledRespawn = false;
        if (targetRoom != cameraFollow.getCurrentRoom()) {
            cameraFollow.setInitialRoom(targetRoom);
        } else {
            cameraFollow.snapToTarget();
        }
    }

    private void executeArenaRetry() {
        LushCaveStage.Room arenaRoom = stage.findRoomAt(LushCaveStage.ARENA_TRIGGER_X, 1.0f);
        resetRoomState(arenaRoom);
        if (waveMantis.isAlive() || waveMantis.getState() != WaveMantis.State.DEFEATED) {
            waveMantis.reset();
            stage.resetArena();
            player.getMotor().setSolids(stage.getBlocks());
        }
        player.resetHealth();
        playerHurtCooldown = 0f;
        handlingControlledRespawn = true;
        player.respawnAt(arenaRoom.spawnX, arenaRoom.spawnY);
        handlingControlledRespawn = false;
        cameraFollow.setInitialRoom(arenaRoom);
    }

    private void emitSparkBurst(float x, float y, Color startCol, Color endCol, int count) {
        if (particles == null) return;
        for (int i = 0; i < count; i++) {
            float angle = MathUtils.random(0f, MathUtils.PI2);
            float spd = MathUtils.random(3.2f, 10.5f);
            particles.emitSpark(
                    x, y,
                    MathUtils.cos(angle) * spd, MathUtils.sin(angle) * spd,
                    MathUtils.random(0.11f, 0.21f),
                    MathUtils.random(0.22f, 0.42f),
                    startCol, endCol, i % 2 == 0
            );
        }
    }

    private void emitShatterBurst(float x, float y, Color chunkColor, int count) {
        if (particles == null) return;
        for (int i = 0; i < count; i++) {
            float angle = MathUtils.random(0.15f, MathUtils.PI - 0.15f);
            float spd = MathUtils.random(2.8f, 8.5f);
            particles.emitDebris(
                    x + MathUtils.random(-0.25f, 0.25f),
                    y + MathUtils.random(-0.20f, 0.20f),
                    MathUtils.cos(angle) * spd, MathUtils.sin(angle) * spd,
                    MathUtils.random(0.12f, 0.24f),
                    MathUtils.random(0.32f, 0.62f),
                    chunkColor
            );
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override
    public void dispose() {
        batch.dispose();
        vfx.dispose();
        atmosphere.dispose();
        hintRenderer.dispose();
        AssetManager.getInstance().dispose();
        ProceduralTextures.dispose();
    }
}
