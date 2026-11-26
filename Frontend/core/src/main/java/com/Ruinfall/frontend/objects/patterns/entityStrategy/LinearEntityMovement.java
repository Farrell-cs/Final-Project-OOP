package com.Ruinfall.frontend.objects.patterns.entityStrategy;

import com.Ruinfall.frontend.objects.GameObject;

public class LinearEntityMovement implements EntityMovementPattern {
    private float vx;
    private float vy;

    public LinearEntityMovement(float vx, float vy) {
        this.vx = vx;
        this.vy = vy;
    }

    @Override
    public void move(GameObject entity, float delta) {
        if (entity != null) {
            entity.setX(entity.getX() + vx * delta);
            entity.setY(entity.getY() + vy * delta);
        }
    }

    public float getVx() { return vx; }
    public void setVx(float vx) { this.vx = vx; }
    public float getVy() { return vy; }
    public void setVy(float vy) { this.vy = vy; }
}
