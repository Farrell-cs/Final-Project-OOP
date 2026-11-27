package com.Ruinfall.frontend.objects.patterns.projectile;

import com.Ruinfall.frontend.systems.BulletManager;

/**
 * Fires an evenly spaced 360-degree ring of projectiles, optionally rotating phase each burst.
 */
public class RadialBurst extends AbstractShootingPattern {
    private final int count;
    private final float initialOffsetDeg;
    private final float rotationPerBurstDeg;
    private float currentOffsetDeg;

    public RadialBurst(float cadence, float bulletSpeed, int damage, int count) {
        this(cadence, bulletSpeed, damage, count, 0f, 15f, 0.45f);
    }

    public RadialBurst(
        float cadence,
        float bulletSpeed,
        int damage,
        int count,
        float initialOffsetDeg,
        float rotationPerBurstDeg,
        float bulletSize
    ) {
        super(cadence, bulletSpeed, damage, bulletSize);
        this.count = Math.max(1, count);
        this.initialOffsetDeg = initialOffsetDeg;
        this.rotationPerBurstDeg = rotationPerBurstDeg;
        this.currentOffsetDeg = initialOffsetDeg;
    }

    @Override
    public void execute(
        float originX,
        float originY,
        float targetX,
        float targetY,
        BulletManager bulletManager,
        IBulletBehavior behavior,
        boolean isPlayer
    ) {
        float stepDeg = 360f / count;
        for (int i = 0; i < count; i++) {
            float angleDeg = currentOffsetDeg + i * stepDeg;
            spawnWithAngle(originX, originY, angleDeg, bulletSpeed, bulletManager, behavior, isPlayer);
        }
        currentOffsetDeg = (currentOffsetDeg + rotationPerBurstDeg) % 360f;
    }

    @Override
    public void reset() {
        super.reset();
        currentOffsetDeg = initialOffsetDeg;
    }
}
