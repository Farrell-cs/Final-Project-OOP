package com.Ruinfall.frontend.subsystems;

public class StaminaSystem {
    private float maxStamina = 110.0f;
    private float currentStamina = 110.0f;
    private float tiredThreshold = 20.0f;

    private float climbUpCost = 45.4545f;
    private float climbStillCost = 10.0f;
    private float climbJumpCost = 27.5f;

    public void configure(float max, float upCost, float stillCost, float jumpCost, float tiredLimit) {
        maxStamina = Math.max(1.0f, max);
        climbUpCost = upCost;
        climbStillCost = stillCost;
        climbJumpCost = jumpCost;
        tiredThreshold = tiredLimit;
        currentStamina = maxStamina;
    }

    public void refill() {
        currentStamina = maxStamina;
    }

    public void drainClimbUp(float dt) {
        drain(climbUpCost * dt);
    }

    public void drainHold(float dt) {
        drain(climbStillCost * dt);
    }

    public void drainClimbJump() {
        drain(climbJumpCost);
    }

    public void drain(float amount) {
        if (amount <= 0.0f || currentStamina <= 0.0f) {
            return;
        }
        currentStamina = Math.max(0.0f, currentStamina - amount);
    }

    public boolean isExhausted() {
        return currentStamina <= 0.0f;
    }

    public boolean isLowStamina() {
        return currentStamina <= tiredThreshold;
    }

    public float getCurrentStamina() {
        return currentStamina;
    }

    public float getMaxStamina() {
        return maxStamina;
    }

    public float getNormalized() {
        return maxStamina > 0.0f ? currentStamina / maxStamina : 0.0f;
    }
}
