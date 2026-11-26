package com.Ruinfall.frontend.visuals;

import com.Ruinfall.frontend.world.LushCaveStage;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Procedural Lush Mossy Caverns / Reflection-inspired background renderer (zero external assets):
 * - Deep forest-teal parallax cavern rock silhouettes & pillars
 * - Warm golden-amber diagonal light shafts filtering through ceiling cracks
 * - Glowing jade-amber cave crystals with soft pulsing halos
 * - Gently swaying mossy hanging vines from the cavern ceiling
 * - Camera-culled Celeste-style ambient moss spores, light-shaft motes, water drips/splashes & lava embers
 */
public class CaveAtmosphereRenderer {

    private static final class CeilingDroplet {
        float x, y, vy, targetY;
    }

    private float time = 0f;
    private float sporeAccum = 0f;
    private float shaftMoteAccum = 0f;
    private float mistAccum = 0f;
    private float dripAccum = 0f;
    private float crystalAccum = 0f;
    private float emberAccum = 0f;

    private final List<CeilingDroplet> droplets = new ArrayList<>();
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

    public void updateParticles(float dt, OrthographicCamera camera, LushCaveStage stage, ParticleSystem particles) {
        if (dt <= 0f || camera == null || stage == null || particles == null) return;

        float camX = camera.position.x;
        float camY = camera.position.y;
        float halfW = camera.viewportWidth * 0.5f;
        float halfH = camera.viewportHeight * 0.5f;
        float left = camX - halfW - 1.5f;
        float right = camX + halfW + 1.5f;
        float bottom = camY - halfH - 1.5f;
        float top = camY + halfH + 1.5f;

        // 1. Bioluminescent moss spores (Rooms 1-4 and cool cavern edges)
        if (left < 96.0f) {
            sporeAccum += dt * 14.0f;
            while (sporeAccum >= 1.0f) {
                sporeAccum -= 1.0f;
                float sx = MathUtils.random(left, Math.min(right, 96.0f));
                float sy = MathUtils.random(Math.max(-2.0f, bottom), Math.min(12.8f, top));
                boolean teal = MathUtils.randomBoolean(0.68f);
                Color col = teal
                        ? new Color(0.28f, 0.92f, 0.68f, MathUtils.random(0.38f, 0.68f))
                        : new Color(0.55f, 0.96f, 0.52f, MathUtils.random(0.32f, 0.58f));
                particles.emitSpore(
                        sx, sy,
                        MathUtils.random(-0.35f, 0.35f),
                        MathUtils.random(0.25f, 0.75f),
                        MathUtils.random(0.07f, 0.14f),
                        MathUtils.random(2.2f, 3.8f),
                        col
                );
            }
        }

        // 2. Warm golden dust motes drifting through visible light shafts
        shaftMoteAccum += dt * 9.0f;
        while (shaftMoteAccum >= 1.0f) {
            shaftMoteAccum -= 1.0f;
            int startRay = (int) Math.floor(left / 14.0f) - 1;
            int endRay = (int) Math.ceil(right / 14.0f) + 1;
            if (endRay >= startRay) {
                int rayIdx = MathUtils.random(startRay, endRay);
                float topX = rayIdx * 14.0f + MathUtils.sin(rayIdx * 2.9f) * 3.0f;
                float tY = MathUtils.random(0.05f, 0.92f);
                float mx = topX + tY * 4.5f + MathUtils.random(-0.2f, 2.2f);
                if (mx >= left && mx <= right) {
                    float my = MathUtils.lerp(13.2f, -1.5f, tY);
                    particles.emitSpore(
                            mx, my,
                            MathUtils.random(-0.2f, 0.3f),
                            MathUtils.random(-0.18f, 0.25f),
                            MathUtils.random(0.06f, 0.12f),
                            MathUtils.random(2.0f, 3.4f),
                            new Color(1.0f, 0.88f, 0.48f, MathUtils.random(0.30f, 0.56f))
                    );
                }
            }
        }

        // 3. WaterPool surface shimmer mist + ceiling droplet spawns
        List<LushCaveStage.WaterPool> visiblePools = new ArrayList<>();
        for (LushCaveStage.WaterPool pool : stage.getWaterPools()) {
            if (pool.x + pool.width >= left && pool.x <= right) {
                visiblePools.add(pool);
            }
        }

        if (!visiblePools.isEmpty()) {
            mistAccum += dt * 8.0f;
            while (mistAccum >= 1.0f) {
                mistAccum -= 1.0f;
                LushCaveStage.WaterPool pool = visiblePools.get(MathUtils.random(visiblePools.size() - 1));
                float mx = MathUtils.random(Math.max(pool.x + 0.2f, left), Math.min(pool.x + pool.width - 0.2f, right));
                float surfaceY = pool.y + pool.height;
                particles.spawnFull(
                        mx, surfaceY + 0.04f,
                        MathUtils.random(-0.25f, 0.25f), MathUtils.random(0.35f, 0.80f),
                        0f, 0.1f, 0.8f,
                        MathUtils.random(1.0f, 1.8f),
                        MathUtils.random(0.10f, 0.18f), 0.03f,
                        1.0f, 45f, 0f,
                        new Color(0.42f, 0.94f, 0.82f, 0.42f),
                        new Color(0.65f, 1.0f, 0.92f, 0.0f),
                        0.28f, 2.5f, true, ParticleSystem.Shape.DIAMOND
                );
            }

            dripAccum += dt * 2.2f;
            while (dripAccum >= 1.0f) {
                dripAccum -= 1.0f;
                LushCaveStage.WaterPool pool = visiblePools.get(MathUtils.random(visiblePools.size() - 1));
                float dx = MathUtils.random(Math.max(pool.x + 0.4f, left), Math.min(pool.x + pool.width - 0.4f, right));
                CeilingDroplet d = new CeilingDroplet();
                d.x = dx;
                d.y = Math.min(top, 11.4f);
                d.vy = -1.2f;
                d.targetY = pool.y + pool.height;
                if (d.y > d.targetY + 1.5f) {
                    droplets.add(d);
                }
            }
        }

        // Update falling ceiling water droplets & emit splash on pool impact
        Iterator<CeilingDroplet> it = droplets.iterator();
        while (it.hasNext()) {
            CeilingDroplet d = it.next();
            d.vy -= 18.0f * dt;
            d.y += d.vy * dt;
            particles.spawn(
                    d.x, d.y,
                    0f, d.vy * 0.15f,
                    0f, 0f, 0f,
                    0.06f, 0.07f, 0.03f,
                    new Color(0.58f, 0.95f, 0.92f, 0.72f),
                    new Color(0.58f, 0.95f, 0.92f, 0.0f),
                    false, ParticleSystem.Shape.STREAK
            );
            if (d.y <= d.targetY) {
                for (int s = 0; s < 5; s++) {
                    float svx = MathUtils.random(-2.2f, 2.2f);
                    float svy = MathUtils.random(1.4f, 3.2f);
                    particles.spawn(
                            d.x, d.targetY + 0.03f,
                            svx, svy,
                            0f, -18.0f, 1.0f,
                            MathUtils.random(0.22f, 0.36f),
                            0.08f, 0.02f,
                            new Color(0.65f, 0.98f, 0.92f, 0.85f),
                            new Color(0.32f, 0.85f, 0.78f, 0.0f),
                            true, ParticleSystem.Shape.DIAMOND
                    );
                }
                it.remove();
            }
        }

        // 4. Crystal cluster ambient sparkles
        crystalAccum += dt * 6.0f;
        while (crystalAccum >= 1.0f) {
            crystalAccum -= 1.0f;
            int startCrys = (int) Math.floor(left / 12.0f) - 1;
            int endCrys = (int) Math.ceil(right / 12.0f) + 1;
            if (endCrys >= startCrys) {
                int i = MathUtils.random(startCrys, endCrys);
                float cx = i * 12.0f + MathUtils.sin(i * 4.3f) * 2.2f + MathUtils.random(-0.4f, 0.5f);
                float cy = Math.abs(MathUtils.cos(i * 2.7f)) * 2.0f + MathUtils.random(0.2f, 1.4f);
                if (cx >= left && cx <= right) {
                    particles.emitSpark(
                            cx, cy,
                            MathUtils.random(-0.3f, 0.3f), MathUtils.random(0.4f, 1.1f),
                            MathUtils.random(0.07f, 0.13f),
                            MathUtils.random(0.45f, 0.85f),
                            new Color(0.55f, 1.0f, 0.82f, 0.85f),
                            new Color(1.0f, 0.86f, 0.38f, 0.0f),
                            false
                    );
                }
            }
        }

        // 5. Room 5 / Lava Shaft rising embers & tense boss-lair heat motes
        if (right >= 74.0f) {
            float emberRate = (right >= LushCaveStage.LAVA_SHAFT_X - 2.0f) ? 24.0f : 7.0f;
            emberAccum += dt * emberRate;
            while (emberAccum >= 1.0f) {
                emberAccum -= 1.0f;
                boolean inShaft = (right >= LushCaveStage.LAVA_SHAFT_X - 1.0f) && MathUtils.randomBoolean(0.75f);
                float ex = inShaft
                        ? MathUtils.random(LushCaveStage.LAVA_SHAFT_X - 0.5f, LushCaveStage.LAVA_SHAFT_X + LushCaveStage.LAVA_SHAFT_WIDTH)
                        : MathUtils.random(Math.max(74.0f, left), right);
                float ey = inShaft
                        ? (stage.isLavaShaftOpen() ? MathUtils.random(-8.5f, 0.5f) : MathUtils.random(-0.2f, 2.5f))
                        : MathUtils.random(Math.max(-1.0f, bottom), 5.0f);
                boolean crimson = MathUtils.randomBoolean(0.32f);
                Color startCol = crimson
                        ? new Color(1.0f, 0.32f, 0.14f, MathUtils.random(0.65f, 0.95f))
                        : new Color(1.0f, 0.72f, 0.22f, MathUtils.random(0.60f, 0.90f));
                Color endCol = new Color(1.0f, 0.25f, 0.05f, 0.0f);
                particles.emitEmber(
                        ex, ey,
                        MathUtils.random(-0.6f, 0.6f),
                        inShaft ? MathUtils.random(2.0f, 4.4f) : MathUtils.random(0.9f, 2.2f),
                        MathUtils.random(0.08f, 0.17f),
                        MathUtils.random(1.4f, 2.9f),
                        startCol, endCol
                );
            }
        }
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
