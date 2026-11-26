package com.Ruinfall.frontend.systems;

import com.Ruinfall.frontend.objects.Interactables.CrystalStalactite;
import com.Ruinfall.frontend.objects.Interactables.RockStalactite;
import com.Ruinfall.frontend.objects.Interactables.Stalactite;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Object Pool pattern — manages pools of reusable {@link Stalactite} instances.
 *
 * <p>Also a Singleton — only one pool exists for the lifetime of the game session.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * StalactitePool pool = StalactitePool.getInstance();
 * pool.prewarm(5, 5);                             // fill pool at level load
 *
 * Stalactite s = pool.obtain("crystal", 200, 400); // get (or create) an instance
 * s.trigger();                                    // use it
 *
 * // when the stalactite is done (destroyed / recycled):
 * pool.release(s);
 * }</pre>
 */
public class StalactitePool {

    // ------------------------------------------------------------------ //
    //  Singleton
    // ------------------------------------------------------------------ //

    private static StalactitePool instance;

    private StalactitePool() {}

    public static StalactitePool getInstance() {
        if (instance == null) {
            instance = new StalactitePool();
        }
        return instance;
    }

    // ------------------------------------------------------------------ //
    //  Pool storage
    // ------------------------------------------------------------------ //

    private final Queue<Stalactite> crystalPool = new LinkedList<>();
    private final Queue<Stalactite> rockPool    = new LinkedList<>();

    private static final int MAX_POOL_SIZE = 20;

    // ------------------------------------------------------------------ //
    //  Obtain
    // ------------------------------------------------------------------ //

    /**
     * Obtain a stalactite of the given type, resetting it to the supplied position.
     * If the pool is empty, a fresh instance is created via {@link StalactiteFactory}.
     *
     * @param type one of {@code "crystal"} or {@code "rock"} (case-insensitive)
     * @param x    spawn X
     * @param y    spawn Y
     * @return a ready-to-use Stalactite
     */
    public Stalactite obtain(String type, float x, float y) {
        Queue<Stalactite> pool = poolFor(type);
        Stalactite s;

        if (!pool.isEmpty()) {
            s = pool.poll();
            s.reinitialize(x, y);         // reset + reposition
        } else {
            s = StalactiteFactory.create(type, x, y);
        }

        return s;
    }

    // ------------------------------------------------------------------ //
    //  Release
    // ------------------------------------------------------------------ //

    /**
     * Return a stalactite to its pool for future reuse.
     * The instance is reset via {@link Stalactite#reset()} before being stored.
     *
     * @param s the stalactite to recycle
     */
    public void release(Stalactite s) {
        if (s == null) return;

        s.reset();  // clean all mutable state

        Queue<Stalactite> pool = poolFor(s);
        if (pool.size() < MAX_POOL_SIZE) {
            pool.offer(s);
        }
        // If pool is full we simply let the GC collect the excess instance
    }

    // ------------------------------------------------------------------ //
    //  Pre-warm
    // ------------------------------------------------------------------ //

    /**
     * Pre-populate the pools ahead of time (e.g. at level load) to avoid
     * runtime allocation spikes.
     *
     * @param crystalCount number of CrystalStalactite instances to pre-create
     * @param rockCount    number of RockStalactite instances to pre-create
     */
    public void prewarm(int crystalCount, int rockCount) {
        for (int i = 0; i < crystalCount && crystalPool.size() < MAX_POOL_SIZE; i++) {
            Stalactite s = StalactiteFactory.createCrystal(0, 0);
            s.reset();
            crystalPool.offer(s);
        }
        for (int i = 0; i < rockCount && rockPool.size() < MAX_POOL_SIZE; i++) {
            Stalactite s = StalactiteFactory.createRock(0, 0);
            s.reset();
            rockPool.offer(s);
        }
        System.out.println("[StalactitePool] Pre-warmed: "
                + crystalPool.size() + " crystal, "
                + rockPool.size()    + " rock.");
    }

    // ------------------------------------------------------------------ //
    //  Diagnostic
    // ------------------------------------------------------------------ //

    public int crystalPoolSize() { return crystalPool.size(); }
    public int rockPoolSize()    { return rockPool.size();    }

    // ------------------------------------------------------------------ //
    //  Helpers
    // ------------------------------------------------------------------ //

    private Queue<Stalactite> poolFor(String type) {
        if (type != null && type.equalsIgnoreCase("rock")) return rockPool;
        return crystalPool; // default / crystal
    }

    private Queue<Stalactite> poolFor(Stalactite s) {
        return (s instanceof RockStalactite) ? rockPool : crystalPool;
    }
}
