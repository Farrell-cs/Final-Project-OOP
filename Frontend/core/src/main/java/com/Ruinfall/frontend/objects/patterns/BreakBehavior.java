package com.Ruinfall.frontend.objects.patterns;

import com.Ruinfall.frontend.objects.Interactables.Rock;

/**
 * Strategy pattern — defines how a Rock object reacts to being hit and destroyed.
 * Different implementations can produce different visual effects, drop counts, etc.
 */
public interface BreakBehavior {

    /**
     * Called each time the Rock receives a hit.
     *
     * @param rock   the Rock being hit
     * @param damage amount of damage dealt this hit
     */
    void onHit(Rock rock, int damage);

    /**
     * Called when the Rock's HP reaches zero and it is about to be destroyed.
     *
     * @param rock the Rock being destroyed
     */
    void onDestroy(Rock rock);
}
