package com.Ruinfall.frontend.world;

import com.Ruinfall.frontend.objects.Interactables.Barrels;
import com.Ruinfall.frontend.objects.Interactables.GrapplePoint;
import com.Ruinfall.frontend.objects.Interactables.Rock;
import com.Ruinfall.frontend.objects.Interactables.Stalactite;
import com.Ruinfall.frontend.objects.items.ItemType;
import com.Ruinfall.frontend.systems.StalactiteFactory;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stage 1: Lush Mossy Caverns (Celeste Reflection-inspired alcoves, vertical drops,
 * shimmering reflective cave water pools -> Wave Mantis Arena -> Lava Shaft Drop).
 * Built purely procedurally with zero external assets.
 */
public class LushCaveStage {

    public static class HintZone {
        public final float x;
        public final float y;
        public final float triggerRadius;
        public final String key1;
        public final String label1;
        public final String key2;
        public final String label2;

        public HintZone(float x, float y, float triggerRadius, String key1, String label1) {
            this(x, y, triggerRadius, key1, label1, null, null);
        }

        public HintZone(float x, float y, float triggerRadius, String key1, String label1, String key2, String label2) {
            this.x = x;
            this.y = y;
            this.triggerRadius = triggerRadius;
            this.key1 = key1;
            this.label1 = label1;
            this.key2 = key2;
            this.label2 = label2;
        }
    }

    public static class SpikeHazard {
        public final Rectangle bounds;

        public SpikeHazard(float x, float y, float width, float height) {
            this.bounds = new Rectangle(x, y, width, height);
        }
    }

    public static class WaterPool {
        public final float x, y, width, height;

        public WaterPool(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    public static class Room {
        public final String name;
        public final float minX, maxX, minY, maxY;
        public final float spawnX, spawnY;
        public final boolean singleScreen;

        public Room(String name, float minX, float maxX, float minY, float maxY, float spawnX, float spawnY) {
            this(name, minX, maxX, minY, maxY, spawnX, spawnY, false);
        }

        public Room(String name, float minX, float maxX, float minY, float maxY, float spawnX, float spawnY, boolean singleScreen) {
            this.name = name;
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.spawnX = spawnX;
            this.spawnY = spawnY;
            this.singleScreen = singleScreen;
        }

        public boolean contains(float x, float y) {
            return x >= minX && x < maxX && y >= minY - 4.0f && y <= maxY + 4.0f;
        }
    }

    // Arena coordinates
    public static final float ARENA_MIN_X = 74.0f;
    public static final float ARENA_MAX_X = 104.0f;
    public static final float ARENA_TRIGGER_X = 76.5f;
    public static final float LAVA_SHAFT_X = 98.0f;
    public static final float LAVA_SHAFT_WIDTH = 6.0f;

    private final List<SolidBlock> blocks = new ArrayList<>();
    private final List<SpikeHazard> spikes = new ArrayList<>();
    private final List<WaterPool> waterPools = new ArrayList<>();
    private final List<GrapplePoint> grapplePoints = new ArrayList<>();
    private final List<Stalactite> stalactites = new ArrayList<>();
    private final List<Rock> breakables = new ArrayList<>();
    private final List<HintZone> hints = new ArrayList<>();
    private final List<Room> rooms = new ArrayList<>();

    private SolidBlock arenaLeftGate;
    private SolidBlock lavaShaftSeal;
    private boolean arenaLocked = false;
    private boolean lavaShaftOpen = false;
    private float waterTime = 0f;

    // Lush Mossy Caverns palette (deep forest teal stone + bright jade moss + warm amber hints)
    private final Color stoneDeep = new Color(0.08f, 0.17f, 0.19f, 1.0f);
    private final Color stoneMid = new Color(0.12f, 0.24f, 0.26f, 1.0f);
    private final Color stoneHighlight = new Color(0.18f, 0.34f, 0.35f, 1.0f);
    private final Color jadeMoss = new Color(0.22f, 0.66f, 0.42f, 1.0f);
    private final Color jadeMossBright = new Color(0.38f, 0.84f, 0.54f, 1.0f);
    private final Color gateColor = new Color(0.32f, 0.56f, 0.46f, 1.0f);
    private final Color lavaGlowColor = new Color(0.95f, 0.36f, 0.12f, 0.88f);
    private final Color spikeColor = new Color(0.86f, 0.28f, 0.28f, 1.0f);

    public LushCaveStage() {
        buildRooms();
        buildGeometry();
        buildInteractables();
        buildHints();
    }

    private void buildRooms() {
        rooms.add(new Room("Mossy Alcove", -14.0f, 16.0f, -3.0f, 14.0f, -10.5f, 5.2f, false));
        rooms.add(new Room("Sunken Moss Shaft", 16.0f, 38.0f, -2.0f, 12.0f, 17.5f, 1.2f, true));
        rooms.add(new Room("Reflection Lake", 38.0f, 58.0f, -2.5f, 11.5f, 38.6f, 7.6f, true));
        rooms.add(new Room("Stalactite Grotto", 58.0f, 74.0f, -1.8f, 12.2f, 59.5f, 2.5f, true));
        rooms.add(new Room("Wave Mantis Lair", 74.0f, 106.0f, -2.0f, 14.0f, 76.0f, 1.2f, false));
    }

    public Room findRoomAt(float x, float y) {
        for (Room r : rooms) {
            if (r.contains(x, y)) return r;
        }
        for (Room r : rooms) {
            if (x >= r.minX && x < r.maxX) return r;
        }
        return rooms.get(0);
    }

    public List<Room> getRooms() {
        return Collections.unmodifiableList(rooms);
    }

    private void buildGeometry() {
        // --- Outer Ceiling & Left Boundary (with sculpted cavern ceiling steps) ---
        blocks.add(new SolidBlock(-16.0f, 13.5f, 124.0f, 3.0f, stoneDeep));
        blocks.add(new SolidBlock(-16.0f, -4.0f, 2.0f, 20.0f, stoneDeep));
        // Cozy ceiling overhang above Room 1 entry alcove
        blocks.add(new SolidBlock(-14.0f, 10.5f, 7.0f, 3.0f, stoneDeep));

        // --- Room 1: Mossy Alcove -> Vertical Drop into Shimmering Pool Basin (-14 to 16) ---
        // Cozy elevated entry ledge (drops down into the cave like Reflection)
        blocks.add(new SolidBlock(-14.0f, 0.0f, 7.5f, 4.5f, stoneMid));
        // Lower cavern floor under the drop
        blocks.add(new SolidBlock(-14.0f, -3.5f, 20.0f, 3.5f, stoneMid));
        // Recessed shimmering reflective water basin (x = 6 to 14, bed top = -1.8, safe 0.54x jump-out)
        blocks.add(new SolidBlock(6.0f, -4.5f, 8.0f, 2.7f, stoneDeep));
        waterPools.add(new WaterPool(6.0f, -1.8f, 8.0f, 1.2f));
        // Cozy moss-capped stepping stone over the pool (top = 2.0, <= 0.60x real vertical reach)
        blocks.add(new SolidBlock(8.0f, 1.0f, 4.2f, 1.0f, stoneMid));
        // Exit ledge into Room 2
        blocks.add(new SolidBlock(14.0f, -3.5f, 10.0f, 3.5f, stoneMid));

        // --- Room 2: Sunken Moss Shaft (Vertical climb + cozy side alcoves, 16 to 38) ---
        // Ceiling dip creating a snug shaft entrance
        blocks.add(new SolidBlock(16.0f, 11.2f, 6.5f, 2.3f, stoneDeep));
        // Main climbable mossy stone wall
        blocks.add(new SolidBlock(24.0f, 0.0f, 2.6f, 7.2f, stoneMid));
        // Cozy breather alcove on left of shaft
        blocks.add(new SolidBlock(19.0f, 3.6f, 3.2f, 0.9f, stoneMid));
        // Sunken floor under upper ledge with shallow reflective pool + spikes
        blocks.add(new SolidBlock(24.0f, -3.5f, 14.0f, 2.5f, stoneMid));
        waterPools.add(new WaterPool(26.6f, -1.0f, 11.4f, 1.1f));
        spikes.add(new SpikeHazard(27.2f, -1.0f, 8.8f, 0.55f));
        // Upper moss landing overlooking the Reflection Lake
        blocks.add(new SolidBlock(29.2f, 6.2f, 6.5f, 1.1f, stoneMid));
        // Safe Room 3 entry perch inside [38.0, 58.0) (gap from 35.7 is 1.9 <= 0.25x reach)
        blocks.add(new SolidBlock(37.6f, 6.2f, 2.2f, 1.1f, stoneMid));

        // --- Room 3: Reflection Lake Chasm (Dash & Grapple over shimmering lake, 38 to 58) ---
        // Lake bed (positioned cleanly inside Room 3's locked 14-unit viewport)
        blocks.add(new SolidBlock(38.0f, -4.2f, 20.0f, 2.0f, stoneDeep));
        // Large shimmering reflective cave lake (Reflection-inspired!)
        waterPools.add(new WaterPool(38.0f, -2.2f, 20.0f, 2.1f));
        spikes.add(new SpikeHazard(38.5f, -2.2f, 19.0f, 0.55f));
        // Central mossy sanctuary island in the lake
        blocks.add(new SolidBlock(46.0f, 2.0f, 3.8f, 1.2f, stoneMid));
        // Cozy upper rock overhang
        blocks.add(new SolidBlock(44.5f, 11.5f, 7.0f, 2.0f, stoneDeep));

        // --- Room 4: Dripping Stalactite Grotto (Vertical step-down into Boss Lair, 58 to 74) ---
        // Elevated grotto entry ledge
        blocks.add(new SolidBlock(58.0f, -2.0f, 6.5f, 4.2f, stoneMid));
        // Lower grotto floor (step down!)
        blocks.add(new SolidBlock(64.5f, -2.5f, 9.5f, 2.5f, stoneMid));
        // Shallow grotto reflection puddle
        waterPools.add(new WaterPool(65.0f, -0.3f, 5.5f, 0.45f));
        // Sculpted grotto ceiling overhang keeping tutorial stalactite inside locked 14-unit viewport
        blocks.add(new SolidBlock(65.5f, 11.4f, 6.0f, 2.1f, stoneDeep));
        // Low cavern arch leading into Wave Mantis Lair
        blocks.add(new SolidBlock(71.5f, 8.2f, 2.5f, 5.3f, stoneDeep));

        // --- Room 5: Wave Mantis Lair (74 to 104) ---
        // Main arena floor
        blocks.add(new SolidBlock(74.0f, -2.0f, 24.0f, 2.0f, stoneMid));
        // Cozy elevated side-wall breather ledges in the arena
        blocks.add(new SolidBlock(74.5f, 4.6f, 2.6f, 0.85f, stoneMid));
        blocks.add(new SolidBlock(100.8f, 4.6f, 3.0f, 0.85f, stoneMid));
        // Right arena wall
        blocks.add(new SolidBlock(104.0f, -10.0f, 2.5f, 26.0f, stoneDeep));
        // Left shaft wall below arena floor
        blocks.add(new SolidBlock(95.5f, -10.0f, 2.5f, 8.0f, stoneDeep));

        // Sealed floor over the Lava Shaft (removed when Wave Mantis is defeated)
        lavaShaftSeal = new SolidBlock(LAVA_SHAFT_X, -2.0f, LAVA_SHAFT_WIDTH, 2.0f, gateColor);
        blocks.add(lavaShaftSeal);

        // Arena left gate (added when player enters arena, removed when boss is defeated)
        arenaLeftGate = new SolidBlock(72.5f, 0.0f, 1.5f, 8.2f, gateColor);
    }

    private void buildInteractables() {
        // Grapple points across the Reflection Lake chasm
        grapplePoints.add(new GrapplePoint(42.0f, 8.5f, 0.55f, 0.55f, 5.5f));
        grapplePoints.add(new GrapplePoint(53.8f, 7.2f, 0.55f, 0.55f, 7.5f));

        // Breakables in Room 4 grotto (elevated alcove + lower floor)
        breakables.add(new Rock(61.0f, 2.2f, ItemType.LIFE));
        breakables.add(new Barrels(66.5f, 0.0f));

        // Tutorial stalactite in Room 4 (anchored to sculpted ceiling overhang at y = 11.4)
        stalactites.add(StalactiteFactory.createRock(69.5f, 10.2f));

        // Boss arena stalactites (3 ceiling stalactites that can be dropped on dazed Wave Mantis)
        stalactites.add(StalactiteFactory.createRock(80.5f, 12.6f));
        stalactites.add(StalactiteFactory.createRock(88.5f, 12.6f));
        stalactites.add(StalactiteFactory.createRock(95.5f, 12.6f));
    }

    private void buildHints() {
        hints.add(new HintZone(-9.5f, 7.6f, 7.5f, "A / D", "Move", "SPACE", "Jump"));
        hints.add(new HintZone(21.0f, 5.4f, 7.5f, "HOLD Z", "Wall Climb", "W / S", "Up / Down"));
        hints.add(new HintZone(38.6f, 9.2f, 8.5f, "SHIFT", "Dash", "E", "Grapple"));
        hints.add(new HintZone(61.5f, 5.4f, 7.5f, "LMB / F", "Swing", "S + F", "Pogo"));
        hints.add(new HintZone(69.5f, 6.2f, 6.2f, "LMB / F", "Drop Stalactite"));
        hints.add(new HintZone(89.0f, 10.0f, 16.0f, "LMB / F", "Parry Dash", "STALACTITE", "Crush Dazed Boss"));
    }

    public void update(float dt) {
        waterTime += dt;
    }

    public void lockArena() {
        if (!arenaLocked) {
            arenaLocked = true;
            if (!blocks.contains(arenaLeftGate)) {
                blocks.add(arenaLeftGate);
            }
        }
    }

    public void unlockArenaAndOpenLavaShaft() {
        arenaLocked = false;
        blocks.remove(arenaLeftGate);
        if (!lavaShaftOpen) {
            lavaShaftOpen = true;
            blocks.remove(lavaShaftSeal);
            hints.add(new HintZone(LAVA_SHAFT_X + 3.0f, 2.5f, 12.0f, "DROP", "Enter Lava Shaft"));
        }
    }

    public void resetRoomInteractables(Room room) {
        if (room == null) return;
        for (Stalactite s : stalactites) {
            if (s.getInitialX() >= room.minX && s.getInitialX() < room.maxX) {
                s.reset();
            }
        }
        for (Rock r : breakables) {
            if (r.getInitialX() >= room.minX && r.getInitialX() < room.maxX) {
                r.reset();
            }
        }
    }

    public void resetArena() {
        arenaLocked = false;
        blocks.remove(arenaLeftGate);
        for (Stalactite s : stalactites) {
            if (s.getInitialX() >= ARENA_MIN_X) {
                s.reset();
            }
        }
    }

    public List<SolidBlock> getBlocks() {
        return Collections.unmodifiableList(blocks);
    }

    public List<SpikeHazard> getSpikes() {
        return Collections.unmodifiableList(spikes);
    }

    public List<WaterPool> getWaterPools() {
        return Collections.unmodifiableList(waterPools);
    }

    public List<GrapplePoint> getGrapplePoints() {
        return grapplePoints;
    }

    public List<Stalactite> getStalactites() {
        return stalactites;
    }

    public List<Rock> getBreakables() {
        return breakables;
    }

    public List<HintZone> getHints() {
        return hints;
    }

    public boolean isArenaLocked() {
        return arenaLocked;
    }

    public boolean isLavaShaftOpen() {
        return lavaShaftOpen;
    }

    public void render(SpriteBatch batch, Texture blockTexture) {
        // Render warm lava glow at the bottom of the shaft
        batch.setColor(lavaGlowColor);
        batch.draw(blockTexture, LAVA_SHAFT_X, -10.0f, LAVA_SHAFT_WIDTH, 2.8f);
        batch.setColor(1.0f, 0.72f, 0.25f, 0.35f);
        batch.draw(blockTexture, LAVA_SHAFT_X + 0.4f, -7.2f, LAVA_SHAFT_WIDTH - 0.8f, 2.5f);

        // Render shimmering reflective cave water pools (behind terrain edges)
        renderWaterPools(batch, blockTexture);

        // Render solid geometry with procedural mossy cavern stone trim
        for (SolidBlock block : blocks) {
            Rectangle b = block.getBounds();
            block.render(batch, blockTexture);

            // Subtle carved stone bevel & bright jade moss cap on walkable tops (below ceiling)
            if (block != arenaLeftGate && block != lavaShaftSeal && b.y + b.height < 12.5f && b.width >= 2.0f) {
                // Inner lighter stone rim
                batch.setColor(stoneHighlight);
                batch.draw(blockTexture, b.x + 0.08f, b.y + b.height - 0.45f, b.width - 0.16f, 0.22f);

                // Lush jade moss top layer
                batch.setColor(jadeMoss);
                batch.draw(blockTexture, b.x, b.y + b.height - 0.24f, b.width, 0.24f);

                // Bright jade moss highlight & small organic overhanging moss tufts
                batch.setColor(jadeMossBright);
                batch.draw(blockTexture, b.x + 0.05f, b.y + b.height - 0.09f, b.width - 0.1f, 0.09f);

                int tufts = Math.max(2, (int) (b.width * 1.1f));
                float step = b.width / tufts;
                for (int i = 0; i < tufts; i++) {
                    float tx = b.x + i * step + 0.12f;
                    float drop = 0.16f + Math.abs(MathUtils.sin(tx * 2.3f)) * 0.26f;
                    batch.setColor(jadeMoss);
                    batch.draw(blockTexture, tx, b.y + b.height - 0.24f - drop, 0.22f, drop);
                }
            }
        }

        // Render spikes as procedural jagged crimson bars
        batch.setColor(spikeColor);
        for (SpikeHazard spike : spikes) {
            Rectangle r = spike.bounds;
            int teeth = Math.max(2, (int) (r.width * 2.0f));
            float toothW = r.width / teeth;
            for (int i = 0; i < teeth; i++) {
                float tx = r.x + i * toothW;
                batch.draw(blockTexture, tx + toothW * 0.15f, r.y, toothW * 0.7f, r.height * 0.65f);
                batch.draw(blockTexture, tx + toothW * 0.30f, r.y + r.height * 0.65f, toothW * 0.4f, r.height * 0.35f);
            }
        }
        batch.setColor(Color.WHITE);
    }

    private void renderWaterPools(SpriteBatch batch, Texture blockTexture) {
        for (WaterPool pool : waterPools) {
            // Deep translucent forest-jade water body
            batch.setColor(0.06f, 0.24f, 0.28f, 0.78f);
            batch.draw(blockTexture, pool.x, pool.y, pool.width, pool.height);

            // Soft teal-emerald reflective surface band
            float surfaceY = pool.y + pool.height - 0.14f;
            batch.setColor(0.32f, 0.82f, 0.74f, 0.62f);
            batch.draw(blockTexture, pool.x, surfaceY, pool.width, 0.14f);

            // Animated horizontal Reflection-style shimmer ripples
            int rippleRows = Math.max(2, (int) (pool.height * 2.2f));
            for (int r = 0; r < rippleRows; r++) {
                float ry = surfaceY - 0.28f - r * 0.36f;
                if (ry < pool.y + 0.1f) break;
                float phase = waterTime * (1.4f + r * 0.35f) + pool.x;
                int segments = Math.max(2, (int) (pool.width / 2.6f));
                for (int s = 0; s < segments; s++) {
                    float segW = 0.75f + 0.45f * MathUtils.sin(phase + s * 1.9f);
                    float rx = pool.x + ((s + 0.5f) / segments) * (pool.width - segW)
                             + MathUtils.sin(phase * 0.8f + s) * 0.35f;
                    rx = MathUtils.clamp(rx, pool.x + 0.1f, pool.x + pool.width - segW - 0.1f);
                    float alpha = (0.36f - r * 0.06f) * (0.65f + 0.35f * MathUtils.cos(phase + s * 2.1f));
                    // Warm golden-jade reflection glint
                    batch.setColor(0.75f, 0.96f, 0.78f, Math.max(0.06f, alpha));
                    batch.draw(blockTexture, rx, ry, segW, 0.07f);
                }
            }
        }
    }
}
