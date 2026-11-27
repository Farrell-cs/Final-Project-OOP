package com.Ruinfall.frontend.world;

import com.Ruinfall.frontend.objects.Interactables.GrapplePoint;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Multi-tiered boss arena with flat main floor, outer bounds, and 9 staggered
 * elevated one-way floating platforms across lower, mid, and upper elevations.
 */
public class BoxRoom {
    public static final float ARENA_MIN_X = -22.0f;
    public static final float ARENA_MIN_Y = 0.0f;
    public static final float ARENA_WIDTH = 44.0f;
    public static final float ARENA_HEIGHT = 14.0f;
    public static final float WALL_THICKNESS = 2.0f;

    private final List<SolidBlock> blocks = new ArrayList<>();
    private final List<OneWayPlatform> platforms = new ArrayList<>();

    public BoxRoom() {
        Color floorColor = new Color(0.28f, 0.36f, 0.50f, 1.0f);
        Color wallColor = new Color(0.22f, 0.27f, 0.38f, 1.0f);
        Color platformColor = new Color(0.42f, 0.54f, 0.72f, 1.0f);

        float outerMinX = ARENA_MIN_X - WALL_THICKNESS;
        float outerWidth = ARENA_WIDTH + WALL_THICKNESS * 2.0f;
        float arenaMaxX = ARENA_MIN_X + ARENA_WIDTH;
        float arenaMaxY = ARENA_MIN_Y + ARENA_HEIGHT;

        // Floor: inner surface at y = 0
        blocks.add(new SolidBlock(outerMinX, ARENA_MIN_Y - WALL_THICKNESS, outerWidth, WALL_THICKNESS, floorColor));
        // Ceiling: inner surface at y = 14
        blocks.add(new SolidBlock(outerMinX, arenaMaxY, outerWidth, WALL_THICKNESS, wallColor));
        // Left Wall: inner surface at x = -22
        blocks.add(new SolidBlock(outerMinX, ARENA_MIN_Y, WALL_THICKNESS, ARENA_HEIGHT, wallColor));
        // Right Wall: inner surface at x = 22
        blocks.add(new SolidBlock(arenaMaxX, ARENA_MIN_Y, WALL_THICKNESS, ARENA_HEIGHT, wallColor));

        // Lower tier (y = 2.4, top = 2.8) — 4 symmetrical platforms across the wider arena
        addPlatform(-18.0f, 2.4f, 5.5f, 0.4f, platformColor);
        addPlatform(-8.5f, 2.4f, 5.0f, 0.4f, platformColor);
        addPlatform(3.5f, 2.4f, 5.0f, 0.4f, platformColor);
        addPlatform(12.5f, 2.4f, 5.5f, 0.4f, platformColor);

        // Mid tier (y = 5.2, top = 5.6) — 3 staggered platforms framing combat lanes
        addPlatform(-14.0f, 5.2f, 5.0f, 0.4f, platformColor);
        addPlatform(-3.0f, 5.2f, 6.0f, 0.4f, platformColor);
        addPlatform(9.0f, 5.2f, 5.0f, 0.4f, platformColor);

        // Upper tier (y = 8.0, top = 8.4) — 5.6 units below ceiling (y = 14.0) for clean dashes/grapples
        addPlatform(-18.5f, 8.0f, 4.5f, 0.4f, platformColor);
        addPlatform(14.0f, 8.0f, 4.5f, 0.4f, platformColor);
    }

    private void addPlatform(float x, float y, float width, float height, Color color) {
        OneWayPlatform platform = new OneWayPlatform(x, y, width, height, color);
        platforms.add(platform);
        blocks.add(platform);
    }

    public List<SolidBlock> getBlocks() {
        return Collections.unmodifiableList(blocks);
    }

    public List<OneWayPlatform> getPlatforms() {
        return Collections.unmodifiableList(platforms);
    }

    public Rectangle getArenaBounds() {
        return new Rectangle(ARENA_MIN_X, ARENA_MIN_Y, ARENA_WIDTH, ARENA_HEIGHT);
    }

    public Rectangle getOuterBounds() {
        return new Rectangle(
            ARENA_MIN_X - WALL_THICKNESS,
            ARENA_MIN_Y - WALL_THICKNESS,
            ARENA_WIDTH + WALL_THICKNESS * 2.0f,
            ARENA_HEIGHT + WALL_THICKNESS * 2.0f
        );
    }

    /**
     * Builds a well-spaced network of grapple anchors across ceiling nodes, mid-air lanes,
     * and platform undersides so the player can chain aerial swings across the wider arena
     * without touching the slime-covered floor.
     */
    public List<GrapplePoint> createGrappleNetwork() {
        List<GrapplePoint> anchors = new ArrayList<>();
        float nodeSize = 0.55f;
        float half = nodeSize * 0.5f;

        // 1. Ceiling lane nodes (y = 11.2, 8.0u apart, radius 5.8u -> overlapping aerial swing chain)
        float[] ceilingXs = {-16.0f, -8.0f, 0.0f, 8.0f, 16.0f};
        for (float cx : ceilingXs) {
            anchors.add(new GrapplePoint(cx - half, 11.2f - half, nodeSize, nodeSize, 5.8f));
        }

        // 2. Mid-air lane nodes (y = 6.8, radius 5.4u -> bridges lower/mid/ceiling traversal)
        float[] midXs = {-12.0f, -5.5f, 5.5f, 12.0f};
        for (float mx : midXs) {
            anchors.add(new GrapplePoint(mx - half, 6.8f - half, nodeSize, nodeSize, 5.4f));
        }

        // 3. Platform underside anchors (hanging beneath center mid & upper side platforms)
        anchors.add(new GrapplePoint(0.0f - half, 4.45f - half, nodeSize, nodeSize, 5.0f));
        anchors.add(new GrapplePoint(-16.25f - half, 7.25f - half, nodeSize, nodeSize, 5.0f));
        anchors.add(new GrapplePoint(16.25f - half, 7.25f - half, nodeSize, nodeSize, 5.0f));

        return anchors;
    }

    public void render(SpriteBatch batch, Texture blockTexture) {
        for (SolidBlock block : blocks) {
            block.render(batch, blockTexture);
        }
    }
}
