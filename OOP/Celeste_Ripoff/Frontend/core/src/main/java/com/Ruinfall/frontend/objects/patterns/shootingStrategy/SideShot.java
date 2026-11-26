package com.Ruinfall.frontend.objects.patterns.shootingStrategy;

import com.Ruinfall.frontend.objects.bullets.Bullet;
import com.Ruinfall.frontend.objects.patterns.ShootingPattern;
import com.Ruinfall.frontend.objects.patterns.bulletStrategy.BulletMovementPattern;
import com.Ruinfall.frontend.systems.BulletManager;

/**
 * ShootingStrategy implementation dedicated strictly to starting POSITION on the flanks/sides.
 * 100% stateless Strategy: Purely calculates spawn coordinates and initial launch angles.
 *
 * In Touhou 6 (EoSD):
 * - Unfocused: +/-24px horizontal offset, -16px vertical offset, launched outward at 120° / 60° (V-shape flare)
 * - Focused (Shift): +/-8px horizontal offset, +16px vertical offset, launched forward at 100° / 80° (tight focus)
 *
 * The shooting entity (e.g. Player) controls firing cadence/cooldown natively (the ZUN way).
 * The bullet MOVEMENT after launch is delegated to the BulletMovementPattern (e.g. HomingBulletMovement).
 */
public class SideShot implements ShootingPattern {
    private float bulletSpeed;
    private int damage;
    private float horizontalOffset; // Lateral distance from entity center (e.g. 24px)
    private float verticalOffset;   // Vertical distance from entity nozzle (e.g. -16px)
    private float leftAngle;         // Launch angle for left flank (default: 120 deg in EoSD)
    private float rightAngle;        // Launch angle for right flank (default: 60 deg in EoSD)
    private boolean dualSides;       // true: both left & right; false: single flank
    private BulletMovementPattern bulletMovementPattern;
    private String spriteKey;

    public SideShot(float bulletSpeed, int damage, float horizontalOffset, float verticalOffset,
                    float leftAngle, float rightAngle, boolean dualSides,
                    BulletMovementPattern bulletMovementPattern, String spriteKey) {
        this.bulletSpeed = bulletSpeed;
        this.damage = damage;
        this.horizontalOffset = horizontalOffset;
        this.verticalOffset = verticalOffset;
        this.leftAngle = leftAngle;
        this.rightAngle = rightAngle;
        this.dualSides = dualSides;
        this.bulletMovementPattern = bulletMovementPattern;
        this.spriteKey = spriteKey;
    }

    public SideShot(float bulletSpeed, int damage, float horizontalOffset, float verticalOffset,
                    float leftAngle, float rightAngle, BulletMovementPattern bulletMovementPattern, String spriteKey) {
        this(bulletSpeed, damage, horizontalOffset, verticalOffset, leftAngle, rightAngle, true, bulletMovementPattern, spriteKey);
    }

    public SideShot(float bulletSpeed, int damage, float horizontalOffset, float verticalOffset,
                    BulletMovementPattern bulletMovementPattern, String spriteKey) {
        this(bulletSpeed, damage, horizontalOffset, verticalOffset, 120f, 60f, true, bulletMovementPattern, spriteKey);
    }

    public SideShot(float bulletSpeed, int damage, BulletMovementPattern bulletMovementPattern, String spriteKey) {
        this(bulletSpeed, damage, 24f, -16f, 120f, 60f, true, bulletMovementPattern, spriteKey);
    }

    public SideShot(float bulletSpeed, int damage, BulletMovementPattern bulletMovementPattern) {
        this(bulletSpeed, damage, 24f, -16f, 120f, 60f, true, bulletMovementPattern, null);
    }

    public SideShot(float bulletSpeed, int damage) {
        this(bulletSpeed, damage, 24f, -16f, 120f, 60f, true, null, null);
    }

    @Override
    public void execute(float originX, float originY, BulletManager bulletManager, boolean isPlayer) {
        if (bulletManager == null) return;

        // Left flank starting position & initial launch angle
        float leftX = originX - horizontalOffset;
        float leftY = originY + (isPlayer ? verticalOffset : -verticalOffset);
        float lAngle = isPlayer ? leftAngle : (leftAngle + 180f) % 360f;
        spawnFlankBullet(leftX, leftY, lAngle, bulletManager, isPlayer);

        // Right flank starting position & initial launch angle (if dual sides enabled)
        if (dualSides) {
            float rightX = originX + horizontalOffset;
            float rightY = originY + (isPlayer ? verticalOffset : -verticalOffset);
            float rAngle = isPlayer ? rightAngle : (rightAngle + 180f) % 360f;
            spawnFlankBullet(rightX, rightY, rAngle, bulletManager, isPlayer);
        }
    }

    private void spawnFlankBullet(float spawnX, float spawnY, float angle, BulletManager bulletManager, boolean isPlayer) {
        float rad = (float) Math.toRadians(angle);
        float vx = (float) (Math.cos(rad) * bulletSpeed);
        float vy = (float) (Math.sin(rad) * bulletSpeed);

        Bullet bullet;
        if (isPlayer) {
            bullet = bulletManager.spawnPlayerBullet(spawnX, spawnY, vx, vy, damage, spriteKey);
        } else {
            bullet = bulletManager.spawnEnemyBullet(spawnX, spawnY, vx, vy, damage);
        }

        if (bullet != null && bulletMovementPattern != null) {
            bullet.setMovementPattern(bulletMovementPattern);
        }
    }

    public float getBulletSpeed() { return bulletSpeed; }
    public void setBulletSpeed(float bulletSpeed) { this.bulletSpeed = bulletSpeed; }

    public int getDamage() { return damage; }
    public void setDamage(int damage) { this.damage = damage; }

    public float getHorizontalOffset() { return horizontalOffset; }
    public void setHorizontalOffset(float horizontalOffset) { this.horizontalOffset = horizontalOffset; }

    public float getVerticalOffset() { return verticalOffset; }
    public void setVerticalOffset(float verticalOffset) { this.verticalOffset = verticalOffset; }

    public float getLeftAngle() { return leftAngle; }
    public void setLeftAngle(float leftAngle) { this.leftAngle = leftAngle; }

    public float getRightAngle() { return rightAngle; }
    public void setRightAngle(float rightAngle) { this.rightAngle = rightAngle; }

    public boolean isDualSides() { return dualSides; }
    public void setDualSides(boolean dualSides) { this.dualSides = dualSides; }

    public BulletMovementPattern getBulletMovementPattern() { return bulletMovementPattern; }
    public void setBulletMovementPattern(BulletMovementPattern bulletMovementPattern) { this.bulletMovementPattern = bulletMovementPattern; }

    public String getSpriteKey() { return spriteKey; }
    public void setSpriteKey(String spriteKey) { this.spriteKey = spriteKey; }
}
