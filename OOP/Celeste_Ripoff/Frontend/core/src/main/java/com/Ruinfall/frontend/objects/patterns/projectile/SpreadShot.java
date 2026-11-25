package com.Ruinfall.frontend.objects.patterns.projectile;

import com.Ruinfall.frontend.systems.BulletManager;

/**
 * Fires a fan of bullets centered on the target direction.
 * Supports optional alternating angular offset between shots for Stage 3 patterns.
 */
public class SpreadShot extends AbstractShootingPattern {
    private final int count;
    private final float spreadAngleDeg;
    private final float alternatingOffsetDeg;
    private boolean flipPhase;

    public SpreadShot(float cadence, float bulletSpeed, int damage, int count, float spreadAngleDeg) {
        this(cadence, bulletSpeed, damage, count, spreadAngleDeg, 0f, 0.45f);
    }

    public SpreadShot(
        float cadence,
        float bulletSpeed,
        int damage,
        int count,
        float spreadAngleDeg,
        float alternatingOffsetDeg,
        float bulletSize
    ) {
        super(cadence, bulletSpeed, damage, bulletSize);
        this.count = Math.max(1, count);
        this.spreadAngleDeg = spreadAngleDeg;
        this.alternatingOffsetDeg = alternatingOffsetDeg;
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
        float centerDeg = angleToTargetDeg(originX, originY, targetX, targetY);
        if (alternatingOffsetDeg != 0f) {
            centerDeg += flipPhase ? alternatingOffsetDeg : -alternatingOffsetDeg;
            flipPhase = !flipPhase;
        }

        if (count == 1) {
            spawnWithAngle(originX, originY, centerDeg, bulletSpeed, bulletManager, behavior, isPlayer);
            return;
        }

        float startDeg = centerDeg - spreadAngleDeg * 0.5f;
        float stepDeg = spreadAngleDeg / (count - 1);
        for (int i = 0; i < count; i++) {
            spawnWithAngle(originX, originY, startDeg + i * stepDeg, bulletSpeed, bulletManager, behavior, isPlayer);
        }
    }

    @Override
    public void reset() {
        super.reset();
        flipPhase = false;
    }
}
