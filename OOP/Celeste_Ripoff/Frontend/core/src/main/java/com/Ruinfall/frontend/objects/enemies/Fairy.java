package com.Ruinfall.frontend.objects.enemies;

import com.badlogic.gdx.graphics.Color;
import com.Ruinfall.frontend.objects.patterns.entityStrategy.LinearEntityMovement;
import com.Ruinfall.frontend.objects.patterns.shootingStrategy.FrontShot;

public class Fairy extends Enemy {
    private float shootTimer = 0f;
    private float shootDelay = 0.67f;
    private float shootInterval = 1.4f;
    private int maxShots = 2;
    private int shotsFired = 0;

    public Fairy(String name, int hp) {
        super(150, 380, 24, 24, Color.PINK, name, hp, 500L);
        // TODO (1 poin): Suntikkan strategi bawaan FrontShot(150f, 10) dan LinearEntityMovement(0f, -40f)
        setShootingPattern(new FrontShot(150f, 10));
        setMovementPattern(new LinearEntityMovement(0f, -40f));
        this.name = name;
        this.maxHP = hp;
    }

    public Fairy(float x, float y, String name, int hp) {
        super(x, y, 24, 24, Color.PINK, name, hp, 500L);
        // TODO: Suntikkan strategi bawaan yang sama
        setShootingPattern(new FrontShot(150f, 10));
        setMovementPattern(new LinearEntityMovement(0f, -40f));
        this.x = x;
        this.y = y;
        this.name = name;
        this.maxHP = hp;
    }

    public Fairy(float x, float y, String name, int hp, float shootDelay, float shootInterval, int maxShots) {
        super(x, y, 24, 24, Color.PINK, name, hp, 500L);
        this.shootDelay = shootDelay;
        this.shootInterval = shootInterval;
        this.maxShots = maxShots;
        // TODO (2 poin): Suntikkan strategi bawaan FrontShot dan LinearEntityMovement
        setShootingPattern(new FrontShot(150f, 10));
        setMovementPattern(new LinearEntityMovement(0f, -40f));
    }

    public boolean canShoot(float delta) {
        shootTimer += delta;
        if (shotsFired == 0) {
            if (shootTimer >= shootDelay) {
                shotsFired++;
                shootTimer = 0f;
                return true;
            }
        } else if (shotsFired < maxShots) {
            if (shootTimer >= shootInterval) {
                shotsFired++;
                shootTimer = 0f;
                return true;
            }
        }
        return false;
    }

    public void setShootTiming(float shootDelay, float shootInterval, int maxShots) {
        this.shootDelay = shootDelay;
        this.shootInterval = shootInterval;
        this.maxShots = maxShots;
    }

    public int getShotsFired() { return shotsFired; }
    public float getShootTimer() { return shootTimer; }
    public float getShootDelay() { return shootDelay; }
    public float getShootInterval() { return shootInterval; }
    public int getMaxShots() { return maxShots; }
}
