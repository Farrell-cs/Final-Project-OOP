package com.Ruinfall.frontend.objects.patterns;

import com.Ruinfall.frontend.objects.Interactables.Stalactite;

/**
 * Strategy pattern — standard gravity fall behaviour.
 * Accelerates the stalactite downward using its own fallGravity value.
 */
public class StandardFallBehavior implements FallBehavior {

    @Override
    public void onFall(Stalactite stalactite, float delta) {
        // Delegate to the stalactite's own gravity-driven movement.
        // The stalactite's update() already handles fallSpeed accumulation;
        // this strategy simply confirms the default physics without override.
        stalactite.applyStandardGravity(delta);
    }
}
