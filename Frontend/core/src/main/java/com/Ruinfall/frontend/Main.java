package com.Ruinfall.frontend;

import com.Ruinfall.frontend.objects.Interactables.Barrels;
import com.Ruinfall.frontend.objects.Interactables.GrapplePoint;
import com.Ruinfall.frontend.objects.Interactables.Rock;
import com.Ruinfall.frontend.objects.Interactables.Stalactite;
import com.Ruinfall.frontend.objects.enemies.WaveMantis;
import com.Ruinfall.frontend.objects.items.Item;
import com.Ruinfall.frontend.objects.items.ItemType;
import com.Ruinfall.frontend.systems.GameEventBus;
import com.Ruinfall.frontend.visuals.CameraFollow;
import com.Ruinfall.frontend.visuals.PlayerVisuals;
import com.Ruinfall.frontend.visuals.ProceduralTextures;
import com.Ruinfall.frontend.visuals.VfxManager;
import com.Ruinfall.frontend.visuals.WorldHintRenderer;
import com.Ruinfall.frontend.world.LushCaveStage;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;
import java.util.HashMap;
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
    private WorldHintRenderer hintRenderer;

    private final List<Item> activeItems = new ArrayList<>();
    private final Map<Stalactite, Float> arenaStalactiteRespawnTimers = new HashMap<>();

    private float accumulator;
    private float playerHurtCooldown = 0f;

    // Banner state for boss defeat / reward pickup
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
        player.getMotor().getPosition().set(-10.0f, 1.0f);
        player.getMotor().setSolids(stage.getBlocks());

        waveMantis = new WaveMantis(92.0f, 0.0f);
        vfx = new VfxManager();
        hintRenderer = new WorldHintRenderer();

        // Subscribe to GameEventBus
        GameEventBus bus = GameEventBus.getInstance();
        bus.subscribe(GameEventBus.GameEvent.ROCK_SHATTERED, (event, payload) -> {
            if (payload instanceof Item item) activeItems.add(item);
        });
        bus.subscribe(GameEventBus.GameEvent.CRYSTAL_SHATTERED, (event, payload) -> {
            if (payload instanceof Item item) activeItems.add(item);
        });
        bus.subscribe(GameEventBus.GameEvent.ROCK_BROKEN, (event, payload) -> {
            if (payload instanceof Rock rock) activeItems.addAll(rock.getDrops());
        });
        bus.subscribe(GameEventBus.GameEvent.BARREL_EXPLODED, (event, payload) -> {
            if (payload instanceof Barrels.BarrelExplosionPayload p) activeItems.addAll(p.items);
        });
        bus.subscribe(GameEventBus.GameEvent.BOSS_SHOCKWAVE, (event, payload) -> {
            if (payload instanceof WaveMantis boss) {
                cameraFollow.addTrauma(0.38f);
                vfx.triggerImpactRing(boss.getCenterX(), 0.4f, new Color(0.3f, 0.95f, 0.85f, 1f), 4.2f, 0.35f);
            }
        });
        bus.subscribe(GameEventBus.GameEvent.BOSS_PARRIED, (event, payload) -> {
            if (payload instanceof WaveMantis boss) {
                vfx.triggerParryFlash(boss.getCenterX(), boss.getCenterY());
                cameraFollow.addTrauma(0.68f);
            }
        });
        bus.subscribe(GameEventBus.GameEvent.BOSS_DAZED, (event, payload) -> {
            if (payload instanceof WaveMantis boss) {
                cameraFollow.addTrauma(0.55f);
                vfx.triggerImpactRing(boss.getCenterX(), boss.getCenterY(), new Color(0.85f, 0.65f, 1.0f, 1f), 3.0f, 0.3f);
            }
        });
        bus.subscribe(GameEventBus.GameEvent.BOSS_DEFEATED, (event, payload) -> {
            if (payload instanceof WaveMantis boss) {
                vfx.triggerHitStop(0.18f);
                vfx.triggerScreenFlash(new Color(1.0f, 0.95f, 0.7f, 1.0f), 0.65f, 0.35f);
                vfx.triggerImpactRing(boss.getCenterX(), boss.getCenterY(), Color.GOLD, 6.0f, 0.5f);
                cameraFollow.addTrauma(1.0f);

                // Drop MANTIS_CLAW reward & open Lava Shaft
                activeItems.add(new Item(boss.getCenterX() - 0.3f, 1.4f, ItemType.MANTIS_CLAW));
                stage.unlockArenaAndOpenLavaShaft();
                player.getMotor().setSolids(stage.getBlocks());
                showBanner("WAVE MANTIS DEFEATED", "CLAIM THE MANTIS CLAW", 3.5f);
            }
        });
        bus.subscribe(GameEventBus.GameEvent.ITEM_COLLECTED, (event, payload) -> {
            if (payload instanceof Item item && item.getItemTypeEnum() == ItemType.MANTIS_CLAW) {
                vfx.triggerHitStop(0.08f);
                vfx.triggerScreenFlash(new Color(0.45f, 1.0f, 0.78f, 1.0f), 0.45f, 0.3f);
                cameraFollow.addTrauma(0.4f);
                showBanner("MANTIS CLAW ACQUIRED", "LAVA SHAFT UNSEALED - DROP DOWN TO CONTINUE", 4.5f);
            }
        });

        visuals = new PlayerVisuals(player);
        cameraFollow = new CameraFollow(camera, player);

        player.setEventListener(new PlayerController.PlayerEventListener() {
            @Override
            public void onJumped(PlayerController.JumpKind kind) {
                visuals.onJumped(kind);
            }

            @Override
            public void onDashed(Vector2 dir) {
                visuals.onDashed(dir);
                cameraFollow.triggerShake(dir);
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
        player.updateInput();

        if (bannerTimer > 0f) {
            bannerTimer = Math.max(0f, bannerTimer - dt);
        }
        if (playerHurtCooldown > 0f) {
            playerHurtCooldown = Math.max(0f, playerHurtCooldown - dt);
        }

        // Hit-stop freezes simulation while keeping camera shake & VFX crisp
        if (!vfx.updateHitStop(dt)) {
            accumulator += Math.min(dt, 0.25f);
            while (accumulator >= FIXED_DT) {
                stepSimulation(FIXED_DT);
                accumulator -= FIXED_DT;
            }
        }

        vfx.update(dt);
        visuals.update(dt);
        cameraFollow.update(dt);

        // Deep mossy cave background
        ScreenUtils.clear(0.05f, 0.09f, 0.11f, 1.0f);

        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        stage.render(batch, blockTexture);

        // Render grapple points & tether
        float px = player.getMotor().getPosition().x;
        float py = player.getMotor().getPosition().y + player.getStats().normalColliderSize.y * 0.5f;
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

        // Render player
        visuals.render(batch, blockTexture);
        batch.end();

        // Render procedural world-space VFX (parry starburst, shockwave rings)
        vfx.renderWorld(camera);

        // Render world-space tutorial hints
        hintRenderer.render(batch, camera, stage.getHints(), px, py);

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

        // Update stalactites & respawn arena stalactites while boss is alive
        for (Stalactite s : stage.getStalactites()) {
            if (!s.isActive()) {
                if (s.getX() >= LushCaveStage.ARENA_MIN_X && waveMantis.isAlive()) {
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

        // Update Wave Mantis
        waveMantis.updateBoss(dt, px, py);

        // Update player physics
        player.stepPhysics(dt);

        // Check Lava Shaft transition
        if (stage.isLavaShaftOpen() && player.getMotor().getPosition().y < -7.5f) {
            showBanner("ENTERING LAVA SHAFT", "STAGE 1 COMPLETE", 4.0f);
            player.getMotor().getPosition().set(LushCaveStage.LAVA_SHAFT_X + 2.5f, 1.5f);
            player.getVelocity().setZero();
        }

        checkCollisions();
    }

    private void checkCollisions() {
        Rectangle playerCore = player.getCoreHitbox();
        boolean isSwinging = player.getStateMachine().getCurrentState() == player.getSwingState();
        Vector2 swingHitboxPos = isSwinging ? player.getSwingState().getHitboxOffset() : null;
        Rectangle swingRect = isSwinging
                ? new Rectangle(swingHitboxPos.x - 0.7f, swingHitboxPos.y - 0.7f, 1.4f, 1.4f)
                : null;

        // Player vs Spikes -> respawn at safe checkpoint
        for (LushCaveStage.SpikeHazard spike : stage.getSpikes()) {
            if (playerCore.overlaps(spike.bounds)) {
                cameraFollow.addTrauma(0.45f);
                vfx.triggerScreenFlash(new Color(0.95f, 0.2f, 0.2f, 1f), 0.35f, 0.2f);
                float safeX = (player.getMotor().getPosition().x < 24f) ? -6.0f
                        : (player.getMotor().getPosition().x < 38f) ? 20.0f : 34.0f;
                player.getMotor().getPosition().set(safeX, 2.0f);
                player.getVelocity().setZero();
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
            } else if (playerCore.overlaps(sBounds)) {
                s.onCollision(player);
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
                    player.getSwingState().onSwingHit(false);
                }
            }

            // Shockwaves vs Player
            if (playerHurtCooldown <= 0f) {
                for (WaveMantis.Shockwave sw : waveMantis.getShockwaves()) {
                    if (playerCore.overlaps(sw.getBounds())) {
                        onPlayerHit();
                        break;
                    }
                }
                if (waveMantis.getState() != WaveMantis.State.DAZED && playerCore.overlaps(waveMantis.getCoreHitbox())) {
                    onPlayerHit();
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

    private void onPlayerHit() {
        playerHurtCooldown = 0.85f;
        vfx.triggerHitStop(0.07f);
        vfx.triggerScreenFlash(new Color(0.95f, 0.2f, 0.25f, 1.0f), 0.35f, 0.2f);
        cameraFollow.addTrauma(0.55f);
        // Knockback away from boss
        float pushDir = (player.getMotor().getPosition().x < waveMantis.getCenterX()) ? -1.0f : 1.0f;
        player.getVelocity().set(pushDir * 9.0f, 7.5f);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override
    public void dispose() {
        batch.dispose();
        vfx.dispose();
        hintRenderer.dispose();
        ProceduralTextures.dispose();
    }
}
