package com.Ruinfall.frontend.objects.patterns.bulletStrategy;

import com.gylchrist.frontend.objects.GameObject;
import com.gylchrist.frontend.objects.bullets.Bullet;
import com.gylchrist.frontend.objects.enemies.Enemy;

import java.util.List;

public class FantasySealMovement implements BulletMovementPattern {
    private GameObject target;
    private List<GameObject> entities;
    private float homingSpeed;
    private float turnSpeed;
    private float driftDuration; // Phase 1 drift time before homing (default 0.4 seconds)
    private float timer = 0f;

    public FantasySealMovement(GameObject target, List<GameObject> entities, float homingSpeed, float turnSpeed, float driftDuration) {
        this.target = target;
        this.entities = entities;
        this.homingSpeed = homingSpeed;
        this.turnSpeed = turnSpeed;
        this.driftDuration = driftDuration;
    }

    public FantasySealMovement(GameObject target, List<GameObject> entities) {
        this(target, entities, 400f, 300f, 0.4f);
    }

    public FantasySealMovement(GameObject target) {
        this(target, null, 400f, 300f, 0.4f);
    }

    @Override
    public void move(Bullet bullet, float delta) {
        if (bullet == null) return;
        timer += delta;

        if (timer < driftDuration) {
            // Phase 1 (TH06 Radial Drift): Bullet moves along its initial radial velocity vector (vx, vy)
            bullet.setX(bullet.getX() + bullet.getVx() * delta);
            bullet.setY(bullet.getY() + bullet.getVy() * delta);
        } else {
            // Dynamic Retargeting: If target is null or destroyed, find nearest active enemy from entities list!
            if (target == null || target.isDestroyed()) {
                if (entities != null) {
                    GameObject nearest = null;
                    float minDistanceSq = Float.MAX_VALUE;
                    for (GameObject obj : entities) {
                        if (obj instanceof Enemy enemy && !enemy.isDestroyed()) {
                            float dx = enemy.getX() - bullet.getX();
                            float dy = enemy.getY() - bullet.getY();
                            float distSq = dx * dx + dy * dy;
                            if (distSq < minDistanceSq) {
                                minDistanceSq = distSq;
                                nearest = enemy;
                            }
                        }
                    }
                    target = nearest;
                }
            }

            // Phase 2 (TH06 Homing Lock): Bullet steers towards target and accelerates
            if (target != null && !target.isDestroyed()) {
                float targetAngle = (float) Math.toDegrees(Math.atan2(
                    target.getY() - bullet.getY(),
                    target.getX() - bullet.getX()
                ));
                float diff = (targetAngle - bullet.getAngle()) % 360f;
                if (diff > 180f) diff -= 360f;
                if (diff < -180f) diff += 360f;

                bullet.setAngle(bullet.getAngle() + Math.signum(diff) * Math.min(Math.abs(diff), turnSpeed * delta));
            }

            float rad = (float) Math.toRadians(bullet.getAngle());
            float dx = (float) Math.cos(rad) * homingSpeed * delta;
            float dy = (float) Math.sin(rad) * homingSpeed * delta;
            bullet.setX(bullet.getX() + dx);
            bullet.setY(bullet.getY() + dy);
        }
    }

    public GameObject getTarget() { return target; }
    public void setTarget(GameObject target) { this.target = target; }

    public List<GameObject> getEntities() { return entities; }
    public void setEntities(List<GameObject> entities) { this.entities = entities; }

    public float getHomingSpeed() { return homingSpeed; }
    public void setHomingSpeed(float homingSpeed) { this.homingSpeed = homingSpeed; }

    public float getTurnSpeed() { return turnSpeed; }
    public void setTurnSpeed(float turnSpeed) { this.turnSpeed = turnSpeed; }

    public float getDriftDuration() { return driftDuration; }
    public void setDriftDuration(float driftDuration) { this.driftDuration = driftDuration; }
}
