package com.Ruinfall.frontend.subsystems;

public class DashManager {
    private int maxDashes = 1;
    private int currentDashes = 1;

    private float cooldownTimer;
    private float refillCooldownTimer;
    private float dashAttackTimer;
    private float freezeTimer;

    private float dashCooldown = 0.20f;
    private float refillCooldown = 0.10f;
    private float dashAttackDuration = 0.30f;
    private float freezeDuration = 0.05f;

    public void configure(int max, float cooldown, float refillWait, float attackWindow, float freezeTime) {
        maxDashes = Math.max(1, Math.min(2, max));
        dashCooldown = cooldown;
        refillCooldown = refillWait;
        dashAttackDuration = attackWindow;
        freezeDuration = freezeTime;
        currentDashes = maxDashes;
    }

    public void setMaxDashes(int max) {
        maxDashes = Math.max(1, Math.min(2, max));
        currentDashes = maxDashes;
    }

    public void tick(float dt) {
        if (freezeTimer > 0.0f) {
            freezeTimer = Math.max(0.0f, freezeTimer - dt);
            return;
        }

        if (cooldownTimer > 0.0f) {
            cooldownTimer = Math.max(0.0f, cooldownTimer - dt);
        }

        if (refillCooldownTimer > 0.0f) {
            refillCooldownTimer = Math.max(0.0f, refillCooldownTimer - dt);
        }

        if (dashAttackTimer > 0.0f) {
            dashAttackTimer = Math.max(0.0f, dashAttackTimer - dt);
        }
    }

    public boolean canDash() {
        return currentDashes > 0 && cooldownTimer <= 0.0f;
    }

    public boolean tryConsumeDash() {
        if (!canDash()) {
            return false;
        }
        currentDashes--;
        cooldownTimer = dashCooldown;
        refillCooldownTimer = refillCooldown;
        dashAttackTimer = dashAttackDuration;
        freezeTimer = freezeDuration;
        return true;
    }

    public boolean tryRefillOnGround() {
        if (refillCooldownTimer > 0.0f || currentDashes >= maxDashes) {
            return false;
        }
        currentDashes = maxDashes;
        return true;
    }

    public void forceRefill() {
        currentDashes = maxDashes;
    }

    public void cancelDashAttack() {
        dashAttackTimer = 0.0f;
    }

    public boolean isFrozen() {
        return freezeTimer > 0.0f;
    }

    public boolean inDashAttack() {
        return dashAttackTimer > 0.0f;
    }

    public int getCurrentDashes() {
        return currentDashes;
    }

    public int getMaxDashes() {
        return maxDashes;
    }
}
