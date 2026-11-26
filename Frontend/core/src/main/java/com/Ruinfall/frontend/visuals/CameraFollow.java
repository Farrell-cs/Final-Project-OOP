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
    private final float fallbackViewportWidth;
    private final float fallbackViewportHeight;

    private final Vector2 baseOffset = new Vector2(0.0f, 2.0f);
    private float lookAheadX = 1.4f;
    private float smoothSpeed = 10.0f;

    // Calm deadzone + smoothed look-ahead with direction-flip hysteresis
    private final Vector2 deadzoneFocus = new Vector2();
    private float deadzoneHalfX = 1.65f;
    private float deadzoneHalfY = 1.25f;
    private float currentLookAheadX = 0.0f;
    private int stableFacingDir = 1;
    private float facingFlipTimer = 0.0f;
    private static final float FACING_HYSTERESIS_TIME = 0.18f;
    private static final float LOOK_AHEAD_SMOOTH_SPEED = 3.2f;

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
        this.fallbackViewportWidth = 24.89f;
        this.fallbackViewportHeight = 14.0f;
        snapToTarget();
    }

    /**
     * Headless-safe constructor (no native Matrix4/GL calls) for unit testing camera math.
     */
    public CameraFollow(float viewportWidth, float viewportHeight, PlayerController target) {
        this.camera = null;
        this.target = target;
        this.fallbackViewportWidth = viewportWidth;
        this.fallbackViewportHeight = viewportHeight;
        snapToTarget();
    }

    public void setInitialRoom(LushCaveStage.Room room) {
        this.currentRoom = room;
        snapToTarget();
    }

    public void transitionToRoom(LushCaveStage.Room newRoom) {
        if (newRoom == null || newRoom == currentRoom) return;
        this.currentRoom = newRoom;
        Vector2 playerPos = target.getMotor().getPosition();
        deadzoneFocus.set(playerPos.x + baseOffset.x, playerPos.y + baseOffset.y);
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
        transitioning = false;
        transitionTimer = 0.0f;
        shakeTimer = 0.0f;
        trauma = 0.0f;
        shakeOffset.setZero();
        currentLookAheadX = 0.0f;
        facingFlipTimer = 0.0f;
        stableFacingDir = target.getFacingDirection();

        Vector2 playerPos = target.getMotor().getPosition();
        deadzoneFocus.set(playerPos.x + baseOffset.x, playerPos.y + baseOffset.y);

        Vector2 desired = (currentRoom != null) ? computeRoomClampedPosition(currentRoom) : computeTargetPosition();
        baseCamPos.set(desired);
        if (camera != null) {
            camera.position.set(baseCamPos.x, baseCamPos.y, 0.0f);
            camera.update();
        }
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
                Vector2 playerPos = target.getMotor().getPosition();
                deadzoneFocus.set(playerPos.x + baseOffset.x, playerPos.y + baseOffset.y);
            }
        } else {
            updateDeadzoneAndLookAhead(dt);
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

        if (camera != null) {
            camera.position.set(baseCamPos.x + shakeOffset.x, baseCamPos.y + shakeOffset.y, 0.0f);
            camera.update();
        }
    }

    private void updateDeadzoneAndLookAhead(float dt) {
        Vector2 playerPos = target.getMotor().getPosition();
        float focusX = playerPos.x + baseOffset.x;
        float focusY = playerPos.y + baseOffset.y;

        float dx = focusX - deadzoneFocus.x;
        if (Math.abs(dx) > deadzoneHalfX) {
            deadzoneFocus.x += dx - Math.signum(dx) * deadzoneHalfX;
        }

        float dy = focusY - deadzoneFocus.y;
        if (Math.abs(dy) > deadzoneHalfY) {
            deadzoneFocus.y += dy - Math.signum(dy) * deadzoneHalfY;
        }

        int facing = target.getFacingDirection();
        if (facing != stableFacingDir) {
            facingFlipTimer += dt;
            if (facingFlipTimer >= FACING_HYSTERESIS_TIME) {
                stableFacingDir = facing;
                facingFlipTimer = 0.0f;
            }
        } else {
            facingFlipTimer = 0.0f;
        }

        float targetAhead = stableFacingDir * lookAheadX;
        float lookFactor = 1.0f - (float) Math.exp(-LOOK_AHEAD_SMOOTH_SPEED * dt);
        currentLookAheadX = MathUtils.lerp(currentLookAheadX, targetAhead, lookFactor);
    }

    private Vector2 computeRoomClampedPosition(LushCaveStage.Room room) {
        if (room.singleScreen) {
            return new Vector2(
                (room.minX + room.maxX) * 0.5f,
                (room.minY + room.maxY) * 0.5f
            );
        }

        Vector2 desired = computeTargetPosition();
        float viewW = (camera != null) ? camera.viewportWidth : fallbackViewportWidth;
        float viewH = (camera != null) ? camera.viewportHeight : fallbackViewportHeight;
        float halfW = viewW * 0.5f;
        float halfH = viewH * 0.5f;

        float roomW = room.maxX - room.minX;
        float roomH = room.maxY - room.minY;

        float cx = (roomW <= viewW)
            ? (room.minX + room.maxX) * 0.5f
            : MathUtils.clamp(desired.x, room.minX + halfW, room.maxX - halfW);

        float cy = (roomH <= viewH)
            ? (room.minY + room.maxY) * 0.5f
            : MathUtils.clamp(desired.y, room.minY + halfH, room.maxY - halfH);

        return new Vector2(cx, cy);
    }

    public Vector2 getCameraPos() {
        return camera != null
            ? new Vector2(camera.position.x, camera.position.y)
            : new Vector2(baseCamPos.x + shakeOffset.x, baseCamPos.y + shakeOffset.y);
    }

    private Vector2 computeTargetPosition() {
        return new Vector2(deadzoneFocus.x + currentLookAheadX, deadzoneFocus.y);
    }

    public float getCurrentLookAheadX() {
        return currentLookAheadX;
    }

    public Vector2 getDeadzoneFocus() {
        return deadzoneFocus;
    }

    public Vector2 getBaseCamPos() {
        return baseCamPos;
    }
}
