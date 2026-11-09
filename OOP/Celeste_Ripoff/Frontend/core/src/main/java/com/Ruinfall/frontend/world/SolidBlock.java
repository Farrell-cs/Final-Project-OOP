package com.Ruinfall.frontend.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.Ruinfall.frontend.core.ISolid;

public class SolidBlock implements ISolid {
    private final Rectangle bounds;
    private final Vector2 liftSpeed = new Vector2();
    private final Color color;

    public SolidBlock(float x, float y, float width, float height, Color color) {
        this.bounds = new Rectangle(x, y, width, height);
        this.color = new Color(color);
    }

    @Override
    public Rectangle getBounds() {
        return bounds;
    }

    @Override
    public Vector2 getLiftSpeed() {
        return liftSpeed;
    }

    public void render(SpriteBatch batch, Texture texture) {
        Color prev = batch.getColor();
        batch.setColor(color);
        batch.draw(texture, bounds.x, bounds.y, bounds.width, bounds.height);
        batch.setColor(prev);
    }
}
