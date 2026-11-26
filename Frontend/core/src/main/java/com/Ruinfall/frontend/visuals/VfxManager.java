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
 * - Hit-stop (freeze frames on dash, parry, stalactite crush, and boss defeat)
 * - Celeste dash ripple (cyan-to-white expanding ring + short freeze frame)
 * - Celeste spike death burst (8 radial orbs + quick screen wipe + instant respawn)
 * - Parry flash (world-space starburst + expanding ring + screen flash)
 */
public class VfxManager {

    private static class ExpandingRing {
        float x, y;
        float radius;
        float maxRadius;
        float timer;
        float duration;
        Color startColor;
        Color endColor;
        boolean starburst;

        ExpandingRing(float x, float y, Color startColor, Color endColor, float maxRadius, float duration, boolean starburst) {
            this.x = x;
            this.y = y;
            this.radius = 0.2f;
            this.maxRadius = maxRadius;
            this.timer = duration;
            this.duration = duration;
            this.startColor = new Color(startColor);
            this.endColor = new Color(endColor);
            this.starburst = starburst;
        }
    }

    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    private final Matrix4 screenProjection = new Matrix4();
    private final List<ExpandingRing> rings = new ArrayList<>();

    private float hitStopTimer = 0f;

    // Screen flash state
    private final Color flashColor = new Color(1f, 1f, 1f, 0f);
    private float flashTimer = 0f;
    private float flashDuration = 0.15f;
    private float flashStartAlpha = 0f;

    // Celeste death burst + wipe state
    private boolean deathActive = false;
    private float deathX, deathY;
    private float deathTimer = 0f;
    private float deathDuration = 0.42f;
    private boolean respawnFired = false;
    private Runnable onDeathRespawn;

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

    public boolean isDeathActive() {
        return deathActive;
    }

    /**
     * Celeste-style instant death: 8 radial bursting orbs + quick dark screen wipe,
     * invoking onRespawn cleanly at the wipe peak (~0.24s).
     */
    public void triggerDeathBurst(float worldX, float worldY, Runnable onRespawn) {
        triggerDeathBurst(worldX, worldY, 0.42f, onRespawn);
    }

    public void triggerDeathBurst(float worldX, float worldY, float duration, Runnable onRespawn) {
        if (deathActive) return;
        this.deathActive = true;
        this.deathX = worldX;
        this.deathY = worldY;
        this.deathDuration = Math.max(0.25f, duration);
        this.deathTimer = 0f;
        this.respawnFired = false;
        this.onDeathRespawn = onRespawn;
        triggerScreenFlash(new Color(1.0f, 0.35f, 0.35f, 1.0f), 0.35f, 0.14f);
    }

    /**
     * Celeste-style dash juice: crisp freeze frame + cyan-to-white expanding ripple.
     */
    public void triggerDashRipple(float worldX, float worldY) {
        triggerHitStop(0.042f);
        rings.add(new ExpandingRing(
            worldX, worldY,
            new Color(0.28f, 0.94f, 1.0f, 1.0f),
            new Color(1.0f, 1.0f, 1.0f, 1.0f),
            2.1f, 0.22f, false
        ));
    }

    public void triggerParryFlash(float worldX, float worldY) {
        triggerHitStop(0.11f);
        triggerScreenFlash(new Color(1.0f, 0.96f, 0.78f, 1.0f), 0.48f, 0.18f);
        rings.add(new ExpandingRing(worldX, worldY, new Color(1.0f, 0.95f, 0.55f, 1.0f), Color.WHITE, 3.8f, 0.28f, true));
        rings.add(new ExpandingRing(worldX, worldY, Color.WHITE, Color.WHITE, 2.2f, 0.18f, false));
    }

    public void triggerImpactRing(float worldX, float worldY, Color color, float maxRadius, float duration) {
        rings.add(new ExpandingRing(worldX, worldY, color, color, maxRadius, duration, false));
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

        if (deathActive) {
            deathTimer += dt;
            if (!respawnFired && deathTimer >= deathDuration * 0.58f) {
                respawnFired = true;
                if (onDeathRespawn != null) {
                    onDeathRespawn.run();
                }
            }
            if (deathTimer >= deathDuration) {
                deathActive = false;
                onDeathRespawn = null;
            }
        }

        Iterator<ExpandingRing> it = rings.iterator();
        while (it.hasNext()) {
            ExpandingRing ring = it.next();
            ring.timer -= dt;
            if (ring.timer <= 0f) {
                it.remove();
            } else {
                float progress = 1.0f - (ring.timer / ring.duration);
                float eased = 1.0f - (1.0f - progress) * (1.0f - progress);
                ring.radius = MathUtils.lerp(0.25f, ring.maxRadius, eased);
            }
        }
    }

    public void renderWorld(OrthographicCamera worldCamera) {
        if (rings.isEmpty() && !deathActive) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(worldCamera.combined);

        // Expanding rings & parry starburst
        if (!rings.isEmpty()) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            for (ExpandingRing ring : rings) {
                float progress = 1.0f - MathUtils.clamp(ring.timer / ring.duration, 0f, 1f);
                float alpha = MathUtils.clamp(ring.timer / ring.duration, 0f, 1f);
                float r = MathUtils.lerp(ring.startColor.r, ring.endColor.r, progress);
                float g = MathUtils.lerp(ring.startColor.g, ring.endColor.g, progress);
                float b = MathUtils.lerp(ring.startColor.b, ring.endColor.b, progress);

                shapeRenderer.setColor(r, g, b, alpha);
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
        }

        // Celeste radial death burst orbs (8 orbs flying outward)
        if (deathActive && !respawnFired) {
            float t = MathUtils.clamp(deathTimer / (deathDuration * 0.58f), 0f, 1f);
            float dist = MathUtils.lerp(0.15f, 2.4f, 1f - (1f - t) * (1f - t));
            float orbRadius = MathUtils.lerp(0.32f, 0.04f, t);

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            for (int i = 0; i < 8; i++) {
                float angle = i * (MathUtils.PI2 / 8f) + t * 0.4f;
                float ox = deathX + MathUtils.cos(angle) * dist;
                float oy = deathY + MathUtils.sin(angle) * dist;
                shapeRenderer.setColor(0.35f, 0.95f, 0.90f, 1.0f - t * 0.4f);
                shapeRenderer.circle(ox, oy, orbRadius, 16);
                shapeRenderer.setColor(1.0f, 1.0f, 1.0f, 1.0f - t * 0.5f);
                shapeRenderer.circle(ox, oy, orbRadius * 0.55f, 12);
            }
            shapeRenderer.end();
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    public void renderScreenOverlay() {
        boolean hasFlash = flashTimer > 0f;
        boolean hasWipe = deathActive;
        if (!hasFlash && !hasWipe) return;

        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        screenProjection.setToOrtho2D(0, 0, w, h);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(screenProjection);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        if (hasFlash) {
            float alpha = flashStartAlpha * MathUtils.clamp(flashTimer / flashDuration, 0f, 1f);
            shapeRenderer.setColor(flashColor.r, flashColor.g, flashColor.b, alpha);
            shapeRenderer.rect(0, 0, w, h);
        }

        // Quick Celeste-style curtain wipe on spike death
        if (hasWipe) {
            float half = deathDuration * 0.5f;
            float wipeAlpha = (deathTimer < half)
                ? MathUtils.clamp(deathTimer / half, 0f, 1f)
                : MathUtils.clamp(1f - (deathTimer - half) / half, 0f, 1f);
            shapeRenderer.setColor(0.03f, 0.05f, 0.07f, wipeAlpha * 0.92f);
            shapeRenderer.rect(0, 0, w, h);
        }

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    public void dispose() {
        shapeRenderer.dispose();
    }
}
