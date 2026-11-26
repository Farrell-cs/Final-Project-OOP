package com.Ruinfall.frontend.visuals;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class GhostTrailEmitter {
    private static class Ghost {
        final Rectangle bounds = new Rectangle();
        final Color color = new Color();
        float remainingLife;
        float totalLife;

        void init(Rectangle bounds, Color color, float life) {
            this.bounds.set(bounds);
            this.color.set(color);
            this.remainingLife = life;
            this.totalLife = life;
        }
    }

    private final List<Ghost> ghosts = new ArrayList<>();
    private final Queue<Ghost> ghostPool = new ArrayDeque<>();
    private float spawnTimer;
    private final float spawnInterval = 0.025f;
    private final float ghostLifetime = 0.22f;

    public void update(boolean shouldEmit, Rectangle playerBounds, Color color, float dt) {
        for (int i = ghosts.size() - 1; i >= 0; i--) {
            Ghost g = ghosts.get(i);
            g.remainingLife -= dt;
            if (g.remainingLife <= 0.0f) {
                ghosts.remove(i);
                ghostPool.offer(g);
            }
        }

        if (!shouldEmit) {
            spawnTimer = 0.0f;
            return;
        }

        spawnTimer -= dt;
        if (spawnTimer <= 0.0f) {
            spawnTimer = spawnInterval;
            Ghost g = ghostPool.isEmpty() ? new Ghost() : ghostPool.poll();
            g.init(playerBounds, color, ghostLifetime);
            ghosts.add(g);
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
