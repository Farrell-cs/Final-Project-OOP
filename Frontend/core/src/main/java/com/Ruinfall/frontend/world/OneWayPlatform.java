package com.Ruinfall.frontend.world;

import com.badlogic.gdx.graphics.Color;

/**
 * Elevated platform supporting one-way pass-through from below and drop-through.
 */
public class OneWayPlatform extends SolidBlock {
    public OneWayPlatform(float x, float y, float width, float height, Color color) {
        super(x, y, width, height, color);
    }

    @Override
    public boolean isOneWay() {
        return true;
    }
}
