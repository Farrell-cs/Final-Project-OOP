package com.Ruinfall.frontend.objects.patterns.projectile;

import com.Ruinfall.frontend.objects.bullets.Bullet;
import com.Ruinfall.frontend.systems.BulletManager;
import com.Ruinfall.frontend.systems.GameEventBus;
import com.Ruinfall.frontend.visuals.EffectManager;
import com.badlogic.gdx.math.MathUtils;

/**
 * Shared cadence and bullet-spawning helper for IShootingPattern implementations.
 */
public abstract class AbstractShootingPattern implements IShootingPattern {
    protected float cadence;
    protected float bulletSpeed;
    protected int damage;
    protected float bulletSize;
    protected float cooldownTimer;

    protected AbstractShootingPattern(float cadence, float bulletSpeed, int damage, float bulletSize) {
        this.cadence = Math.max(0.01f, cadence);
        this.bulletSpeed = bulletSpeed;
        this.damage = damage;
        this.bulletSize = bulletSize > 0f ? bulletSize : 0.45f;
        this.cooldownTimer = 0f;
    }

    @Override
    public float getCadence() {
        return cadence;
    }

    @Override
    public void update(
        float dt,
        float originX,
        float originY,
        float targetX,
        float targetY,
        BulletManager bulletManager,
        IBulletBehavior behavior,
        boolean isPlayer
    ) {
        cooldownTimer -= dt;
        if (cooldownTimer <= 0f) {
            execute(originX, originY, targetX, targetY, bulletManager, behavior, isPlayer);
            cooldownTimer += cadence;
            if (cooldownTimer < 0f) {
                cooldownTimer = cadence;
            }
        }
    }

    @Override
    public void reset() {
        cooldownTimer = 0f;
    }

    protected Bullet spawnWithAngle(
        float originX,
        float originY,
        float angleDeg,
        float speed,
        BulletManager bulletManager,
        IBulletBehavior behavior,
        boolean isPlayer
    ) {
        if (bulletManager == null) {
            return null;
        }
        float rad = angleDeg * MathUtils.degreesToRadians;
        float vx = MathUtils.cos(rad) * speed;
        float vy = MathUtils.sin(rad) * speed;

        Bullet bullet = isPlayer
            ? bulletManager.spawnPlayerBullet(originX - bulletSize * 0.5f, originY - bulletSize * 0.5f, vx, vy, damage)
            : bulletManager.spawnEnemyBullet(originX - bulletSize * 0.5f, originY - bulletSize * 0.5f, vx, vy, damage);

        if (bullet != null) {
            bullet.setSize(bulletSize, bulletSize);
            IBulletBehavior effective = (behavior != null) ? behavior.copy() : new LinearBullet();
            bullet.setBehavior(effective);
            GameEventBus.getInstance().publish(
                GameEventBus.GameEvent.BULLET_SPAWNED,
                new EffectManager.BulletVfxPayload(originX, originY, vx, vy, effective instanceof BubbleBullet, isPlayer)
            );
        }
        return bullet;
    }

    protected float angleToTargetDeg(float originX, float originY, float targetX, float targetY) {
        return MathUtils.atan2(targetY - originY, targetX - originX) * MathUtils.radiansToDegrees;
    }
}
