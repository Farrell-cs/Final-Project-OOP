package com.Ruinfall.frontend.objects.patterns;

import com.Ruinfall.frontend.objects.Interactables.Stalactite;

/**
 * Strategy pattern — heavy fall behaviour (2× gravity).
 * Used by RockStalactite to plummet faster than a standard stalactite.
 */
public class HeavyFallBehavior implements FallBehavior {

    private static final float GRAVITY_MULTIPLIER = 2.0f;

    @Override
    public void onFall(Stalactite stalactite, float delta) {
        // Apply double-strength gravity through the stalactite's public helper.
        stalactite.applyGravityMultiplied(delta, GRAVITY_MULTIPLIER);
    }
}
