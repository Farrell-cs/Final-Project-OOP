package com.Ruinfall.frontend.objects.patterns.entityStrategy;

import com.Ruinfall.frontend.objects.GameObject;

public class TargetPointMovement implements EntityMovementPattern {
    private float targetX;
    private float targetY;
    private float speed;

    public TargetPointMovement(float targetX, float targetY, float speed) {
        this.targetX = targetX;
        this.targetY = targetY;
        this.speed = speed;
    }

    @Override
    public void move(GameObject entity, float delta) {
        if (entity == null) return;

        float currentX = entity.getX();
        float currentY = entity.getY();
        float dx = targetX - currentX;
        float dy = targetY - currentY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        float step = delta * speed;
        if (distance <= step || distance < 1f) {
            entity.setX(targetX);
            entity.setY(targetY);
        } else {
            entity.setX(currentX + (dx / distance) * step);
            entity.setY(currentY + (dy / distance) * step);
        }
    }

    public boolean hasArrived(GameObject entity) {
        if (entity == null) return false;
        float dx = targetX - entity.getX();
        float dy = targetY - entity.getY();
        return (dx * dx + dy * dy) < 1f;
    }

    public float getTargetX() { return targetX; }
    public float getTargetY() { return targetY; }
    public void setTargetPoint(float targetX, float targetY) {
        this.targetX = targetX;
        this.targetY = targetY;
    }
    public float getSpeed() { return speed; }
    public void setSpeed(float speed) { this.speed = speed; }
}
