package com.Ruinfall.frontend.visuals;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.Ruinfall.frontend.PlayerController;

public class
CameraFollow {
    private final OrthographicCamera camera;
    private final PlayerController target;

    private final Vector2 baseOffset = new Vector2(0.0f, 2.2f);
    private float lookAheadX = 1.8f;
    private float smoothSpeed = 10.0f;
    private float minY = 0.0f;

    private final Vector2 shakeOffset = new Vector2();
    private final Vector2 shakeDirection = new Vector2();
    private float shakeTimer;
    private float shakeDuration = 0.14f;

    // Trauma-based screen shake (0..1, quadratic intensity)
    private float trauma = 0.0f;
    private float traumaDecay = 1.8f;
    private float maxShakeOffset = 0.55f;
    private float shakeTime = 0.0f;

    public CameraFollow(OrthographicCamera camera, PlayerController target) {
        this.camera = camera;
        this.target = target;
        snapToTarget();
    }

    public void snapToTarget() {
        Vector2 desired = computeTargetPosition();
        camera.position.set(desired.x, desired.y, 0.0f);
        camera.update();
    }

    public void triggerShake(Vector2 dir) {
        shakeDirection.set(dir).nor();
        shakeTimer = shakeDuration;
        addTrauma(0.28f);
    }

    public void addTrauma(float amount) {
        trauma = MathUtils.clamp(trauma + amount, 0.0f, 1.0f);
    }

    public void update(float dt) {
        Vector2 desired = computeTargetPosition();

        float expFactor = 1.0f - (float) Math.exp(-smoothSpeed * dt);
        camera.position.x = MathUtils.lerp(camera.position.x, desired.x, expFactor);
        camera.position.y = MathUtils.lerp(camera.position.y, desired.y, expFactor);

        shakeOffset.setZero();
        shakeTime += dt;

        if (shakeTimer > 0.0f) {
            shakeTimer -= dt;
            float damper = MathUtils.clamp(shakeTimer / shakeDuration, 0.0f, 1.0f);
            float osc = MathUtils.sin((shakeDuration - shakeTimer) * 75.0f) * 0.18f * damper;
            shakeOffset.add(shakeDirection.x * osc, shakeDirection.y * osc);
        }

        if (trauma > 0.0f) {
            float intensity = trauma * trauma;
            float nx = MathUtils.sin(shakeTime * 67.0f) * 0.6f + MathUtils.cos(shakeTime * 113.0f) * 0.4f;
            float ny = MathUtils.cos(shakeTime * 79.0f) * 0.6f + MathUtils.sin(shakeTime * 97.0f) * 0.4f;
            shakeOffset.add(nx * maxShakeOffset * intensity, ny * maxShakeOffset * intensity);
            trauma = Math.max(0.0f, trauma - traumaDecay * dt);
        }

        camera.position.add(shakeOffset.x, shakeOffset.y, 0.0f);
        camera.update();
    }

    private Vector2 computeTargetPosition() {
        Vector2 playerPos = target.getMotor().getPosition();
        float ahead = target.getFacingDirection() * lookAheadX;
        float tx = playerPos.x + baseOffset.x + ahead;
        float ty = Math.max(minY, playerPos.y + baseOffset.y);
        return new Vector2(tx, ty);
    }
}
