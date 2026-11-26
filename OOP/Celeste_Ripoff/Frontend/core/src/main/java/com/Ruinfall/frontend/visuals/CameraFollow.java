package com.Ruinfall.frontend.visuals;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.Ruinfall.frontend.PlayerController;

public class CameraFollow {
    private final OrthographicCamera camera;
    private final PlayerController target;

    private final Vector2 baseOffset = new Vector2(0.0f, 2.2f);
    private float lookAheadX = 1.8f;
    private float smoothSpeed = 10.0f;
    private float minY = 0.0f;

    private Rectangle worldBounds;
    private Viewport viewport;

    private final Vector2 shakeOffset = new Vector2();
    private final Vector2 shakeDirection = new Vector2();
    private float shakeTimer;
    private float shakeDuration = 0.14f;

    public CameraFollow(OrthographicCamera camera, PlayerController target) {
        this.camera = camera;
        this.target = target;
        snapToTarget();
    }

    public void setWorldBounds(Rectangle bounds, Viewport viewport) {
        this.worldBounds = bounds != null ? new Rectangle(bounds) : null;
        this.viewport = viewport;
        snapToTarget();
    }

    public void snapToTarget() {
        Vector2 desired = clampToWorldBounds(computeTargetPosition());
        camera.position.set(desired.x, desired.y, 0.0f);
        camera.update();
    }

    public void triggerShake(Vector2 dir) {
        shakeDirection.set(dir).nor();
        shakeTimer = shakeDuration;
    }

    public void update(float dt) {
        Vector2 desired = clampToWorldBounds(computeTargetPosition());

        float expFactor = 1.0f - (float) Math.exp(-smoothSpeed * dt);
        camera.position.x = MathUtils.lerp(camera.position.x, desired.x, expFactor);
        camera.position.y = MathUtils.lerp(camera.position.y, desired.y, expFactor);

        Vector2 clamped = clampToWorldBounds(new Vector2(camera.position.x, camera.position.y));
        camera.position.set(clamped.x, clamped.y, 0.0f);

        if (shakeTimer > 0.0f) {
            shakeTimer -= dt;
            float damper = MathUtils.clamp(shakeTimer / shakeDuration, 0.0f, 1.0f);
            float osc = MathUtils.sin((shakeDuration - shakeTimer) * 75.0f) * 0.18f * damper;
            shakeOffset.set(shakeDirection).scl(osc);
        } else {
            shakeOffset.setZero();
        }

        CameraShake globalShake = EffectManager.getInstance().getCameraShake();
        camera.position.add(
            shakeOffset.x + globalShake.getOffsetX(),
            shakeOffset.y + globalShake.getOffsetY(),
            0.0f
        );
        camera.update();
    }

    private Vector2 computeTargetPosition() {
        Vector2 playerPos = target.getMotor().getPosition();
        float ahead = target.getFacingDirection() * lookAheadX;
        float tx = playerPos.x + baseOffset.x + ahead;
        float ty = Math.max(minY, playerPos.y + baseOffset.y);
        return new Vector2(tx, ty);
    }

    private Vector2 clampToWorldBounds(Vector2 pos) {
        if (worldBounds == null) {
            return pos;
        }
        float viewW = viewport != null ? viewport.getWorldWidth() : camera.viewportWidth;
        float viewH = viewport != null ? viewport.getWorldHeight() : camera.viewportHeight;
        if (viewW <= 0f || viewH <= 0f) {
            return pos;
        }

        float halfW = viewW * 0.5f;
        float halfH = viewH * 0.5f;

        if (worldBounds.width <= viewW) {
            pos.x = worldBounds.x + worldBounds.width * 0.5f;
        } else {
            pos.x = MathUtils.clamp(pos.x, worldBounds.x + halfW, worldBounds.x + worldBounds.width - halfW);
        }

        if (worldBounds.height <= viewH) {
            pos.y = worldBounds.y + worldBounds.height * 0.5f;
        } else {
            pos.y = MathUtils.clamp(pos.y, worldBounds.y + halfH, worldBounds.y + worldBounds.height - halfH);
        }
        return pos;
    }

    public Rectangle getWorldBounds() {
        return worldBounds != null ? new Rectangle(worldBounds) : null;
    }
}
