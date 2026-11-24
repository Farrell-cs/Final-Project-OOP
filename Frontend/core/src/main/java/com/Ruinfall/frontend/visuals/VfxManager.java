package com.Ruinfall.frontend.visuals;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Procedural special effects manager for Stage 1 (zero external assets):
 * - Hit-stop (freeze frames on parry, stalactite crush, and boss defeat)
 * - Parry flash (world-space starburst + expanding ring + screen flash)
 * - Expanding shockwave/impact rings
 */
public class VfxManager {

    private static class ExpandingRing {
        float x, y;
        float radius;
        float maxRadius;
        float timer;
        float duration;
        Color color;
        boolean starburst;

        ExpandingRing(float x, float y, Color color, float maxRadius, float duration, boolean starburst) {
            this.x = x;
            this.y = y;
            this.radius = 0.2f;
            this.maxRadius = maxRadius;
            this.timer = duration;
            this.duration = duration;
            this.color = new Color(color);
            this.starburst = starburst;
        }
    }

    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    private final Matrix4 screenProjection = new Matrix4();
    private final List<ExpandingRing> rings = new ArrayList<>();

    private float hitStopTimer = 0f;

    private final Color flashColor = new Color(1f, 1f, 1f, 0f);
    private float flashTimer = 0f;
    private float flashDuration = 0.15f;
    private float flashStartAlpha = 0f;

    public void triggerHitStop(float seconds) {
        hitStopTimer = Math.max(hitStopTimer, seconds);
    }

    /**
     * @return true if the simulation should remain frozen this frame.
     */
    public boolean updateHitStop(float dt) {
        if (hitStopTimer > 0f) {
            hitStopTimer -= dt;
            return true;
        }
        return false;
    }

    public void triggerParryFlash(float worldX, float worldY) {
        triggerHitStop(0.11f);
        triggerScreenFlash(new Color(1.0f, 0.96f, 0.78f, 1.0f), 0.48f, 0.18f);
        rings.add(new ExpandingRing(worldX, worldY, new Color(1.0f, 0.95f, 0.55f, 1.0f), 3.8f, 0.28f, true));
        rings.add(new ExpandingRing(worldX, worldY, new Color(1.0f, 1.0f, 1.0f, 1.0f), 2.2f, 0.18f, false));
    }

    public void triggerImpactRing(float worldX, float worldY, Color color, float maxRadius, float duration) {
        rings.add(new ExpandingRing(worldX, worldY, color, maxRadius, duration, false));
    }

    public void triggerScreenFlash(Color color, float alpha, float duration) {
        flashColor.set(color);
        flashStartAlpha = alpha;
        flashDuration = Math.max(0.01f, duration);
        flashTimer = flashDuration;
    }

    public void update(float dt) {
        if (flashTimer > 0f) {
            flashTimer = Math.max(0f, flashTimer - dt);
        }

        Iterator<ExpandingRing> it = rings.iterator();
        while (it.hasNext()) {
            ExpandingRing ring = it.next();
            ring.timer -= dt;
            if (ring.timer <= 0f) {
                it.remove();
            } else {
                float progress = 1.0f - (ring.timer / ring.duration);
                // Ease-out expansion
                float eased = 1.0f - (1.0f - progress) * (1.0f - progress);
                ring.radius = MathUtils.lerp(0.25f, ring.maxRadius, eased);
            }
        }
    }

    public void renderWorld(OrthographicCamera worldCamera) {
        if (rings.isEmpty()) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(worldCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (ExpandingRing ring : rings) {
            float alpha = MathUtils.clamp(ring.timer / ring.duration, 0f, 1f);
            shapeRenderer.setColor(ring.color.r, ring.color.g, ring.color.b, alpha);
            shapeRenderer.circle(ring.x, ring.y, ring.radius, 32);

            if (ring.starburst) {
                float ray = ring.radius * 1.15f;
                shapeRenderer.line(ring.x - ray, ring.y, ring.x + ray, ring.y);
                shapeRenderer.line(ring.x, ring.y - ray, ring.x, ring.y + ray);
                float diag = ray * 0.65f;
                shapeRenderer.line(ring.x - diag, ring.y - diag, ring.x + diag, ring.y + diag);
                shapeRenderer.line(ring.x - diag, ring.y + diag, ring.x + diag, ring.y - diag);
            }
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    public void renderScreenOverlay() {
        if (flashTimer <= 0f) return;

        float alpha = flashStartAlpha * MathUtils.clamp(flashTimer / flashDuration, 0f, 1f);
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        screenProjection.setToOrtho2D(0, 0, w, h);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(screenProjection);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(flashColor.r, flashColor.g, flashColor.b, alpha);
        shapeRenderer.rect(0, 0, w, h);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    public void dispose() {
        shapeRenderer.dispose();
    }
}
