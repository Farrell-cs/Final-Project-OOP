package com.Ruinfall.frontend.objects.Interactables;

import com.Ruinfall.frontend.PlayerController;
import com.Ruinfall.frontend.core.ISolid;
import com.Ruinfall.frontend.objects.Collidable;
import com.Ruinfall.frontend.objects.GameObject;
import com.Ruinfall.frontend.objects.enemies.WormBoss;
import com.Ruinfall.frontend.objects.enemies.WormSegment;
import com.Ruinfall.frontend.systems.GameEventBus;
import com.Ruinfall.frontend.visuals.EffectManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

import java.util.List;

/**
 * Ceiling-mounted stalactite hazard with a 4-state lifecycle:
 * INTACT -> SHAKING -> FALLING -> REGENERATING -> INTACT.
 */
public class RegeneratingStalactite extends Stalactite {
    public enum State {
        INTACT,
        SHAKING,
        FALLING,
        REGENERATING
    }

    private State state = State.INTACT;
    private final float laneCenterX;
    private float xJitterRange = 0.85f;
    private float minTriggerDelay = 0.30f;
    private float maxTriggerDelay = 0.55f;
    private float minRegenCooldown = 3.5f;
    private float maxRegenCooldown = 7.0f;
    private float maxInitialArmingDelay = 0.0f;
    private float armingDelayTimer = 0.0f;
    private float regenCooldown = 4.0f;
    private float regenTimer = 0f;
    private float renderScale = 1.0f;
    private float triggerHalfWidth = 1.6f;
    private int playerDamage = 1;
    private int bossDamage = 25;
    private List<? extends ISolid> solids;

    public RegeneratingStalactite(float x, float y) {
        super(x, y, 0.8f, 1.5f, new Color(0.55f, 0.72f, 0.85f, 1f));
        this.laneCenterX = x;
        this.fallGravity = 32.0f;
        randomizeSpawnSlot();
        this.shakeDuration = sampleTriggerDelay();
        this.regenCooldown = sampleRegenCooldown();
    }

    /**
     * Configures per-lane horizontal jitter, randomized pre-drop trigger delay,
     * asynchronous respawn cooldown window, and initial arming stagger.
     */
    public void configureRandomization(
        float xJitterRange,
        float minTriggerDelay,
        float maxTriggerDelay,
        float minRegenCooldown,
        float maxRegenCooldown,
        float maxInitialArmingDelay
    ) {
        this.xJitterRange = Math.max(0f, xJitterRange);
        this.minTriggerDelay = Math.max(0.05f, minTriggerDelay);
        this.maxTriggerDelay = Math.max(this.minTriggerDelay, maxTriggerDelay);
        this.minRegenCooldown = Math.max(0.1f, minRegenCooldown);
        this.maxRegenCooldown = Math.max(this.minRegenCooldown, maxRegenCooldown);
        this.maxInitialArmingDelay = Math.max(0f, maxInitialArmingDelay);

        randomizeSpawnSlot();
        this.shakeDuration = sampleTriggerDelay();
        this.regenCooldown = sampleRegenCooldown();
        this.armingDelayTimer = this.maxInitialArmingDelay > 0f
            ? MathUtils.random(0f, this.maxInitialArmingDelay)
            : 0f;
    }

    private void randomizeSpawnSlot() {
        float jitter = xJitterRange > 0f ? MathUtils.random(-xJitterRange, xJitterRange) : 0f;
        this.initialX = laneCenterX + jitter;
        this.x = this.initialX;
    }

    private float sampleTriggerDelay() {
        return MathUtils.random(minTriggerDelay, maxTriggerDelay);
    }

    private float sampleRegenCooldown() {
        return MathUtils.random(minRegenCooldown, maxRegenCooldown);
    }

    public void setSolids(List<? extends ISolid> solids) {
        this.solids = solids;
    }

    public boolean checkProximityTrigger(float entityCenterX, float entityY) {
        if (state != State.INTACT || !active || armingDelayTimer > 0f) {
            return false;
        }
        float centerX = initialX + width * 0.5f;
        if (Math.abs(entityCenterX - centerX) <= triggerHalfWidth && entityY < initialY) {
            trigger();
            return true;
        }
        return false;
    }

    @Override
    public void trigger() {
        if (state == State.INTACT) {
            state = State.SHAKING;
            triggered = true;
            falling = false;
            shakeDuration = sampleTriggerDelay();
            shakeTimer = shakeDuration;
            GameEventBus.getInstance().publish(GameEventBus.GameEvent.STALACTITE_TRIGGERED, this);
        }
    }

    @Override
    public void update(float delta) {
        if (!active) {
            return;
        }
        stateTime += delta;

        switch (state) {
            case INTACT:
                if (armingDelayTimer > 0f) {
                    armingDelayTimer = Math.max(0f, armingDelayTimer - delta);
                }
                break;

            case SHAKING:
                shakeTimer -= delta;
                x = initialX + (float) ((Math.random() - 0.5) * 0.16);
                EffectManager.getInstance().onStalactiteShaking(initialX + width * 0.5f, initialY);
                if (shakeTimer <= 0f) {
                    x = initialX;
                    falling = true;
                    fallSpeed = 0f;
                    state = State.FALLING;
                }
                break;

            case FALLING:
                if (fallBehavior != null) {
                    fallBehavior.onFall(this, delta);
                } else {
                    applyStandardGravity(delta);
                }
                if (y <= 0f || hitsPlatformTop()) {
                    onGroundHit();
                }
                break;

            case REGENERATING:
                regenTimer += delta;
                renderScale = MathUtils.clamp(regenTimer / regenCooldown, 0f, 1f);
                EffectManager.getInstance().onStalactiteRegenerating(
                    initialX + width * 0.5f,
                    initialY + height * (1f - renderScale * 0.5f)
                );
                if (regenTimer >= regenCooldown) {
                    renderScale = 1.0f;
                    regenTimer = 0f;
                    state = State.INTACT;
                }
                break;
        }
    }

    private boolean hitsPlatformTop() {
        if (solids == null) {
            return false;
        }
        Rectangle box = getCoreHitbox();
        for (ISolid solid : solids) {
            if (!solid.isOneWay()) continue;
            Rectangle s = solid.getBounds();
            if (box.overlaps(s) && y <= s.y + s.height) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onCollision(Collidable other) {
        if (!active || state != State.FALLING) {
            return;
        }
        if (other instanceof PlayerController player) {
            onEntityHit(player);
        } else if (other instanceof WormBoss || other instanceof WormSegment || other instanceof Barrels) {
            onEntityHit((GameObject) other);
        }
    }

    @Override
    public void onEntityHit(GameObject entity) {
        if (!active || state != State.FALLING) {
            return;
        }
        if (entity instanceof PlayerController player) {
            player.takeDamage(playerDamage);
        } else if (entity instanceof WormBoss boss) {
            boss.takeDamage(bossDamage);
        } else if (entity instanceof WormSegment seg) {
            seg.takeDamage(bossDamage);
        } else if (entity instanceof Barrels barrel) {
            barrel.onHeavyImpact();
        }
        shatter();
    }

    @Override
    public void onGroundHit() {
        if (!active || state != State.FALLING) {
            return;
        }
        shatter();
    }

    private void shatter() {
        state = State.REGENERATING;
        triggered = false;
        falling = false;
        fallSpeed = 0f;
        regenTimer = 0f;
        renderScale = 0f;
        regenCooldown = sampleRegenCooldown();
        randomizeSpawnSlot();
        y = initialY;
        GameEventBus.getInstance().publish(GameEventBus.GameEvent.ROCK_SHATTERED, this);
    }

    @Override
    public void reset() {
        randomizeSpawnSlot();
        super.reset();
        this.state = State.INTACT;
        this.regenTimer = 0f;
        this.renderScale = 1.0f;
        this.shakeDuration = sampleTriggerDelay();
        this.regenCooldown = sampleRegenCooldown();
        this.armingDelayTimer = this.maxInitialArmingDelay > 0f
            ? MathUtils.random(0f, this.maxInitialArmingDelay)
            : 0f;
    }

    public State getState() {
        return state;
    }

    public float getRenderScale() {
        return renderScale;
    }

    public float getRegenCooldown() {
        return regenCooldown;
    }

    public void setRegenCooldown(float regenCooldown) {
        float clamped = Math.max(0.1f, regenCooldown);
        this.minRegenCooldown = clamped;
        this.maxRegenCooldown = clamped;
        this.regenCooldown = clamped;
    }

    public float getLaneCenterX() {
        return laneCenterX;
    }

    public float getArmingDelayTimer() {
        return armingDelayTimer;
    }
}
