package com.Ruinfall.frontend.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BoxRoom {
    private final List<SolidBlock> blocks = new ArrayList<>();

    public BoxRoom() {
        Color floorColor = new Color(0.28f, 0.36f, 0.50f, 1.0f);
        Color wallColor = new Color(0.22f, 0.27f, 0.38f, 1.0f);

        // Floor: inner surface at y = 0
        blocks.add(new SolidBlock(-16.0f, -2.0f, 32.0f, 2.0f, floorColor));
        // Ceiling: inner surface at y = 14
        blocks.add(new SolidBlock(-16.0f, 14.0f, 32.0f, 2.0f, wallColor));
        // Left Wall: inner surface at x = -14
        blocks.add(new SolidBlock(-16.0f, 0.0f, 2.0f, 14.0f, wallColor));
        // Right Wall: inner surface at x = 14
        blocks.add(new SolidBlock(14.0f, 0.0f, 2.0f, 14.0f, wallColor));

        // Center test pillar to practice climbing, neutral jumps, and ledge hops
        blocks.add(new SolidBlock(-1.0f, 0.0f, 2.0f, 5.0f, wallColor));
    }

    public List<SolidBlock> getBlocks() {
        return Collections.unmodifiableList(blocks);
    }

    public void render(SpriteBatch batch, Texture blockTexture) {
        for (SolidBlock block : blocks) {
            block.render(batch, blockTexture);
        }
    }
}
