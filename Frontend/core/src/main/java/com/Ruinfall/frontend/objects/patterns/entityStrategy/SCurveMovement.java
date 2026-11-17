package com.Ruinfall.frontend.objects.patterns.entityStrategy;

import com.gylchrist.frontend.objects.GameObject;

public class SCurveMovement implements EntityMovementPattern {
    private float speed;
    private float angle;
    private boolean mirrored;
    private float timer = 0f;

    public SCurveMovement(float speed, float initialAngle, boolean mirrored) {
        this.speed = speed;
        this.angle = initialAngle;
        this.mirrored = mirrored;
    }

    @Override
    public void move(GameObject entity, float delta) {
        if (entity != null) {
            timer += delta;
            float w;
            if (timer < 0.67f) {
                w = 0f;
            } else if (timer < 2.0f) {
                w = mirrored ? -1.4726f : 1.4726f;
            } else if (timer < 3.67f) {
                w = mirrored ? 1.1781f : -1.1781f;
            } else {
                w = 0f;
            }

            angle += w * delta;
            float vx = (float) Math.cos(angle) * speed;
            float vy = (float) Math.sin(angle) * speed;

            entity.setX(entity.getX() + vx * delta);
            entity.setY(entity.getY() + vy * delta);
        }
    }

    public float getSpeed() { return speed; }
    public void setSpeed(float speed) { this.speed = speed; }
    public float getAngle() { return angle; }
    public void setAngle(float angle) { this.angle = angle; }
    public boolean isMirrored() { return mirrored; }
    public void setMirrored(boolean mirrored) { this.mirrored = mirrored; }
}
