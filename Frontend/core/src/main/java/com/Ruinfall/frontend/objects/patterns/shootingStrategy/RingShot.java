// RingShot.java
package com.Ruinfall.frontend.objects.patterns.shootingStrategy;

import com.gylchrist.frontend.objects.bullets.Bullet;
import com.gylchrist.frontend.objects.patterns.ShootingPattern;
import com.gylchrist.frontend.objects.patterns.bulletStrategy.BulletMovementPattern;
import com.gylchrist.frontend.systems.BulletManager;

public class RingShot implements ShootingPattern {
    private float bulletSpeed;
    private int bulletCount;
    private int damage;
    private BulletMovementPattern bulletMovementPattern;

    public RingShot(float bulletSpeed, int bulletCount, int damage, BulletMovementPattern bulletMovementPattern) {
        this.bulletSpeed = bulletSpeed;
        this.bulletCount = Math.max(3, bulletCount);
        this.damage = damage;
        this.bulletMovementPattern = bulletMovementPattern;
    }

    public RingShot(float bulletSpeed, int bulletCount, int damage) {
        this(bulletSpeed, bulletCount, damage, null);
    }

    @Override
    public void execute(float originX, float originY, BulletManager bulletManager, boolean isPlayer) {
        if (bulletManager == null) return;
        float angleStep = 360f / bulletCount;
        for (int i = 0; i < bulletCount; i++) {
            float rad = (float) Math.toRadians(i * angleStep);
            float vx = (float) (Math.cos(rad) * bulletSpeed);
            float vy = (float) (Math.sin(rad) * bulletSpeed);
            Bullet bullet = isPlayer
                ? bulletManager.spawnPlayerBullet(originX, originY, vx, vy, damage)
                : bulletManager.spawnEnemyBullet(originX, originY, vx, vy, damage);
            if (bullet != null && bulletMovementPattern != null) bullet.setMovementPattern(bulletMovementPattern);
        }
    }

    public float getBulletSpeed() { return bulletSpeed; }
    public void setBulletSpeed(float bulletSpeed) { this.bulletSpeed = bulletSpeed; }
    public int getBulletCount() { return bulletCount; }
    public void setBulletCount(int bulletCount) { this.bulletCount = bulletCount; }
    public int getDamage() { return damage; }
    public void setDamage(int damage) { this.damage = damage; }
    public BulletMovementPattern getBulletMovementPattern() { return bulletMovementPattern; }
    public void setBulletMovementPattern(BulletMovementPattern bulletMovementPattern) { this.bulletMovementPattern = bulletMovementPattern; }
}
