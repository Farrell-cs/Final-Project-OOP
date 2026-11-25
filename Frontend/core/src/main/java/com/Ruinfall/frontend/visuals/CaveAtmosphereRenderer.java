package com.Ruinfall.frontend.visuals;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

/**
 * Procedural Lush Mossy Caverns / Reflection-inspired background renderer (zero external assets):
 * - Deep forest-teal parallax cavern rock silhouettes & pillars
 * - Warm golden-amber diagonal light shafts filtering through ceiling cracks
 * - Glowing jade-amber cave crystals with soft pulsing halos
 * - Gently swaying mossy hanging vines from the cavern ceiling
 */
public class CaveAtmosphereRenderer {

    private float time = 0f;
    private final ShapeRenderer shapeRenderer = new ShapeRenderer();

    // Lush Mossy Caverns palette (deep forest teal stone + bright jade moss + warm golden-amber rays)
    private final Color farRockColor = new Color(0.04f, 0.10f, 0.12f, 1.0f);
    private final Color midRockColor = new Color(0.07f, 0.15f, 0.17f, 1.0f);
    private final Color vineColor = new Color(0.18f, 0.48f, 0.30f, 0.88f);
    private final Color vineTipColor = new Color(0.36f, 0.78f, 0.48f, 0.90f);
    private final Color crystalCore = new Color(0.42f, 0.94f, 0.72f, 0.92f);
    private final Color crystalGlow = new Color(0.88f, 0.72f, 0.28f, 0.14f);

    public void update(float dt) {
        time += dt;
    }

    public void render(SpriteBatch batch, Texture blockTexture, OrthographicCamera camera) {
        float camX = camera.position.x;
        float camY = camera.position.y;
        float viewW = camera.viewportWidth;
        float viewH = camera.viewportHeight;
        float left = camX - viewW * 0.65f;
        float right = camX + viewW * 0.65f;

        // --- Layer 1: Far parallax cavern pillars (0.25x scroll) ---
        batch.setColor(farRockColor);
        float farShift = camX * 0.75f;
        int startFar = (int) Math.floor((left - farShift) / 9.0f) - 1;
        int endFar = (int) Math.ceil((right - farShift) / 9.0f) + 1;
        for (int i = startFar; i <= endFar; i++) {
            float px = farShift + i * 9.0f;
            float w = 3.2f + Math.abs(MathUtils.sin(i * 1.7f)) * 2.5f;
            float h = 18.0f;
            batch.draw(blockTexture, px, camY - viewH * 0.65f, w, h);
        }

        // --- Layer 2: Mid parallax mossy stalagmites & ceiling arches (0.5x scroll) ---
        batch.setColor(midRockColor);
        float midShift = camX * 0.50f;
        int startMid = (int) Math.floor((left - midShift) / 6.5f) - 1;
        int endMid = (int) Math.ceil((right - midShift) / 6.5f) + 1;
        for (int i = startMid; i <= endMid; i++) {
            float px = midShift + i * 6.5f;
            float w = 1.9f + Math.abs(MathUtils.cos(i * 2.3f)) * 1.6f;
            float baseH = 3.5f + Math.abs(MathUtils.sin(i * 3.1f)) * 5.0f;
            batch.draw(blockTexture, px, -4.0f, w, baseH);
            float topH = 2.0f + Math.abs(MathUtils.cos(i * 1.9f)) * 3.6f;
            batch.draw(blockTexture, px + 1.8f, 14.0f - topH, w * 0.75f, topH);
        }

        // --- Layer 3: World-space glowing jade-amber crystal clusters ---
        int startCrys = (int) Math.floor(left / 12.0f) - 1;
        int endCrys = (int) Math.ceil(right / 12.0f) + 1;
        for (int i = startCrys; i <= endCrys; i++) {
            float cx = i * 12.0f + MathUtils.sin(i * 4.3f) * 2.2f;
            float cy = 0.0f + Math.abs(MathUtils.cos(i * 2.7f)) * 2.0f;
            float pulse = 0.85f + 0.15f * MathUtils.sin(time * 2.2f + i * 1.3f);

            // Warm golden-amber outer glow halo
            batch.setColor(crystalGlow.r, crystalGlow.g, crystalGlow.b, crystalGlow.a * pulse);
            batch.draw(blockTexture, cx - 1.2f, cy - 0.5f, 2.8f, 3.2f);

            // Jade crystal shards
            batch.setColor(crystalCore.r, crystalCore.g, crystalCore.b, crystalCore.a * pulse);
            batch.draw(blockTexture, cx, cy, 0.34f, 1.4f);
            batch.draw(blockTexture, cx + 0.40f, cy, 0.24f, 0.92f);
            batch.draw(blockTexture, cx - 0.28f, cy, 0.22f, 0.72f);
        }

        // --- Layer 4: Swaying mossy hanging vines from ceiling ---
        int startVine = (int) Math.floor(left / 5.0f) - 1;
        int endVine = (int) Math.ceil(right / 5.0f) + 1;
        for (int i = startVine; i <= endVine; i++) {
            float vx = i * 5.0f + MathUtils.cos(i * 3.7f) * 1.3f;
            float length = 2.0f + Math.abs(MathUtils.sin(i * 2.1f)) * 3.4f;
            int segments = 5;
            float segH = length / segments;
            for (int s = 0; s < segments; s++) {
                float sway = MathUtils.sin(time * 1.6f + i + s * 0.45f) * (0.08f * s);
                float segY = 13.6f - (s + 1) * segH;
                batch.setColor(s == segments - 1 ? vineTipColor : vineColor);
                batch.draw(blockTexture, vx + sway, segY, 0.12f, segH + 0.03f);
            }
        }

        batch.setColor(Color.WHITE);
    }

    /**
     * Renders soft warm golden-amber diagonal light shafts filtering down from ceiling cracks.
     * Call outside SpriteBatch.begin()/end().
     */
    public void renderLightShafts(OrthographicCamera camera) {
        float camX = camera.position.x;
        float viewW = camera.viewportWidth;
        float left = camX - viewW * 0.7f;
        float right = camX + viewW * 0.7f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        int startRay = (int) Math.floor(left / 14.0f) - 1;
        int endRay = (int) Math.ceil(right / 14.0f) + 1;

        for (int i = startRay; i <= endRay; i++) {
            float topX = i * 14.0f + MathUtils.sin(i * 2.9f) * 3.0f;
            float topW = 1.6f + Math.abs(MathUtils.cos(i * 1.7f)) * 1.4f;
            float slant = 4.5f;
            float topY = 14.0f;
            float botY = -2.5f;
            float pulse = 0.055f + 0.025f * MathUtils.sin(time * 0.9f + i * 1.4f);

            // Warm golden-amber light shaft quad (2 triangles)
            Color topCol = new Color(1.0f, 0.86f, 0.45f, pulse * 1.4f);
            Color botCol = new Color(0.95f, 0.78f, 0.35f, 0.0f);

            float x1 = topX;
            float x2 = topX + topW;
            float x3 = topX + slant + topW * 1.75f;
            float x4 = topX + slant;

            shapeRenderer.triangle(x1, topY, x2, topY, x3, botY, topCol, topCol, botCol);
            shapeRenderer.triangle(x1, topY, x3, botY, x4, botY, topCol, botCol, botCol);
        }

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    public void dispose() {
        shapeRenderer.dispose();
    }
}
