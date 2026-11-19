package com.Ruinfall.frontend.objects.Interactables;

import com.badlogic.gdx.graphics.Color;
import com.Ruinfall.frontend.PlayerController;
import com.Ruinfall.frontend.objects.Collidable;
import com.Ruinfall.frontend.objects.GameObject;
import com.Ruinfall.frontend.objects.patterns.FallBehavior;
import com.Ruinfall.frontend.objects.patterns.StandardFallBehavior;
import com.Ruinfall.frontend.systems.GameEventBus;

/**
 * Stalactite (Abstract): Base ceiling hazard that detaches and falls downward
 * when triggered, damaging entities below.
 */
public abstract class Stalactite extends GameObject {
    protected boolean triggered = false;
    protected boolean falling = false;
    protected float fallSpeed = 0f;
    protected float fallGravity = 300f;
    protected float shakeTimer = 0f;
    protected float shakeDuration = 0.4f;
    protected float initialX;
    protected float initialY;

    protected FallBehavior fallBehavior;

    public Stalactite(float x, float y, float width, float height, Color color) {
        super(x, y, width, height, 0f, color != null ? color : Color.GRAY);
        this.initialX = x;
        this.initialY = y;
        this.fallBehavior = new StandardFallBehavior();
    }

    public void trigger() {
        if (!triggered) {
            triggered = true;
            shakeTimer = shakeDuration;
            GameEventBus.getInstance().publish(GameEventBus.GameEvent.STALACTITE_TRIGGERED, this);
            System.out.println("[Stalactite] Triggered at (" + x + ", " + y + ")");
        }
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        if (!active) return;

        if (triggered && !falling) {
            shakeTimer -= delta;
            // Visual jitter while shaking
            x = initialX + (float) ((Math.random() - 0.5) * 4.0);
            if (shakeTimer <= 0f) {
                falling = true;
                x = initialX; // Reset to original x
            }
        } else if (falling) {
            if (fallBehavior != null) {
                fallBehavior.onFall(this, delta);
            } else {
                applyStandardGravity(delta);
            }

            // Ground impact check
            if (y <= 0f) {
                onGroundHit();
            }
        }
    }

    public void applyStandardGravity(float delta) {
        fallSpeed += fallGravity * delta;
        y -= fallSpeed * delta;
    }

    public void applyGravityMultiplied(float delta, float multiplier) {
        fallSpeed += fallGravity * multiplier * delta;
        y -= fallSpeed * delta;
    }

    public abstract void onEntityHit(GameObject entity);
    public abstract void onGroundHit();

    public boolean isPogoable() {
        return true;
    }

    @Override
    public void onCollision(Collidable other) {
        if (!active) return;

        if (other instanceof PlayerController player) {
            onEntityHit(player);
        } else if (other instanceof GameObject obj && !(other instanceof Stalactite)) {
            onEntityHit(obj);
        }
    }

    public void reset() {
        this.active = true;
        this.triggered = false;
        this.falling = false;
        this.fallSpeed = 0f;
        this.shakeTimer = 0f;
        this.x = initialX;
        this.y = initialY;
    }

    public void reinitialize(float x, float y) {
        this.initialX = x;
        this.initialY = y;
        this.x = x;
        this.y = y;
        reset();
    }

    public boolean isTriggered() { return triggered; }
    public boolean isFalling() { return falling; }
    public float getFallSpeed() { return fallSpeed; }
    public void setFallBehavior(FallBehavior fallBehavior) { this.fallBehavior = fallBehavior; }
    public FallBehavior getFallBehavior() { return fallBehavior; }
}
