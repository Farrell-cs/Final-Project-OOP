package com.Ruinfall.frontend.objects.patterns.projectile;

import com.Ruinfall.frontend.objects.bullets.Bullet;
import com.badlogic.gdx.math.Rectangle;

/**
 * Constant-velocity projectile behavior that destroys itself when leaving bounds.
 * Player-hit destruction is handled by Bullet collision.
 */
public class LinearBullet implements IBulletBehavior {
    private final float minX;
    private final float minY;
    private final float maxX;
    private final float maxY;
    private final float margin;

    public LinearBullet() {
        this(-16f, -4f, 16f, 16f, 2.0f);
    }

    public LinearBullet(Rectangle bounds, float margin) {
        this(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height, margin);
    }

    public LinearBullet(float minX, float minY, float maxX, float maxY, float margin) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.margin = margin;
    }

    @Override
    public void update(Bullet bullet, float dt) {
        if (bullet == null || bullet.isDestroyed()) {
            return;
        }

        bullet.setX(bullet.getX() + bullet.getVx() * dt);
        bullet.setY(bullet.getY() + bullet.getVy() * dt);

        float cx = bullet.getX() + bullet.getWidth() * 0.5f;
        float cy = bullet.getY() + bullet.getHeight() * 0.5f;
        if (cx < minX - margin || cx > maxX + margin || cy < minY - margin || cy > maxY + margin) {
            bullet.destroy();
        }
    }

    @Override
    public IBulletBehavior copy() {
        return new LinearBullet(minX, minY, maxX, maxY, margin);
    }
}
