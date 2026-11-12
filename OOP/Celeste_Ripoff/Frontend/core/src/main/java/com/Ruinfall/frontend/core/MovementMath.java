package com.Ruinfall.frontend.core;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public final class MovementMath {
    private MovementMath() {}

    public static float approach(float current, float target, float maxDelta) {
        if (current < target) {
            return Math.min(current + maxDelta, target);
        } else {
            return Math.max(current - maxDelta, target);
        }
    }

    public static float computeHorizontalVelocity(
        float currentVx,
        int moveX,
        float maxRun,
        float runAccel,
        float runReduce,
        float airMult,
        boolean isGrounded,
        float dt
    ) {
        float mult = isGrounded ? 1.0f : airMult;
        float target = moveX * maxRun;

        boolean exceedingMaxInSameDirection =
            Math.abs(currentVx) > maxRun &&
            moveX != 0 &&
            (int) Math.signum(currentVx) == moveX;

        float rate = exceedingMaxInSameDirection ? runReduce : runAccel;
        return approach(currentVx, target, rate * mult * dt);
    }

    public static Vector2 quantizeEightWayAim(Vector2 rawInput, int fallbackFacing, float deadzone) {
        if (rawInput.len2() < deadzone * deadzone) {
            return new Vector2(fallbackFacing >= 0 ? 1.0f : -1.0f, 0.0f);
        }

        float angleRad = MathUtils.atan2(rawInput.y, rawInput.x);
        float step = MathUtils.PI * 0.25f;
        float snappedAngle = Math.round(angleRad / step) * step;

        float x = MathUtils.cos(snappedAngle);
        float y = MathUtils.sin(snappedAngle);

        if (Math.abs(x) < 0.001f) x = 0.0f;
        if (Math.abs(y) < 0.001f) y = 0.0f;

        Vector2 dir = new Vector2(x, y);
        return dir.nor();
    }
}
