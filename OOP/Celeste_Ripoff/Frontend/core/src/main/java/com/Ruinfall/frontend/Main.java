package com.Ruinfall.frontend;

import com.Ruinfall.frontend.objects.GameObject;
import com.Ruinfall.frontend.objects.Interactables.*;
import com.Ruinfall.frontend.objects.bullets.Bullet;
import com.Ruinfall.frontend.objects.enemies.WormBoss;
import com.Ruinfall.frontend.objects.enemies.WormSegment;
import com.Ruinfall.frontend.objects.hazards.SlimeHazard;
import com.Ruinfall.frontend.objects.items.Item;
import com.Ruinfall.frontend.objects.items.ItemType;
import com.Ruinfall.frontend.systems.BulletManager;
import com.Ruinfall.frontend.systems.GameEventBus;
import com.Ruinfall.frontend.systems.StalactiteFactory;
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
import com.Ruinfall.frontend.visuals.CameraFollow;
import com.Ruinfall.frontend.visuals.PlayerVisuals;
import com.Ruinfall.frontend.visuals.ProceduralTextures;
import com.Ruinfall.frontend.world.BoxRoom;

import java.util.ArrayList;
import java.util.List;

public class Main extends ApplicationAdapter {
    private static final float FIXED_DT = 1.0f / 60.0f;

    private SpriteBatch batch;
    private Texture blockTexture;

    private OrthographicCamera camera;
    private Viewport viewport;

    private PlayerController player;
    private PlayerVisuals visuals;
    private CameraFollow cameraFollow;
    private BoxRoom boxRoom;
    private GrapplePoint grapplePoint;
    private BulletManager bulletManager;
    private WormBoss wormBoss;

    private final List<Stalactite> stalactites = new ArrayList<>();
    private final List<Rock> breakables = new ArrayList<>();
    private final List<Item> activeItems = new ArrayList<>();

    private float accumulator;

    @Override
    public void create() {
        batch = new SpriteBatch();
        blockTexture = ProceduralTextures.getBlockTexture();

        camera = new OrthographicCamera();
        viewport = new ExtendViewport(24.0f, 14.0f, camera);

        boxRoom = new BoxRoom();
        player = new PlayerController();
        bulletManager = new BulletManager();
        wormBoss = new WormBoss(-10.0f, 10.0f, "Eater of Ruin", 300, boxRoom.getArenaBounds(), 8, 1.0f);

        // 1. Pass world coordinates: (x, y, width, height, grappleRadius)
        grapplePoint = new GrapplePoint(0.0f, 10.2f, 0.5f, 0.5f, 5.0f);

        // Add ceiling-mounted regenerating stalactites across the arena roof
        float[] stalactiteXs = {-10.0f, -5.0f, 0.0f, 5.0f, 10.0f};
        for (float sx : stalactiteXs) {
            RegeneratingStalactite rs = new RegeneratingStalactite(sx, 12.5f);
            rs.setSolids(boxRoom.getBlocks());
            stalactites.add(rs);
        }

        // Add destructible/explosive barrels across floor and platforms
        breakables.add(new Barrels(-5.5f, 0.0f));
        breakables.add(new Barrels(5.5f, 0.0f));
        breakables.add(new Barrels(-8.5f, 2.8f));
        breakables.add(new Barrels(-0.7f, 5.6f));

        // Subscribe to GameEventBus to receive dropped items
        GameEventBus.getInstance().subscribe(GameEventBus.GameEvent.ROCK_SHATTERED, (event, payload) -> {
            if (payload instanceof Item item) activeItems.add(item);
        });
        GameEventBus.getInstance().subscribe(GameEventBus.GameEvent.CRYSTAL_SHATTERED, (event, payload) -> {
            if (payload instanceof Item item) activeItems.add(item);
        });
        GameEventBus.getInstance().subscribe(GameEventBus.GameEvent.ROCK_BROKEN, (event, payload) -> {
            if (payload instanceof Rock rock) activeItems.addAll(rock.getDrops());
        });
        GameEventBus.getInstance().subscribe(GameEventBus.GameEvent.BARREL_EXPLODED, (event, payload) -> {
            if (payload instanceof Barrels.BarrelExplosionPayload p) activeItems.addAll(p.items);
        });

        player.getMotor().setSolids(boxRoom.getBlocks());

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
            }

            @Override
            public void onRespawned() {
                visuals.onRespawned();
                cameraFollow.snapToTarget();
                resetInteractables();
            }
        });
    }

    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();
        player.updateInput();

        // 2. Single merged fixed-timestep loop
        accumulator += Math.min(dt, 0.25f);
        while (accumulator >= FIXED_DT) {
            grapplePoint.update(FIXED_DT);

            // Check if player is within grapple radius
            float px = player.getMotor().getPosition().x;
            float py = player.getMotor().getPosition().y + player.getStats().normalColliderSize.y * 0.5f;
            grapplePoint.checkPlayerRange(px, py);

            if (grapplePoint.canGrapple()) {
                player.setNearbyGrapplePoint(grapplePoint);
            } else if (player.getNearbyGrapplePoint() == grapplePoint && grapplePoint.getState() != GrapplePoint.GrappleState.ACTIVE) {
                player.setNearbyGrapplePoint(null);
            }

            // Update stalactites & check player proximity for trigger
            for (Stalactite s : stalactites) {
                if (!s.isActive()) continue;
                if (s instanceof RegeneratingStalactite rs) {
                    rs.checkProximityTrigger(px, py);
                } else if (!s.isTriggered() && Math.abs(px - (s.getX() + s.getWidth() * 0.5f)) < 1.5f && py < s.getY()) {
                    s.trigger();
                }
                s.update(FIXED_DT);
            }

            // Update breakables (rocks, barrels) & trigger pending barrel AoE detonations
            for (Rock r : breakables) {
                if (r.isActive()) {
                    r.update(FIXED_DT);
                } else if (r instanceof Barrels barrel && !barrel.hasDetonated()) {
                    barrel.detonate(wormBoss, bulletManager);
                }
            }

            // Update items
            for (Item item : activeItems) {
                if (item.isActive()) {
                    item.update(FIXED_DT);
                }
            }

            player.stepPhysics(FIXED_DT);

            if (wormBoss != null && wormBoss.isActive()) {
                wormBoss.updateBoss(FIXED_DT, player, bulletManager);
            }
            bulletManager.update(FIXED_DT);

            // Collisions
            checkCollisions();

            accumulator -= FIXED_DT;
        }

        visuals.update(dt);
        cameraFollow.update(dt);

        ScreenUtils.clear(0.08f, 0.10f, 0.16f, 1.0f);

        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        boxRoom.render(batch, blockTexture);
        grapplePoint.render(batch, blockTexture);
        grapplePoint.renderTether(
            batch,
            blockTexture,
            player.getMotor().getPosition().x,
            player.getMotor().getPosition().y + player.getStats().normalColliderSize.y * 0.5f
        );

        // Render stalactites (with grow-down scale during REGENERATING)
        for (Stalactite s : stalactites) {
            if (!s.isActive()) continue;
            batch.setColor(s.getColor() != null ? s.getColor() : Color.GRAY);
            if (s instanceof RegeneratingStalactite rs) {
                float scale = rs.getRenderScale();
                if (scale > 0.01f) {
                    float scaledW = s.getWidth() * scale;
                    float scaledH = s.getHeight() * scale;
                    float drawX = s.getX() + (s.getWidth() - scaledW) * 0.5f;
                    float drawY = (s.getY() + s.getHeight()) - scaledH;
                    batch.draw(blockTexture, drawX, drawY, scaledW, scaledH);
                }
            } else {
                batch.draw(blockTexture, s.getX(), s.getY(), s.getWidth(), s.getHeight());
            }
        }

        // Render breakables (rock, barrel)
        for (Rock r : breakables) {
            if (r.isActive()) {
                batch.setColor(r.getColor() != null ? r.getColor() : Color.DARK_GRAY);
                batch.draw(blockTexture, r.getX(), r.getY(), r.getWidth(), r.getHeight());
            }
        }

        // Render items
        for (Item item : activeItems) {
            if (item.isActive()) {
                batch.setColor(item.getColor() != null ? item.getColor() : Color.YELLOW);
                batch.draw(blockTexture, item.getX(), item.getY(), item.getWidth(), item.getHeight());
            }
        }

        renderWormBossAndProjectiles();
        batch.setColor(Color.WHITE);

        visuals.render(batch, blockTexture);
        renderBossHpBar();
        batch.end();
    }

    private void checkCollisions() {
        Rectangle playerCore = player.getCoreHitbox();
        boolean isSwinging = player.getStateMachine().getCurrentState() == player.getSwingState();
        Vector2 swingHitboxPos = isSwinging ? player.getSwingState().getHitboxOffset() : null;
        Rectangle swingRect = isSwinging ? new Rectangle(swingHitboxPos.x - 0.5f, swingHitboxPos.y - 0.5f, 1.0f, 1.0f) : null;

        // Player & Boss vs Stalactites
        for (Stalactite s : stalactites) {
            if (!s.isActive()) continue;
            if (s instanceof RegeneratingStalactite rs && rs.getState() != RegeneratingStalactite.State.FALLING) {
                continue;
            }
            Rectangle sBounds = s.getCoreHitbox();
            if (swingRect != null && swingRect.overlaps(sBounds)) {
                if (s.isPogoable()) {
                    player.getSwingState().onSwingHit(true);
                }
            } else if (playerCore.overlaps(sBounds)) {
                s.onCollision(player);
            } else if (wormBoss != null && wormBoss.isActive() && wormBoss.isAlive()) {
                for (Rectangle box : wormBoss.getActiveHitboxes()) {
                    if (sBounds.overlaps(box)) {
                        s.onCollision(wormBoss);
                        break;
                    }
                }
            }
        }

        // Player & Boss vs Breakables (Rock, Barrel)
        for (Rock r : breakables) {
            if (!r.isActive()) continue;
            Rectangle rBounds = r.getCoreHitbox();
            if (swingRect != null && swingRect.overlaps(rBounds)) {
                r.onCollision(player);
            } else if (playerCore.overlaps(rBounds)) {
                r.onCollision(player);
            } else if (wormBoss != null && wormBoss.isActive() && wormBoss.isAlive()) {
                for (Rectangle box : wormBoss.getActiveHitboxes()) {
                    if (rBounds.overlaps(box)) {
                        r.onCollision(wormBoss);
                        break;
                    }
                }
            }
        }

        // Player vs Items
        for (Item item : activeItems) {
            if (!item.isActive()) continue;
            if (playerCore.overlaps(item.getCoreHitbox())) {
                item.onCollision(player);
            }
        }

        // Player & Swing vs WormBoss
        if (wormBoss != null && wormBoss.isActive() && wormBoss.isAlive()) {
            List<Rectangle> bossBoxes = wormBoss.getActiveHitboxes();
            boolean swingConnected = false;
            for (Rectangle box : bossBoxes) {
                if (swingRect != null && !player.getSwingState().hasHitThisSwing() && swingRect.overlaps(box)) {
                    wormBoss.takeDamage(15);
                    player.getSwingState().onSwingHit(true);
                    swingConnected = true;
                    break;
                }
            }
            if (!swingConnected) {
                for (Rectangle box : bossBoxes) {
                    if (playerCore.overlaps(box)) {
                        wormBoss.onCollision(player);
                        break;
                    }
                }
            }
        }

        // Projectiles vs Player & WormBoss
        List<Bullet> bullets = bulletManager.getActiveBullets();
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            if (b.isDestroyed()) continue;
            Rectangle bBox = b.getCoreHitbox();
            if (!b.getIsPlayerBullet() && playerCore.overlaps(bBox)) {
                b.onCollision(player);
            } else if (b.getIsPlayerBullet() && wormBoss != null && wormBoss.isActive() && wormBoss.isAlive()) {
                for (Rectangle box : wormBoss.getActiveHitboxes()) {
                    if (bBox.overlaps(box)) {
                        b.onCollision(wormBoss);
                        break;
                    }
                }
            }
        }
    }

    private void renderWormBossAndProjectiles() {
        if (wormBoss != null && wormBoss.isActive()) {
            for (SlimeHazard hazard : wormBoss.getSlimeHazards()) {
                if (hazard.isActive()) {
                    batch.setColor(hazard.getColor());
                    batch.draw(blockTexture, hazard.getX(), hazard.getY(), hazard.getWidth(), hazard.getHeight());
                }
            }
            List<WormSegment> segs = wormBoss.getSegments();
            for (int i = segs.size() - 1; i >= 0; i--) {
                WormSegment seg = segs.get(i);
                if (seg.isActive()) {
                    batch.setColor(seg.getColor());
                    batch.draw(blockTexture, seg.getX(), seg.getY(), seg.getWidth(), seg.getHeight());
                }
            }
            batch.setColor(wormBoss.getColor());
            batch.draw(blockTexture, wormBoss.getX(), wormBoss.getY(), wormBoss.getWidth(), wormBoss.getHeight());
        }

        for (Bullet b : bulletManager.getActiveBullets()) {
            if (!b.isDestroyed()) {
                batch.setColor(b.getIsPlayerBullet() ? Color.CYAN : Color.ORANGE);
                batch.draw(blockTexture, b.getX(), b.getY(), b.getWidth(), b.getHeight());
            }
        }
    }

    private void renderBossHpBar() {
        if (wormBoss == null || !wormBoss.isActive() || !wormBoss.isAlive()) {
            return;
        }
        float ratio = Math.max(0f, Math.min(1f, wormBoss.getHp() / (float) wormBoss.getMaxHP()));
        float barWidth = 14.0f;
        float barHeight = 0.45f;
        float barX = camera.position.x - barWidth * 0.5f;
        float barY = camera.position.y + viewport.getWorldHeight() * 0.5f - 1.0f;

        Color stageColor;
        switch (wormBoss.getCurrentStage()) {
            case STAGE_2_SIDE_SWEEPS:
                stageColor = Color.GOLD;
                break;
            case STAGE_3_BULLET_HELL_CORE:
                stageColor = Color.MAGENTA;
                break;
            default:
                stageColor = Color.SCARLET;
                break;
        }

        batch.setColor(0.10f, 0.10f, 0.14f, 0.85f);
        batch.draw(blockTexture, barX - 0.1f, barY - 0.1f, barWidth + 0.2f, barHeight + 0.2f);
        batch.setColor(stageColor);
        batch.draw(blockTexture, barX, barY, barWidth * ratio, barHeight);
        batch.setColor(Color.WHITE);
    }

    public void resetInteractables() {
        for (Stalactite s : stalactites) {
            s.reset();
        }
        for (Rock r : breakables) {
            if (r instanceof Barrels barrel) {
                barrel.reset();
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override
    public void dispose() {
        batch.dispose();
        ProceduralTextures.dispose();
    }
}
