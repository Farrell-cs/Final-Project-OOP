package com.Ruinfall.frontend;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.Ruinfall.frontend.visuals.CameraFollow;
import com.Ruinfall.frontend.visuals.PlayerVisuals;
import com.Ruinfall.frontend.visuals.ProceduralTextures;
import com.Ruinfall.frontend.world.BoxRoom;

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

    private float accumulator;

    @Override
    public void create() {
        batch = new SpriteBatch();
        blockTexture = ProceduralTextures.getBlockTexture();

        camera = new OrthographicCamera();
        viewport = new ExtendViewport(24.0f, 14.0f, camera);

        boxRoom = new BoxRoom();
        player = new PlayerController();
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

        accumulator += Math.min(dt, 0.25f);
        while (accumulator >= FIXED_DT) {
            player.stepPhysics(FIXED_DT);
            accumulator -= FIXED_DT;
        }

        visuals.update(dt);
        cameraFollow.update(dt);

        ScreenUtils.clear(0.08f, 0.10f, 0.16f, 1.0f);

        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        boxRoom.render(batch, blockTexture);
        visuals.render(batch, blockTexture);
        batch.end();
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
