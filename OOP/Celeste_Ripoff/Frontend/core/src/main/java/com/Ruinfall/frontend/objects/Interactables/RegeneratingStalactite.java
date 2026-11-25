package com.Ruinfall.frontend.objects.Interactables;

import com.Ruinfall.frontend.PlayerController;
import com.Ruinfall.frontend.core.ISolid;
import com.Ruinfall.frontend.objects.Collidable;
import com.Ruinfall.frontend.objects.GameObject;
import com.Ruinfall.frontend.objects.enemies.WormBoss;
import com.Ruinfall.frontend.objects.enemies.WormSegment;
import com.Ruinfall.frontend.systems.GameEventBus;
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
    private float regenCooldown = 4.0f;
    private float regenTimer = 0f;
    private float renderScale = 1.0f;
    private float triggerHalfWidth = 1.6f;
    private int playerDamage = 1;
    private int bossDamage = 25;
    private List<? extends ISolid> solids;

    public RegeneratingStalactite(float x, float y) {
        super(x, y, 0.8f, 1.5f, new Color(0.55f, 0.72f, 0.85f, 1f));
        this.shakeDuration = 0.45f;
        this.fallGravity = 32.0f;
    }

    public void setSolids(List<? extends ISolid> solids) {
        this.solids = solids;
    }

    public boolean checkProximityTrigger(float entityCenterX, float entityY) {
        if (state != State.INTACT || !active) {
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
                break;

            case SHAKING:
                shakeTimer -= delta;
                x = initialX + (float) ((Math.random() - 0.5) * 0.16);
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
        } else if (other instanceof WormBoss || other instanceof WormSegment) {
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
        x = initialX;
        y = initialY;
        GameEventBus.getInstance().publish(GameEventBus.GameEvent.ROCK_SHATTERED, this);
    }

    @Override
    public void reset() {
        super.reset();
        this.state = State.INTACT;
        this.regenTimer = 0f;
        this.renderScale = 1.0f;
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
        this.regenCooldown = Math.max(0.1f, regenCooldown);
    }
}
