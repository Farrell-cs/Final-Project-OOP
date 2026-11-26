package com.Ruinfall.frontend.visuals;

/**
 * Brief freeze-frame / time-scale pause on heavy impacts for tactile game feel.
 */
public class HitstopManager {
    private float remainingTime;
    private float freezeTimeScale = 0.05f;

    public void trigger(float durationSeconds) {
        if (durationSeconds > remainingTime) {
            remainingTime = durationSeconds;
        }
    }

    public float computeEffectiveDt(float rawDt) {
        if (remainingTime <= 0f) {
            return rawDt;
        }
        remainingTime = Math.max(0f, remainingTime - rawDt);
        return rawDt * freezeTimeScale;
    }

    public boolean isActive() {
        return remainingTime > 0f;
    }

    public float getRemainingTime() {
        return remainingTime;
    }

    public void setFreezeTimeScale(float freezeTimeScale) {
        this.freezeTimeScale = Math.max(0f, Math.min(1f, freezeTimeScale));
    }

    public void reset() {
        remainingTime = 0f;
    }
}
