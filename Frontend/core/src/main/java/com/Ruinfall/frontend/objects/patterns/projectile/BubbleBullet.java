package com.Ruinfall.frontend.objects.patterns.projectile;

import com.Ruinfall.frontend.objects.bullets.Bullet;
import com.Ruinfall.frontend.systems.BulletManager;
import com.Ruinfall.frontend.systems.GameEventBus;
import com.Ruinfall.frontend.visuals.EffectManager;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

/**
 * Slow-moving projectile with high drag and subtle drift.
 * Spawns with a randomized TTL in [minTtl, maxTtl]. On timeout, destroys itself
 * and triggers a 4-way cardinal (+ or x) LinearBullet burst.
 */
public class BubbleBullet implements IBulletBehavior {
    private final float minTtl;
    private final float maxTtl;
    private final float drag;
    private final float driftAmplitude;
    private final float driftFrequency;
    private final float burstSpeed;
    private final int burstDamage;
    private final boolean diagonalBurst;
    private final Rectangle bounds;
    private final float boundsMargin;

    private float ttl;
    private float elapsed;
    private boolean timedOut;

    public BubbleBullet(
        float minTtl,
        float maxTtl,
        float drag,
        float driftAmplitude,
        float driftFrequency,
        float burstSpeed,
        int burstDamage,
        boolean diagonalBurst,
        Rectangle bounds
    ) {
        this.minTtl = Math.max(0.05f, minTtl);
        this.maxTtl = Math.max(this.minTtl, maxTtl);
        this.drag = Math.max(0f, drag);
        this.driftAmplitude = driftAmplitude;
        this.driftFrequency = driftFrequency;
        this.burstSpeed = burstSpeed;
        this.burstDamage = burstDamage;
        this.diagonalBurst = diagonalBurst;
        this.bounds = bounds != null ? new Rectangle(bounds) : new Rectangle(-16f, -4f, 32f, 20f);
        this.boundsMargin = 2.0f;
        this.ttl = MathUtils.random(this.minTtl, this.maxTtl);
    }

    public BubbleBullet(float minTtl, float maxTtl, float burstSpeed, boolean diagonalBurst, Rectangle bounds) {
        this(minTtl, maxTtl, 2.8f, 1.2f, 4.0f, burstSpeed, 1, diagonalBurst, bounds);
    }

    @Override
    public void update(Bullet bullet, float dt) {
        if (bullet == null || bullet.isDestroyed()) {
            return;
        }

        elapsed += dt;
        ttl -= dt;

        // Exponential-style drag damping
        float damp = Math.max(0f, 1.0f - drag * dt);
        float vx = bullet.getVx() * damp;
        float vy = bullet.getVy() * damp;
        bullet.setVx(vx);
        bullet.setVy(vy);

        // Gentle perpendicular/sinusoidal drift
        float driftX = MathUtils.cos(elapsed * driftFrequency) * driftAmplitude;
        float driftY = MathUtils.sin(elapsed * driftFrequency * 0.8f) * driftAmplitude;

        bullet.setX(bullet.getX() + (vx + driftX) * dt);
        bullet.setY(bullet.getY() + (vy + driftY) * dt);

        if (ttl <= 0f) {
            timedOut = true;
            bullet.destroy();
        }
    }

    @Override
    public void onDestroy(Bullet bullet, BulletManager bulletManager) {
        if (!timedOut || bullet == null || bulletManager == null) {
            return;
        }

        float originX = bullet.getX();
        float originY = bullet.getY();
        float baseAngleDeg = diagonalBurst ? 45f : 0f;
        boolean isPlayer = bullet.getIsPlayerBullet();

        GameEventBus.getInstance().publish(
            GameEventBus.GameEvent.BUBBLE_POPPED,
            new EffectManager.BubblePopPayload(
                originX + bullet.getWidth() * 0.5f,
                originY + bullet.getHeight() * 0.5f,
                diagonalBurst
            )
        );

        for (int i = 0; i < 4; i++) {
            float angleDeg = baseAngleDeg + i * 90f;
            float rad = angleDeg * MathUtils.degreesToRadians;
            float vx = MathUtils.cos(rad) * burstSpeed;
            float vy = MathUtils.sin(rad) * burstSpeed;

            Bullet child = isPlayer
                ? bulletManager.spawnPlayerBullet(originX, originY, vx, vy, burstDamage)
                : bulletManager.spawnEnemyBullet(originX, originY, vx, vy, burstDamage);

            if (child != null) {
                child.setSize(bullet.getWidth() * 0.75f, bullet.getHeight() * 0.75f);
                child.setBehavior(new LinearBullet(bounds, boundsMargin));
            }
        }
    }

    @Override
    public IBulletBehavior copy() {
        return new BubbleBullet(
            minTtl,
            maxTtl,
            drag,
            driftAmplitude,
            driftFrequency,
            burstSpeed,
            burstDamage,
            diagonalBurst,
            bounds
        );
    }

    public float getTtl() {
        return ttl;
    }

    public boolean didTimeOut() {
        return timedOut;
    }
}
