package com.Ruinfall.frontend.objects.patterns.bulletStrategy;

import com.gylchrist.frontend.objects.GameObject;
import com.gylchrist.frontend.objects.bullets.Bullet;
import com.gylchrist.frontend.objects.enemies.Enemy;

import java.util.List;

/**
 * BulletStrategy implementation dedicated to MOVEMENT:
 * Manages the flight trajectory of homing bullets.
 * In Touhou 6 (EoSD):
 * 1. Bullets steer towards target for up to 40 frames (~0.67 - 0.8 seconds).
 * 2. If target is eliminated, it dynamically re-targets the nearest living enemy.
 * 3. Once homing expires or target is lost, direction locks and bullet accelerates forward up to 600 px/s (10 px/frame).
 */
public class HomingBulletMovement implements BulletMovementPattern {
    private GameObject target;
    private List<GameObject> entities;
    private float turnSpeed;
    private float maxHomingDuration = 0.8f; // Authentic EoSD homing window

    public HomingBulletMovement(GameObject target, float turnSpeed) {
        this(target, null, turnSpeed, 0.8f);
    }

    public HomingBulletMovement(GameObject target, List<GameObject> entities, float turnSpeed) {
        this(target, entities, turnSpeed, 0.8f);
    }

    public HomingBulletMovement(GameObject target, List<GameObject> entities, float turnSpeed, float maxHomingDuration) {
        this.target = target;
        this.entities = entities;
        this.turnSpeed = turnSpeed;
        this.maxHomingDuration = maxHomingDuration;
    }

    public HomingBulletMovement(List<GameObject> entities, float turnSpeed) {
        this(null, entities, turnSpeed, 0.8f);
    }

    public HomingBulletMovement(float turnSpeed) {
        this(null, null, turnSpeed, 0.8f);
    }

    public HomingBulletMovement() {
        this(null, null, 360f, 0.8f);
    }

    @Override
    public void move(Bullet bullet, float delta) {
        if (bullet == null) return;

        // 1. Determine active target:
        // Priority A: Fixed target if specified and alive/active
        // Priority B: Dynamically acquire nearest living Enemy from entities list
        GameObject activeTarget = target;
        if (activeTarget == null || activeTarget.isDestroyed() || !activeTarget.isActive()) {
            if (entities != null) {
                GameObject nearest = null;
                float minDistanceSq = Float.MAX_VALUE;
                for (GameObject obj : entities) {
                    if (obj instanceof Enemy enemy && !enemy.isDestroyed() && enemy.isActive() && enemy.isAlive()) {
                        float dx = enemy.getX() - bullet.getX();
                        float dy = enemy.getY() - bullet.getY();
                        float distSq = dx * dx + dy * dy;
                        if (distSq < minDistanceSq) {
                            minDistanceSq = distSq;
                            nearest = enemy;
                        }
                    }
                }
                activeTarget = nearest;
            }
        }

        // 2. EoSD Homing Window: Home for first maxHomingDuration (e.g. 0.8s)
        boolean isHomingActive = (bullet.getStateTime() <= maxHomingDuration);

        if (isHomingActive && activeTarget != null && !activeTarget.isDestroyed() && activeTarget.isActive()) {
            float targetAngle = (float) Math.toDegrees(Math.atan2(
                    activeTarget.getY() - bullet.getY(),
                    activeTarget.getX() - bullet.getX()
            ));
            float diff = (targetAngle - bullet.getAngle()) % 360f;
            if (diff > 180f) diff -= 360f;
            if (diff < -180f) diff += 360f;

            bullet.setAngle(bullet.getAngle() + Math.signum(diff) * Math.min(Math.abs(diff), turnSpeed * delta));
        } else if (!isHomingActive) {
            // Once homing window expires, accelerate forward up to max speed (Authentic EoSD: 600 px/s)
            float newSpeed = Math.min(600f, bullet.getSpeed() + 250f * delta);
            bullet.setSpeed(newSpeed);
        }

        // 3. Move along current trajectory angle at bullet speed
        float rad = (float) Math.toRadians(bullet.getAngle());
        float dx = (float) Math.cos(rad) * bullet.getSpeed() * delta;
        float dy = (float) Math.sin(rad) * bullet.getSpeed() * delta;
        bullet.setX(bullet.getX() + dx);
        bullet.setY(bullet.getY() + dy);
    }

    public GameObject getTarget() { return target; }
    public void setTarget(GameObject target) { this.target = target; }

    public List<GameObject> getEntities() { return entities; }
    public void setEntities(List<GameObject> entities) { this.entities = entities; }

    public float getTurnSpeed() { return turnSpeed; }
    public void setTurnSpeed(float turnSpeed) { this.turnSpeed = turnSpeed; }

    public float getMaxHomingDuration() { return maxHomingDuration; }
    public void setMaxHomingDuration(float maxHomingDuration) { this.maxHomingDuration = maxHomingDuration; }
}
