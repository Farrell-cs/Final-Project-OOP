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
import com.badlogic.gdx.math.Rectangle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stage 1: Lush Cave (Tutorial -> Wave Mantis Arena -> Lava Shaft Drop).
 * Built purely from procedural SolidBlocks, spike hazards, interactables, and world hints.
 */
public class LushCaveStage {

    public static class HintZone {
        public final float x;
        public final float y;
        public final float triggerRadius;
        public final String text;

        public HintZone(float x, float y, float triggerRadius, String text) {
            this.x = x;
            this.y = y;
            this.triggerRadius = triggerRadius;
            this.text = text;
        }
    }

    public static class SpikeHazard {
        public final Rectangle bounds;

        public SpikeHazard(float x, float y, float width, float height) {
            this.bounds = new Rectangle(x, y, width, height);
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
    private final List<GrapplePoint> grapplePoints = new ArrayList<>();
    private final List<Stalactite> stalactites = new ArrayList<>();
    private final List<Rock> breakables = new ArrayList<>();
    private final List<HintZone> hints = new ArrayList<>();

    private SolidBlock arenaLeftGate;
    private SolidBlock lavaShaftSeal;
    private boolean arenaLocked = false;
    private boolean lavaShaftOpen = false;

    // Lush cave palette (Hollow Knight inspired mossy stone & bioluminescent accents)
    private final Color floorColor = new Color(0.18f, 0.29f, 0.25f, 1.0f);
    private final Color wallColor = new Color(0.13f, 0.20f, 0.22f, 1.0f);
    private final Color mossAccent = new Color(0.24f, 0.45f, 0.34f, 1.0f);
    private final Color gateColor = new Color(0.35f, 0.55f, 0.48f, 1.0f);
    private final Color lavaGlowColor = new Color(0.95f, 0.32f, 0.10f, 0.85f);
    private final Color spikeColor = new Color(0.85f, 0.25f, 0.28f, 1.0f);

    public LushCaveStage() {
        buildGeometry();
        buildInteractables();
        buildHints();
    }

    private void buildGeometry() {
        // --- Outer Ceiling & Left Boundary ---
        blocks.add(new SolidBlock(-16.0f, 14.0f, 124.0f, 2.5f, wallColor));
        blocks.add(new SolidBlock(-16.0f, -2.0f, 2.0f, 18.0f, wallColor));

        // --- Section 1: Awakening & Basic Jump (x = -14 to 14) ---
        blocks.add(new SolidBlock(-14.0f, -2.0f, 20.0f, 2.0f, floorColor));
        // Small stepping platform
        blocks.add(new SolidBlock(8.5f, 1.5f, 4.0f, 1.0f, mossAccent));
        // Floor continues after small pit
        blocks.add(new SolidBlock(14.0f, -2.0f, 10.0f, 2.0f, floorColor));
        // Pit floor with spikes (x = 6 to 14)
        blocks.add(new SolidBlock(6.0f, -4.0f, 8.0f, 2.0f, wallColor));
        spikes.add(new SpikeHazard(6.5f, -2.0f, 7.0f, 0.6f));

        // --- Section 2: Wall Climb & Spikes (x = 24 to 38) ---
        // Tall wall to climb up & over
        blocks.add(new SolidBlock(24.0f, 0.0f, 2.5f, 7.0f, wallColor));
        blocks.add(new SolidBlock(24.0f, -2.0f, 14.0f, 2.0f, floorColor));
        // Upper landing after climb
        blocks.add(new SolidBlock(29.0f, 6.0f, 6.0f, 1.2f, mossAccent));
        // Spikes along the lower floor under the ledge
        spikes.add(new SpikeHazard(27.0f, 0.0f, 9.0f, 0.6f));

        // --- Section 3: Dash & Grapple Chasm (x = 38 to 58) ---
        // Chasm floor with spikes
        blocks.add(new SolidBlock(38.0f, -4.0f, 20.0f, 2.0f, wallColor));
        spikes.add(new SpikeHazard(38.5f, -2.0f, 19.0f, 0.6f));
        // Mid-chasm rest pillar
        blocks.add(new SolidBlock(46.0f, 2.0f, 3.5f, 1.2f, mossAccent));

        // --- Section 4: Stalactite & Breakables Hallway (x = 58 to 74) ---
        blocks.add(new SolidBlock(58.0f, -2.0f, 16.0f, 2.0f, floorColor));
        // Low ceiling overhang before boss arena
        blocks.add(new SolidBlock(71.5f, 8.5f, 2.5f, 5.5f, wallColor));

        // --- Section 5: Wave Mantis Boss Arena (x = 74 to 104) ---
        // Main arena floor up to the Lava Shaft drop
        blocks.add(new SolidBlock(74.0f, -2.0f, 24.0f, 2.0f, floorColor));
        // Right arena wall
        blocks.add(new SolidBlock(104.0f, -10.0f, 2.5f, 26.0f, wallColor));
        // Left shaft wall below arena floor
        blocks.add(new SolidBlock(95.5f, -10.0f, 2.5f, 8.0f, wallColor));

        // Sealed floor over the Lava Shaft (removed when Wave Mantis is defeated)
        lavaShaftSeal = new SolidBlock(LAVA_SHAFT_X, -2.0f, LAVA_SHAFT_WIDTH, 2.0f, gateColor);
        blocks.add(lavaShaftSeal);

        // Arena left gate (added when player enters arena, removed when boss is defeated)
        arenaLeftGate = new SolidBlock(72.5f, 0.0f, 1.5f, 8.5f, gateColor);
    }

    private void buildInteractables() {
        // Grapple points across the Section 3 chasm
        grapplePoints.add(new GrapplePoint(41.5f, 7.5f, 0.55f, 0.55f, 5.5f));
        grapplePoints.add(new GrapplePoint(53.0f, 7.5f, 0.55f, 0.55f, 5.5f));

        // Breakables in Section 4 (teaches swinging & drops)
        breakables.add(new Rock(61.5f, 0.0f, ItemType.LIFE));
        breakables.add(new Barrels(66.5f, 0.0f));

        // Tutorial stalactite in Section 4
        stalactites.add(StalactiteFactory.createRock(69.5f, 12.8f));

        // Boss arena stalactites (3 ceiling stalactites that can be dropped on dazed Wave Mantis)
        stalactites.add(StalactiteFactory.createRock(80.0f, 12.8f));
        stalactites.add(StalactiteFactory.createRock(88.0f, 12.8f));
        stalactites.add(StalactiteFactory.createRock(95.0f, 12.8f));
    }

    private void buildHints() {
        hints.add(new HintZone(-6.0f, 3.8f, 8.0f, "MOVE: A / D   |   JUMP: SPACE"));
        hints.add(new HintZone(21.5f, 4.5f, 7.5f, "WALL CLIMB: HOLD SHIFT + W / S"));
        hints.add(new HintZone(36.0f, 5.5f, 8.0f, "DASH: X / J   |   GRAPPLE: E / K"));
        hints.add(new HintZone(62.5f, 3.8f, 7.5f, "SWING: C / L  (DOWN + SWING TO POGO)"));
        hints.add(new HintZone(69.5f, 6.2f, 6.0f, "DISTURB OR HIT STALACTITES TO DROP THEM"));
        hints.add(new HintZone(89.0f, 9.5f, 16.0f, "WAVE MANTIS: PARRY DASH WITH SWING OR LURE TO WALL -> DROP STALACTITE"));
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
            hints.add(new HintZone(LAVA_SHAFT_X + 3.0f, 2.5f, 12.0f, "LAVA SHAFT UNSEALED - DROP DOWN TO CONTINUE"));
        }
    }

    public List<SolidBlock> getBlocks() {
        return Collections.unmodifiableList(blocks);
    }

    public List<SpikeHazard> getSpikes() {
        return Collections.unmodifiableList(spikes);
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
        // Render lava glow at the bottom of the shaft if open (or peeking through)
        batch.setColor(lavaGlowColor);
        batch.draw(blockTexture, LAVA_SHAFT_X, -10.0f, LAVA_SHAFT_WIDTH, 2.5f);

        // Render solid geometry
        for (SolidBlock block : blocks) {
            block.render(batch, blockTexture);
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
}
