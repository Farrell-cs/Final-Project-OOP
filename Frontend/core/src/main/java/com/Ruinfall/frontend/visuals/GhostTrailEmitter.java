package com.Ruinfall.frontend.visuals;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GhostTrailEmitter {
    private static class Ghost {
        Rectangle bounds;
        Color color;
        float remainingLife;
        float totalLife;

        Ghost(Rectangle bounds, Color color, float life) {
            this.bounds = new Rectangle(bounds);
            this.color = new Color(color);
            this.remainingLife = life;
            this.totalLife = life;
        }
    }

    private final List<Ghost> ghosts = new ArrayList<>();
    private float spawnTimer;
    private final float spawnInterval = 0.035f;
    private final float ghostLifetime = 0.22f;

    public void update(boolean shouldEmit, Rectangle playerBounds, Color color, float dt) {
        Iterator<Ghost> it = ghosts.iterator();
        while (it.hasNext()) {
            Ghost g = it.next();
            g.remainingLife -= dt;
            if (g.remainingLife <= 0.0f) {
                it.remove();
            }
        }

        if (!shouldEmit) {
            spawnTimer = 0.0f;
            return;
        }

        spawnTimer -= dt;
        if (spawnTimer <= 0.0f) {
            spawnTimer = spawnInterval;
            ghosts.add(new Ghost(playerBounds, color, ghostLifetime));
        }
    }

    public void render(SpriteBatch batch, Texture texture) {
        Color prev = batch.getColor();
        for (Ghost g : ghosts) {
            float alpha = (g.remainingLife / g.totalLife) * g.color.a;
            batch.setColor(g.color.r, g.color.g, g.color.b, alpha);
            batch.draw(texture, g.bounds.x, g.bounds.y, g.bounds.width, g.bounds.height);
        }
        batch.setColor(prev);
    }

    public void clear() {
        ghosts.clear();
    }
}
