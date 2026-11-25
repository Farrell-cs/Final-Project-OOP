package com.Ruinfall.frontend.objects.hazards;

import com.Ruinfall.frontend.PlayerController;
import com.Ruinfall.frontend.objects.Collidable;
import com.Ruinfall.frontend.objects.GameObject;
import com.badlogic.gdx.graphics.Color;

/**
 * Persistent slime hazard zone dropped by WormBoss during Stage 2.
 * Overlapping the zone applies a movement speed debuff and sets canDash = false
 * on the player until exit.
 */
public class SlimeHazard extends GameObject {
    private final float speedMultiplier;
    private final float maxLifetime;
    private float age;

    public SlimeHazard(float x, float y, float width, float height) {
        this(x, y, width, height, 0.45f, -1f);
    }

    public SlimeHazard(float x, float y, float width, float height, float speedMultiplier, float maxLifetime) {
        super(x, y, width, height, 0f, new Color(0.35f, 0.90f, 0.25f, 0.75f));
        this.speedMultiplier = Math.max(0.1f, Math.min(1.0f, speedMultiplier));
        this.maxLifetime = maxLifetime;
    }

    @Override
    public void update(float delta) {
        super.update(delta);
        if (!active) {
            return;
        }
        age += delta;
        if (maxLifetime > 0f && age >= maxLifetime) {
            destroy();
        }
    }

    public boolean overlaps(Collidable other) {
        if (!active || other == null) {
            return false;
        }
        return getCoreHitbox().overlaps(other.getCoreHitbox());
    }

    @Override
    public void onCollision(Collidable other) {
        if (other instanceof PlayerController player) {
            applyTo(player);
        }
    }

    public void applyTo(PlayerController player) {
        if (player == null) {
            return;
        }
        player.setMovementSpeedMultiplier(speedMultiplier);
        player.setCanDash(false);
    }

    public static void clearFrom(PlayerController player) {
        if (player == null) {
            return;
        }
        player.setMovementSpeedMultiplier(1.0f);
        player.setCanDash(true);
    }

    public float getSpeedMultiplier() {
        return speedMultiplier;
    }
}
