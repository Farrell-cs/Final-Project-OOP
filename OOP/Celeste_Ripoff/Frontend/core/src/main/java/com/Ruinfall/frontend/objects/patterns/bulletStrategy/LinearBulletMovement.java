package com.Ruinfall.frontend.objects.patterns.bulletStrategy;

import com.gylchrist.frontend.objects.bullets.Bullet;

public class LinearBulletMovement implements BulletMovementPattern {
    @Override
    public void move(Bullet bullet, float delta) {
        if (bullet != null) {
            float rad = (float) Math.toRadians(bullet.getAngle());
            float dx = (float) Math.cos(rad) * bullet.getSpeed() * delta;
            float dy = (float) Math.sin(rad) * bullet.getSpeed() * delta;
            bullet.setX(bullet.getX() + dx);
            bullet.setY(bullet.getY() + dy);
        }
    }
}
