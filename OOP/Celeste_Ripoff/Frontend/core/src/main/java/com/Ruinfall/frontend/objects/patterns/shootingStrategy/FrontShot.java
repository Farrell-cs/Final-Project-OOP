package com.Ruinfall.frontend.objects.patterns.shootingStrategy;

import com.gylchrist.frontend.objects.bullets.Bullet;
import com.gylchrist.frontend.objects.patterns.ShootingPattern;
import com.gylchrist.frontend.objects.patterns.bulletStrategy.BulletMovementPattern;
import com.gylchrist.frontend.systems.BulletManager;

public class FrontShot implements ShootingPattern {
    private float bulletSpeed;
    private int damage;
    private BulletMovementPattern bulletMovementPattern;

    public FrontShot(float bulletSpeed, int damage, BulletMovementPattern bulletMovementPattern) {
        this.bulletSpeed = bulletSpeed;
        this.damage = damage;
        this.bulletMovementPattern = bulletMovementPattern;
    }

    public FrontShot(float bulletSpeed, int damage) {
        this(bulletSpeed, damage, null);
    }

    @Override
    public void execute(float originX, float originY, BulletManager bulletManager, boolean isPlayer) {
        if (bulletManager == null) return;

        float angleDeg = isPlayer ? 90.0f : 270.0f;
        float rad = (float) Math.toRadians(angleDeg);
        float vx = (float) (bulletSpeed * Math.cos(rad));
        float vy = (float) (bulletSpeed * Math.sin(rad));

        Bullet bullet;
        if (isPlayer) {
            bullet = bulletManager.spawnPlayerBullet(originX, originY, vx, vy, damage);
        } else {
            bullet = bulletManager.spawnEnemyBullet(originX, originY, vx, vy, damage);
        }

        if (bullet != null && bulletMovementPattern != null) {
            bullet.setMovementPattern(bulletMovementPattern);
        }
    }

    public float getBulletSpeed() { return bulletSpeed; }
    public void setBulletSpeed(float bulletSpeed) { this.bulletSpeed = bulletSpeed; }
    public int getDamage() { return damage; }
    public void setDamage(int damage) { this.damage = damage; }
    public BulletMovementPattern getBulletMovementPattern() { return bulletMovementPattern; }
    public void setBulletMovementPattern(BulletMovementPattern bulletMovementPattern) { this.bulletMovementPattern = bulletMovementPattern; }
}
