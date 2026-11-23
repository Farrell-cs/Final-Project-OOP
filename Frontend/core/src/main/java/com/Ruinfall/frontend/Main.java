package com.Ruinfall.frontend;

import com.Ruinfall.frontend.objects.GameObject;
import com.Ruinfall.frontend.objects.Interactables.*;
import com.Ruinfall.frontend.objects.items.Item;
import com.Ruinfall.frontend.objects.items.ItemType;
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

        // 1. Pass world coordinates: (x, y, width, height, grappleRadius)
        grapplePoint = new GrapplePoint(4.0f, 5.0f, 0.5f, 0.5f, 4.5f);

        // Add interactables
        stalactites.add(StalactiteFactory.createRock(2.0f, 12.5f));
        stalactites.add(StalactiteFactory.createCrystal(6.0f, 12.5f));

        breakables.add(new Rock(5.0f, 0.0f, ItemType.LIFE));
        breakables.add(new Barrels(8.0f, 0.0f));

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
                if (!s.isTriggered() && Math.abs(px - (s.getX() + s.getWidth() * 0.5f)) < 1.5f && py < s.getY()) {
                    s.trigger();
                }
                s.update(FIXED_DT);
            }

            // Update breakables (rocks, barrels)
            for (Rock r : breakables) {
                if (r.isActive()) {
                    r.update(FIXED_DT);
                }
            }

            // Update items
            for (Item item : activeItems) {
                if (item.isActive()) {
                    item.update(FIXED_DT);
                }
            }

            player.stepPhysics(FIXED_DT);

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

        // Render stalactites
        for (Stalactite s : stalactites) {
            if (s.isActive()) {
                batch.setColor(s.getColor() != null ? s.getColor() : Color.GRAY);
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
        batch.setColor(Color.WHITE);

        visuals.render(batch, blockTexture);
        batch.end();
    }

    private void checkCollisions() {
        Rectangle playerCore = player.getCoreHitbox();
        boolean isSwinging = player.getStateMachine().getCurrentState() == player.getSwingState();
        Vector2 swingHitboxPos = isSwinging ? player.getSwingState().getHitboxOffset() : null;
        Rectangle swingRect = isSwinging ? new Rectangle(swingHitboxPos.x - 0.5f, swingHitboxPos.y - 0.5f, 1.0f, 1.0f) : null;

        // Player vs Stalactites
        for (Stalactite s : stalactites) {
            if (!s.isActive()) continue;
            Rectangle sBounds = s.getCoreHitbox();
            if (swingRect != null && swingRect.overlaps(sBounds)) {
                if (s.isPogoable()) {
                    player.getSwingState().onSwingHit(true);
                }
            } else if (playerCore.overlaps(sBounds)) {
                s.onCollision(player);
            }
        }

        // Player vs Breakables (Rock, Barrel)
        for (Rock r : breakables) {
            if (!r.isActive()) continue;
            Rectangle rBounds = r.getCoreHitbox();
            if (swingRect != null && swingRect.overlaps(rBounds)) {
                r.onCollision(player);
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
