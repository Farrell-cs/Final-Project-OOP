package com.Ruinfall.frontend.objects.patterns.projectile;

import com.Ruinfall.frontend.systems.BulletManager;

/**
 * Fires continuous multi-arm spiral streams by advancing angle each cadence tick.
 */
public class SpiralStream extends AbstractShootingPattern {
    private final int armCount;
    private final float angularStepDeg;
    private final float initialAngleDeg;
    private float currentAngleDeg;

    public SpiralStream(float cadence, float bulletSpeed, int damage, int armCount, float angularStepDeg) {
        this(cadence, bulletSpeed, damage, armCount, angularStepDeg, 0f, 0.45f);
    }

    public SpiralStream(
        float cadence,
        float bulletSpeed,
        int damage,
        int armCount,
        float angularStepDeg,
        float initialAngleDeg,
        float bulletSize
    ) {
        super(cadence, bulletSpeed, damage, bulletSize);
        this.armCount = Math.max(1, armCount);
        this.angularStepDeg = angularStepDeg;
        this.initialAngleDeg = initialAngleDeg;
        this.currentAngleDeg = initialAngleDeg;
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
        float armSpacing = 360f / armCount;
        for (int i = 0; i < armCount; i++) {
            float angleDeg = currentAngleDeg + i * armSpacing;
            spawnWithAngle(originX, originY, angleDeg, bulletSpeed, bulletManager, behavior, isPlayer);
        }
        currentAngleDeg = (currentAngleDeg + angularStepDeg) % 360f;
    }

    @Override
    public void reset() {
        super.reset();
        currentAngleDeg = initialAngleDeg;
    }
}
