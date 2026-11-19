package com.Ruinfall.frontend.objects.patterns;

import com.Ruinfall.frontend.systems.BulletManager;

public interface ShootingPattern {
    void execute(float originX, float originY, BulletManager bulletManager, boolean isPlayer);
}

