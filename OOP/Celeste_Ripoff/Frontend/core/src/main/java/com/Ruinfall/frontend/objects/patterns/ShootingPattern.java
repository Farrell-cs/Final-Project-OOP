package com.Ruinfall.frontend.objects.patterns;

import com.gylchrist.frontend.systems.BulletManager;

public interface ShootingPattern {
    void execute(float originX, float originY, BulletManager bulletManager, boolean isPlayer);
}

