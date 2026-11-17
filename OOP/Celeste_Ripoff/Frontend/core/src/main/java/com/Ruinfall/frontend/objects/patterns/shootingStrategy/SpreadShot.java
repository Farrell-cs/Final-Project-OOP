package com.Ruinfall.frontend.objects.patterns.shootingStrategy;

import com.gylchrist.frontend.objects.GameObject;
import com.gylchrist.frontend.objects.bullets.Bullet;
import com.gylchrist.frontend.objects.patterns.ShootingPattern;
import com.gylchrist.frontend.objects.patterns.bulletStrategy.BulletMovementPattern;
import com.gylchrist.frontend.systems.BulletManager;

public class SpreadShot implements ShootingPattern {
    private float speed;
    private int numBullets;
    private float spreadAngleDegrees;
    private GameObject target;
    private float horizontalSpacing = 0f;
    private BulletMovementPattern bulletMovementPattern;
    private String spriteKey = null;
    private int damage = 1;

    // Konstruktor Spreadshot dengan sudut statis
    public SpreadShot(float speed, int numBullets, float spreadAngleDegrees) {
        this(speed, numBullets, spreadAngleDegrees, null);
    }

    // Konstruktor Spreadshot terarah (FAN_AIMED) yang membidik target
    public SpreadShot(float speed, int numBullets, float spreadAngleDegrees, GameObject target) {
        this.speed = speed;
        this.numBullets = numBullets;
        this.spreadAngleDegrees = spreadAngleDegrees;
        this.target = target;
    }

    // Master Constructor: Menampung seluruh atribut dan konfigurasi
    public SpreadShot(float speed, int numBullets, float spreadAngleDegrees, int damage, float horizontalSpacing,
                      BulletMovementPattern bulletMovementPattern, GameObject target, String spriteKey) {
        this.speed = speed;
        this.numBullets = numBullets;
        this.spreadAngleDegrees = spreadAngleDegrees;
        this.damage = damage;
        this.horizontalSpacing = horizontalSpacing;
        this.bulletMovementPattern = bulletMovementPattern;
        this.target = target;
        this.spriteKey = spriteKey;
    }

    // Konstruktor baru: menyuntikkan strategi trajektori (BulletMovementPattern)
    public SpreadShot(float speed, int numBullets, float spreadAngleDegrees, int damage, BulletMovementPattern bulletMovementPattern) {
        this(speed, numBullets, spreadAngleDegrees, damage, 0f, bulletMovementPattern, null, null);
    }

    // Konstruktor baru: menyuntikkan target dan sprite warna peluru (misal: "bullet_danmaku_red")
    public SpreadShot(float speed, int numBullets, float spreadAngleDegrees, int damage, GameObject target, String spriteKey) {
        this(speed, numBullets, spreadAngleDegrees, damage, 0f, null, target, spriteKey);
    }


    @Override
    public void execute(float originX, float originY, BulletManager bulletManager, boolean isPlayer) {
        // Sudut dasar: Jika ada target, bidik target; jika tidak, gunakan 90° (player) / 270° (Musuh)
        float centerAngle;
        if (target != null) {
            float targetCenterX = target.getX() + target.getWidth() / 2f;
            float targetCenterY = target.getY() + target.getHeight() / 2f;
            centerAngle = (float) Math.toDegrees(Math.atan2(targetCenterY - originY, targetCenterX - originX));
        } else {
            centerAngle = isPlayer ? 90f : 270f;
        }

        float startAngle = centerAngle - (spreadAngleDegrees / 2f);
        float angleStep = (numBullets > 1) ? (spreadAngleDegrees / (numBullets - 1)) : 0;
        int damage = isPlayer ? 25 : 15;

        float startX = (horizontalSpacing > 0f && numBullets > 1) ? (originX - ((numBullets - 1) * horizontalSpacing / 2f)) : originX;

        for (int i = 0; i < numBullets; i++) {
            float angleDeg = startAngle + (i * angleStep);
            float angleRad = (float) Math.toRadians(angleDeg);

            float vx = (float) (speed * Math.cos(angleRad));
            float vy = (float) (speed * Math.sin(angleRad));
            float spawnX = (horizontalSpacing > 0f && numBullets > 1) ? (startX + i * horizontalSpacing) : originX;

            Bullet bullet;
            if (isPlayer) {
                bullet = bulletManager.spawnPlayerBullet(spawnX, originY, vx, vy, damage, spriteKey);
            } else {
                bullet = (spriteKey != null)
                    ? bulletManager.spawnEnemyBullet(spawnX, originY, vx, vy, damage, spriteKey)
                    : bulletManager.spawnEnemyBullet(spawnX, originY, vx, vy, damage);
            }

            // Suntikkan strategi trajektori ke peluru bila tersedia
            if (bullet != null && bulletMovementPattern != null) {
                bullet.setMovementPattern(bulletMovementPattern);
            }
        }

    }

    public float getSpeed() { return speed; }
    public void setSpeed(float speed) { this.speed = speed; }

    public int getNumBullets() { return numBullets; }
    public void setNumBullets(int numBullets) { this.numBullets = numBullets; }

    public float getSpreadAngleDegrees() { return spreadAngleDegrees; }
    public void setSpreadAngleDegrees(float spreadAngleDegrees) { this.spreadAngleDegrees = spreadAngleDegrees; }

    public GameObject getTarget() { return target; }
    public void setTarget(GameObject target) { this.target = target; }

    public float getHorizontalSpacing() { return horizontalSpacing; }
    public void setHorizontalSpacing(float horizontalSpacing) { this.horizontalSpacing = horizontalSpacing; }

    public BulletMovementPattern getBulletMovementPattern() { return bulletMovementPattern; }
    public void setBulletMovementPattern(BulletMovementPattern bulletMovementPattern) { this.bulletMovementPattern = bulletMovementPattern; }

    public String getSpriteKey() { return spriteKey; }
    public void setSpriteKey(String spriteKey) { this.spriteKey = spriteKey; }
}

