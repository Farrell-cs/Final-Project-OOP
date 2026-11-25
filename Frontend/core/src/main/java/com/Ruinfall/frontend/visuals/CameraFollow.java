package com.Ruinfall.frontend.visuals;

import com.Ruinfall.frontend.PlayerController;
import com.Ruinfall.frontend.world.LushCaveStage;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * Celeste-style discrete room camera with smooth room-to-room transitions
 * and trauma-based screen shake.
 */
public class CameraFollow {
    private final OrthographicCamera camera;
    private final PlayerController target;

    private final Vector2 baseOffset = new Vector2(0.0f, 2.0f);
    private float lookAheadX = 1.4f;
    private float smoothSpeed = 10.0f;

    // Discrete room locking & smooth transition
    private LushCaveStage.Room currentRoom;
    private final Vector2 transitionStart = new Vector2();
    private final Vector2 transitionEnd = new Vector2();
    private float transitionTimer = 0.0f;
    private float transitionDuration = 0.30f;
    private boolean transitioning = false;

    // Directional kick + trauma-based shake
    private final Vector2 shakeOffset = new Vector2();
    private final Vector2 shakeDirection = new Vector2();
    private float shakeTimer;
    private float shakeDuration = 0.14f;

    private float trauma = 0.0f;
    private float traumaDecay = 1.8f;
    private float maxShakeOffset = 0.55f;
    private float shakeTime = 0.0f;

    // Clean position before shake is added (prevents shake drift)
    private final Vector2 baseCamPos = new Vector2();

    public CameraFollow(OrthographicCamera camera, PlayerController target) {
        this.camera = camera;
        this.target = target;
        snapToTarget();
    }

    public void setInitialRoom(LushCaveStage.Room room) {
        this.currentRoom = room;
        this.transitioning = false;
        Vector2 clamped = computeRoomClampedPosition(room);
        baseCamPos.set(clamped);
        camera.position.set(baseCamPos.x, baseCamPos.y, 0.0f);
        camera.update();
    }

    public void transitionToRoom(LushCaveStage.Room newRoom) {
        if (newRoom == null || newRoom == currentRoom) return;
        this.currentRoom = newRoom;
        this.transitionStart.set(baseCamPos);
        this.transitionEnd.set(computeRoomClampedPosition(newRoom));
        this.transitionTimer = 0.0f;
        this.transitioning = true;
    }

    public boolean isTransitioning() {
        return transitioning;
    }

    public LushCaveStage.Room getCurrentRoom() {
        return currentRoom;
    }

    public void snapToTarget() {
        Vector2 desired = (currentRoom != null) ? computeRoomClampedPosition(currentRoom) : computeTargetPosition();
        baseCamPos.set(desired);
        camera.position.set(baseCamPos.x, baseCamPos.y, 0.0f);
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
        if (transitioning) {
            transitionTimer += dt;
            float t = MathUtils.clamp(transitionTimer / transitionDuration, 0.0f, 1.0f);
            // Smooth cubic ease-in-out (Celeste room pan feel)
            float eased = t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
            baseCamPos.set(
                MathUtils.lerp(transitionStart.x, transitionEnd.x, eased),
                MathUtils.lerp(transitionStart.y, transitionEnd.y, eased)
            );
            if (t >= 1.0f) {
                transitioning = false;
            }
        } else {
            Vector2 desired = (currentRoom != null) ? computeRoomClampedPosition(currentRoom) : computeTargetPosition();
            float expFactor = 1.0f - (float) Math.exp(-smoothSpeed * dt);
            baseCamPos.x = MathUtils.lerp(baseCamPos.x, desired.x, expFactor);
            baseCamPos.y = MathUtils.lerp(baseCamPos.y, desired.y, expFactor);
        }

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

        camera.position.set(baseCamPos.x + shakeOffset.x, baseCamPos.y + shakeOffset.y, 0.0f);
        camera.update();
    }

    private Vector2 computeRoomClampedPosition(LushCaveStage.Room room) {
        Vector2 desired = computeTargetPosition();
        float halfW = camera.viewportWidth * 0.5f;
        float halfH = camera.viewportHeight * 0.5f;

        float roomW = room.maxX - room.minX;
        float roomH = room.maxY - room.minY;

        float cx = (roomW <= camera.viewportWidth)
            ? (room.minX + room.maxX) * 0.5f
            : MathUtils.clamp(desired.x, room.minX + halfW, room.maxX - halfW);

        float cy = (roomH <= camera.viewportHeight)
            ? (room.minY + room.maxY) * 0.5f
            : MathUtils.clamp(desired.y, room.minY + halfH, room.maxY - halfH);

        return new Vector2(cx, cy);
    }

    private Vector2 computeTargetPosition() {
        Vector2 playerPos = target.getMotor().getPosition();
        float ahead = target.getFacingDirection() * lookAheadX;
        float tx = playerPos.x + baseOffset.x + ahead;
        float ty = playerPos.y + baseOffset.y;
        return new Vector2(tx, ty);
    }
}
