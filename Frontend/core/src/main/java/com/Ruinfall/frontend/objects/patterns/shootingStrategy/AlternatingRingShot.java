package com.Ruinfall.frontend.objects.patterns.shootingStrategy;

import com.gylchrist.frontend.objects.patterns.ShootingPattern;
import com.gylchrist.frontend.systems.BulletManager;

public class AlternatingRingShot implements ShootingPattern {
    private float baseSpeed;
    private int bulletCountPerRing;
    private int damage;
    private int ringLayers;
    private float speedStep;
    private float rotationOffset = 0f;
    private float rotationSpeed = 0f;

    public AlternatingRingShot(float baseSpeed, int bulletCountPerRing, int damage, int ringLayers, float speedStep) {
        this.baseSpeed = baseSpeed;
        this.bulletCountPerRing = Math.max(3, bulletCountPerRing);
        this.damage = damage;
        this.ringLayers = Math.max(1, ringLayers);
        this.speedStep = speedStep;
    }

    public AlternatingRingShot(float baseSpeed, int bulletCountPerRing, int damage) {
        this(baseSpeed, bulletCountPerRing, damage, 2, 35f);
    }

    @Override
    public void execute(float originX, float originY, BulletManager bulletManager, boolean isPlayer) {
        if (bulletManager == null) return;

        String[] layerColors = {"bullet_danmaku_red", "bullet_danmaku_blue", "bullet_danmaku_purple", "bullet_danmaku_green"};
        float angleStep = 360f / bulletCountPerRing;

        for (int l = 0; l < ringLayers; l++) {
            float layerSpeed = baseSpeed + l * speedStep;
            float layerPhase = rotationOffset + (angleStep / (float) ringLayers) * l;
            String spriteKey = layerColors[l % layerColors.length];

            for (int i = 0; i < bulletCountPerRing; i++) {
                float angle = layerPhase + i * angleStep;
                float rad = (float) Math.toRadians(angle);
                float vx = (float) (Math.cos(rad) * layerSpeed);
                float vy = (float) (Math.sin(rad) * layerSpeed);

                if (isPlayer) {
                    bulletManager.spawnPlayerBullet(originX, originY, vx, vy, damage);
                } else {
                    bulletManager.spawnEnemyBullet(originX, originY, vx, vy, damage, spriteKey);
                }
            }
        }

        rotationOffset = (rotationOffset + rotationSpeed) % 360f;
    }

    public void setRotationSpeed(float rotationSpeed) { this.rotationSpeed = rotationSpeed; }
    public float getBaseSpeed() { return baseSpeed; }
    public void setBaseSpeed(float baseSpeed) { this.baseSpeed = baseSpeed; }
    public int getBulletCountPerRing() { return bulletCountPerRing; }
    public void setBulletCountPerRing(int count) { this.bulletCountPerRing = count; }
    public int getDamage() { return damage; }
    public void setDamage(int damage) { this.damage = damage; }
}
