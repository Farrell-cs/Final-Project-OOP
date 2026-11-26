package com.Ruinfall.frontend.visuals;

import com.badlogic.gdx.math.MathUtils;

/**
 * Trauma-based decaying camera shake.
 * Uses quadratic trauma (trauma^2) for smooth, tactile impact decay.
 */
public class CameraShake {
    private float trauma;
    private float traumaDecay = 1.9f;
    private float maxOffset = 0.50f;
    private float elapsed;

    private float offsetX;
    private float offsetY;

    public void addTrauma(float amount) {
        trauma = MathUtils.clamp(trauma + Math.max(0f, amount), 0f, 1f);
    }

    public void update(float dt) {
        elapsed += dt;
        if (trauma <= 0f) {
            offsetX = 0f;
            offsetY = 0f;
            return;
        }

        float shake = trauma * trauma;
        float angle = elapsed * 65.0f;
        offsetX = MathUtils.sin(angle) * maxOffset * shake;
        offsetY = MathUtils.cos(angle * 1.37f) * maxOffset * shake;

        trauma = Math.max(0f, trauma - traumaDecay * dt);
    }

    public float getTrauma() {
        return trauma;
    }

    public float getOffsetX() {
        return offsetX;
    }

    public float getOffsetY() {
        return offsetY;
    }

    public void reset() {
        trauma = 0f;
        offsetX = 0f;
        offsetY = 0f;
    }
}
