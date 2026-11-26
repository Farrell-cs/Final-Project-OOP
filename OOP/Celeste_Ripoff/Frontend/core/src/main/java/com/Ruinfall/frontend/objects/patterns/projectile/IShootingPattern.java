package com.Ruinfall.frontend.objects.patterns.projectile;

import com.Ruinfall.frontend.systems.BulletManager;

/**
 * Emitter strategy controlling spawn cadence, bullet count, angle offsets, and initial velocity vectors.
 */
public interface IShootingPattern {
    /**
     * Returns the spawn cadence (seconds between bursts/shots).
     */
    float getCadence();

    /**
     * Advances internal cadence timer and emits projectiles when ready.
     */
    void update(
        float dt,
        float originX,
        float originY,
        float targetX,
        float targetY,
        BulletManager bulletManager,
        IBulletBehavior behavior,
        boolean isPlayer
    );

    /**
     * Immediately emits one volley using the pattern's geometry.
     */
    void execute(
        float originX,
        float originY,
        float targetX,
        float targetY,
        BulletManager bulletManager,
        IBulletBehavior behavior,
        boolean isPlayer
    );

    /**
     * Resets cadence timers and internal phase state.
     */
    void reset();
}
