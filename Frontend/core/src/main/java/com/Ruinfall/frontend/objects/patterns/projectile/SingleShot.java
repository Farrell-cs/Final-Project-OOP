package com.Ruinfall.frontend.objects.patterns.projectile;

import com.Ruinfall.frontend.systems.BulletManager;

/**
 * Fires a single projectile aimed at the target (with optional angle offset).
 */
public class SingleShot extends AbstractShootingPattern {
    private final float angleOffsetDeg;

    public SingleShot(float cadence, float bulletSpeed, int damage) {
        this(cadence, bulletSpeed, damage, 0f, 0.45f);
    }

    public SingleShot(float cadence, float bulletSpeed, int damage, float angleOffsetDeg, float bulletSize) {
        super(cadence, bulletSpeed, damage, bulletSize);
        this.angleOffsetDeg = angleOffsetDeg;
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
        float aimDeg = angleToTargetDeg(originX, originY, targetX, targetY) + angleOffsetDeg;
        spawnWithAngle(originX, originY, aimDeg, bulletSpeed, bulletManager, behavior, isPlayer);
    }
}
