package com.Ruinfall.frontend.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Multi-tiered boss arena with flat main floor, outer bounds, and 5 staggered
 * elevated one-way floating platforms across lower, mid, and upper elevations.
 */
public class BoxRoom {
    public static final float ARENA_MIN_X = -14.0f;
    public static final float ARENA_MIN_Y = 0.0f;
    public static final float ARENA_WIDTH = 28.0f;
    public static final float ARENA_HEIGHT = 14.0f;

    private final List<SolidBlock> blocks = new ArrayList<>();
    private final List<OneWayPlatform> platforms = new ArrayList<>();

    public BoxRoom() {
        Color floorColor = new Color(0.28f, 0.36f, 0.50f, 1.0f);
        Color wallColor = new Color(0.22f, 0.27f, 0.38f, 1.0f);
        Color platformColor = new Color(0.42f, 0.54f, 0.72f, 1.0f);

        // Floor: inner surface at y = 0
        blocks.add(new SolidBlock(-16.0f, -2.0f, 32.0f, 2.0f, floorColor));
        // Ceiling: inner surface at y = 14
        blocks.add(new SolidBlock(-16.0f, 14.0f, 32.0f, 2.0f, wallColor));
        // Left Wall: inner surface at x = -14
        blocks.add(new SolidBlock(-16.0f, 0.0f, 2.0f, 14.0f, wallColor));
        // Right Wall: inner surface at x = 14
        blocks.add(new SolidBlock(14.0f, 0.0f, 2.0f, 14.0f, wallColor));

        // Lower tier (y = 2.4, top = 2.8)
        addPlatform(-10.5f, 2.4f, 5.0f, 0.4f, platformColor);
        addPlatform(5.5f, 2.4f, 5.0f, 0.4f, platformColor);

        // Mid tier (y = 5.2, top = 5.6)
        addPlatform(-3.0f, 5.2f, 6.0f, 0.4f, platformColor);

        // Upper tier (y = 8.0, top = 8.4) — 5.6 units below ceiling (y = 14.0) for clean dashes/grapples
        addPlatform(-11.0f, 8.0f, 4.5f, 0.4f, platformColor);
        addPlatform(6.5f, 8.0f, 4.5f, 0.4f, platformColor);
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

    public void render(SpriteBatch batch, Texture blockTexture) {
        for (SolidBlock block : blocks) {
            block.render(batch, blockTexture);
        }
    }
}
