package com.Ruinfall.frontend.objects.patterns.projectile;

import com.Ruinfall.frontend.objects.bullets.Bullet;
import com.Ruinfall.frontend.systems.BulletManager;

/**
 * Movement and lifecycle strategy for projectiles.
 */
public interface IBulletBehavior {
    /**
     * Called once per frame while the bullet is active.
     */
    void update(Bullet bullet, float dt);

    /**
     * Called when the bullet is destroyed.
     *
     * @param bullet        the bullet being destroyed
     * @param bulletManager manager available for spawning child projectiles (may be null)
     */
    default void onDestroy(Bullet bullet, BulletManager bulletManager) {
    }

    /**
     * Returns an independent instance for per-bullet stateful behaviors.
     */
    default IBulletBehavior copy() {
        return this;
    }
}
