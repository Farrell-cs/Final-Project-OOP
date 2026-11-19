package com.Ruinfall.frontend.systems;

import com.Ruinfall.frontend.objects.Interactables.CrystalStalactite;
import com.Ruinfall.frontend.objects.Interactables.RockStalactite;
import com.Ruinfall.frontend.objects.Interactables.Stalactite;

/**
 * Factory pattern — creates {@link Stalactite} variants.
 *
 * <p>All stalactite construction should be routed here to keep instantiation
 * logic in one place. Use in conjunction with {@link StalactitePool} when
 * object pooling is desired.
 */
public class StalactiteFactory {

    // Private constructor — static factory only
    private StalactiteFactory() {}

    // ------------------------------------------------------------------ //
    //  Typed factory methods
    // ------------------------------------------------------------------ //

    /**
     * Create a {@link CrystalStalactite} at the given position.
     */
    public static Stalactite createCrystal(float x, float y) {
        return new CrystalStalactite(x, y);
    }

    /**
     * Create a {@link RockStalactite} at the given position.
     */
    public static Stalactite createRock(float x, float y) {
        return new RockStalactite(x, y);
    }

    // ------------------------------------------------------------------ //
    //  Generic factory method
    // ------------------------------------------------------------------ //

    /**
     * Create a stalactite by string type name.
     *
     * @param type one of {@code "crystal"} or {@code "rock"} (case-insensitive)
     * @param x    X position
     * @param y    Y position
     * @return the new Stalactite instance
     * @throws IllegalArgumentException if the type is not recognised
     */
    public static Stalactite create(String type, float x, float y) {
        if (type == null) {
            throw new IllegalArgumentException("Stalactite type must not be null");
        }
        switch (type.toLowerCase()) {
            case "crystal":
                return createCrystal(x, y);
            case "rock":
                return createRock(x, y);
            default:
                throw new IllegalArgumentException("Unknown stalactite type: " + type);
        }
    }
}
