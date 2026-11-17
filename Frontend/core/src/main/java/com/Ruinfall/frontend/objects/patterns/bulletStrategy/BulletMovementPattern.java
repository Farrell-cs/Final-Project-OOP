package com.Ruinfall.frontend.objects.patterns.bulletStrategy;

import com.gylchrist.frontend.objects.bullets.Bullet;

public interface BulletMovementPattern {
    void move(Bullet bullet, float delta);
}
