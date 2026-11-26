package com.Ruinfall.frontend.visuals;

import com.Ruinfall.frontend.world.LushCaveStage;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

import java.util.List;

/**
 * Renders Celeste-style procedural keycap badges inside a high-contrast dark translucent pill
 * with gentle floating bob, pulsing keycap rim, eased proximity fade, and safe-area edge clamping.
 * Uses only LibGDX's built-in BitmapFont and ShapeRenderer (zero external assets).
 */
public class WorldHintRenderer {
    private final BitmapFont font;
    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    private final GlyphLayout layout = new GlyphLayout();
    private final Matrix4 screenProjection = new Matrix4();
    private final Vector3 projected = new Vector3();

    private float animTime = 0f;

    public WorldHintRenderer() {
        this.font = new BitmapFont();
        this.font.getData().setScale(1.1f);
    }

    public void update(float dt) {
        animTime += dt;
    }

    public void render(SpriteBatch batch, OrthographicCamera worldCamera, List<LushCaveStage.HintZone> hints, float playerX, float playerY) {
        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();
        if (screenW <= 0 || screenH <= 0) return;

        float uiScale = MathUtils.clamp(screenH / 720.0f, 0.85f, 1.8f);
        float fontScale = 1.05f * uiScale;
        font.getData().setScale(fontScale);

        screenProjection.setToOrtho2D(0, 0, screenW, screenH);
        float safeMargin = 20f * uiScale;

        for (int idx = 0; idx < hints.size(); idx++) {
            LushCaveStage.HintZone hint = hints.get(idx);
            float dist = (float) Math.hypot(playerX - hint.x, playerY - hint.y);
            if (dist > hint.triggerRadius) continue;

            // Smooth ease-out proximity fade
            float raw = MathUtils.clamp(1.0f - (dist / hint.triggerRadius), 0.0f, 1.0f);
            float alpha = MathUtils.clamp(raw * 1.75f, 0.0f, 1.0f);
            alpha = alpha * alpha * (3.0f - 2.0f * alpha); // smoothstep
            if (alpha <= 0.02f) continue;

            // Measure keycap + label segments
            float padX = 12f * uiScale;
            float padY = 8f * uiScale;
            float keyPadX = 8f * uiScale;
            float keyPadY = 5f * uiScale;
            float gapKeyLabel = 8f * uiScale;
            float gapPairs = 18f * uiScale;

            layout.setText(font, hint.key1);
            float k1W = layout.width + keyPadX * 2f;
            float textH = layout.height;

            layout.setText(font, hint.label1);
            float l1W = layout.width;

            boolean hasSecond = hint.key2 != null && hint.label2 != null;
            float k2W = 0f, l2W = 0f;
            if (hasSecond) {
                layout.setText(font, hint.key2);
                k2W = layout.width + keyPadX * 2f;
                layout.setText(font, hint.label2);
                l2W = layout.width;
            }

            float contentW = k1W + gapKeyLabel + l1W + (hasSecond ? (gapPairs + k2W + gapKeyLabel + l2W) : 0f);
            float pillW = contentW + padX * 2f;
            float keyH = textH + keyPadY * 2f;
            float pillH = keyH + padY * 2f;

            // Project world position + gentle Celeste floating bob
            projected.set(hint.x, hint.y, 0.0f);
            worldCamera.project(projected);
            float bobY = MathUtils.sin(animTime * 2.4f + idx * 1.3f) * 3.5f * uiScale;

            // Clamp inside safe area so pills never clip off-screen
            float pillX = MathUtils.clamp(projected.x - pillW * 0.5f, safeMargin, screenW - pillW - safeMargin);
            float pillY = MathUtils.clamp(projected.y - pillH * 0.5f + bobY, safeMargin, screenH - pillH - safeMargin);

            float pulse = 0.5f + 0.5f * MathUtils.sin(animTime * 4.2f + idx);

            // --- Draw dark translucent pill + tactile keycap badges ---
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

            shapeRenderer.setProjectionMatrix(screenProjection);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

            // Outer drop shadow
            shapeRenderer.setColor(0.01f, 0.03f, 0.04f, alpha * 0.55f);
            shapeRenderer.rect(pillX + 2f * uiScale, pillY - 3f * uiScale, pillW, pillH);

            // Dark translucent pill body (high contrast >= 4.5:1)
            shapeRenderer.setColor(0.04f, 0.09f, 0.11f, alpha * 0.86f);
            shapeRenderer.rect(pillX, pillY, pillW, pillH);

            // Subtle jade-gold top accent line
            shapeRenderer.setColor(0.38f, 0.84f, 0.56f, alpha * 0.70f);
            shapeRenderer.rect(pillX, pillY + pillH - 2f * uiScale, pillW, 2f * uiScale);

            // Keycap 1 badge (tactile keycap base + top face)
            float curX = pillX + padX;
            float keyY = pillY + padY;
            drawKeycapFill(curX, keyY, k1W, keyH, uiScale, alpha, pulse);

            float curX2 = curX + k1W + gapKeyLabel + l1W + gapPairs;
            if (hasSecond) {
                drawKeycapFill(curX2, keyY, k2W, keyH, uiScale, alpha, pulse);
            }

            shapeRenderer.end();

            // Keycap pulsing outline
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(0.88f, 0.82f, 0.45f, alpha * (0.45f + 0.45f * pulse));
            shapeRenderer.rect(curX, keyY, k1W, keyH);
            if (hasSecond) {
                shapeRenderer.rect(curX2, keyY, k2W, keyH);
            }
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);

            // --- Draw crisp text inside keycaps & labels ---
            batch.setProjectionMatrix(screenProjection);
            batch.begin();

            float textY = keyY + keyH - keyPadY;

            // Key 1 text (warm ivory-gold)
            font.setColor(1.0f, 0.96f, 0.76f, alpha);
            font.draw(batch, hint.key1, curX + keyPadX, textY);

            // Label 1 text (crisp off-white)
            font.setColor(0.88f, 0.96f, 0.92f, alpha);
            font.draw(batch, hint.label1, curX + k1W + gapKeyLabel, textY);

            if (hasSecond) {
                // Subtle separator dot
                font.setColor(0.45f, 0.72f, 0.62f, alpha * 0.75f);
                font.draw(batch, "•", curX2 - gapPairs * 0.65f, textY);

                // Key 2 text
                font.setColor(1.0f, 0.96f, 0.76f, alpha);
                font.draw(batch, hint.key2, curX2 + keyPadX, textY);

                // Label 2 text
                font.setColor(0.88f, 0.96f, 0.92f, alpha);
                font.draw(batch, hint.label2, curX2 + k2W + gapKeyLabel, textY);
            }

            font.setColor(Color.WHITE);
            batch.end();
        }
    }

    private void drawKeycapFill(float x, float y, float w, float h, float uiScale, float alpha, float pulse) {
        // Keycap bottom bevel (3D key depth)
        shapeRenderer.setColor(0.08f, 0.18f, 0.18f, alpha * 0.95f);
        shapeRenderer.rect(x, y - 2f * uiScale, w, h);
        // Keycap face
        float glow = 0.05f * pulse;
        shapeRenderer.setColor(0.14f + glow, 0.28f + glow, 0.27f + glow, alpha * 0.95f);
        shapeRenderer.rect(x, y, w, h);
    }

    public void renderBanner(SpriteBatch batch, String title, String subtitle, float alpha) {
        if (alpha <= 0.01f) return;
        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();
        float uiScale = MathUtils.clamp(screenH / 720.0f, 0.85f, 1.8f);
        screenProjection.setToOrtho2D(0, 0, screenW, screenH);

        batch.setProjectionMatrix(screenProjection);
        batch.begin();

        font.getData().setScale(1.65f * uiScale);
        layout.setText(font, title);
        float tx = (screenW - layout.width) * 0.5f;
        float ty = screenH * 0.84f;
        font.setColor(0.03f, 0.06f, 0.07f, alpha * 0.92f);
        font.draw(batch, layout, tx + 2f, ty - 2f);
        font.setColor(1.0f, 0.92f, 0.55f, alpha);
        font.draw(batch, layout, tx, ty);

        if (subtitle != null && !subtitle.isEmpty()) {
            font.getData().setScale(1.05f * uiScale);
            layout.setText(font, subtitle);
            float sx = (screenW - layout.width) * 0.5f;
            float sy = ty - 30f * uiScale;
            font.setColor(0.84f, 0.96f, 0.90f, alpha * 0.92f);
            font.draw(batch, layout, sx, sy);
        }

        font.getData().setScale(1.05f * uiScale);
        font.setColor(Color.WHITE);
        batch.end();
    }

    public void renderHud(SpriteBatch batch, int hp, int maxHp, int dashes, int maxDashes,
                          boolean bossAwake, int bossHp, int bossMaxHp, String bossName) {
        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();
        if (screenW <= 0 || screenH <= 0 || maxHp <= 0) return;

        float uiScale = MathUtils.clamp(screenH / 720.0f, 0.85f, 1.8f);
        screenProjection.setToOrtho2D(0, 0, screenW, screenH);
        float margin = 18f * uiScale;

        // Top-left Player HP pill + Dash pips
        float pipSize = 14f * uiScale;
        float pipGap = 8f * uiScale;
        float padX = 12f * uiScale;
        float padY = 9f * uiScale;
        float labelW = 30f * uiScale;
        float pipsW = maxHp * pipSize + Math.max(0, maxHp - 1) * pipGap;
        float dashSep = 14f * uiScale;
        float dashPipW = 10f * uiScale;
        float dashBlockW = maxDashes * dashPipW + Math.max(0, maxDashes - 1) * (5f * uiScale);
        float hudW = padX * 2f + labelW + pipsW + dashSep + dashBlockW;
        float hudH = pipSize + padY * 2f;
        float hudX = margin;
        float hudY = screenH - margin - hudH;

        float lowPulse = (hp <= 1) ? (0.5f + 0.5f * MathUtils.sin(animTime * 9.0f)) : 0f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(screenProjection);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Backdrop shadow + dark translucent pill
        shapeRenderer.setColor(0.01f, 0.03f, 0.04f, 0.55f);
        shapeRenderer.rect(hudX + 2f * uiScale, hudY - 2f * uiScale, hudW, hudH);
        shapeRenderer.setColor(0.04f, 0.09f, 0.11f, 0.84f);
        shapeRenderer.rect(hudX, hudY, hudW, hudH);
        shapeRenderer.setColor(0.38f, 0.84f, 0.56f, 0.65f);
        shapeRenderer.rect(hudX, hudY + hudH - 2f * uiScale, hudW, 2f * uiScale);

        // Player HP pips
        float pipStartX = hudX + padX + labelW;
        float pipY = hudY + padY;
        for (int i = 0; i < maxHp; i++) {
            float px = pipStartX + i * (pipSize + pipGap);
            if (i < hp) {
                shapeRenderer.setColor(0.95f, 0.30f + lowPulse * 0.35f, 0.36f + lowPulse * 0.25f, 0.96f);
                shapeRenderer.rect(px, pipY, pipSize, pipSize);
                shapeRenderer.setColor(1.0f, 0.78f, 0.76f, 0.75f);
                shapeRenderer.rect(px + 2f * uiScale, pipY + pipSize - 4f * uiScale, pipSize - 4f * uiScale, 2f * uiScale);
            } else {
                shapeRenderer.setColor(0.14f, 0.20f, 0.22f, 0.85f);
                shapeRenderer.rect(px, pipY, pipSize, pipSize);
            }
        }

        // Dash pips (cyan when ready, dim when spent)
        float dashStartX = pipStartX + pipsW + dashSep;
        float dashY = hudY + padY + (pipSize - dashPipW) * 0.5f;
        for (int i = 0; i < maxDashes; i++) {
            float dx = dashStartX + i * (dashPipW + 5f * uiScale);
            if (i < dashes) {
                shapeRenderer.setColor(0.28f, 0.92f, 1.0f, 0.95f);
            } else {
                shapeRenderer.setColor(0.16f, 0.24f, 0.28f, 0.75f);
            }
            shapeRenderer.rect(dx, dashY, dashPipW, dashPipW);
        }

        // Boss HP Bar at bottom-center when Wave Mantis is awake
        float bossBarW = Math.min(420f * uiScale, screenW * 0.55f);
        float bossBarH = 12f * uiScale;
        float bossBarX = (screenW - bossBarW) * 0.5f;
        float bossBarY = margin + 8f * uiScale;
        if (bossAwake && bossMaxHp > 0) {
            float ratio = MathUtils.clamp((float) bossHp / (float) bossMaxHp, 0f, 1f);
            shapeRenderer.setColor(0.03f, 0.06f, 0.08f, 0.85f);
            shapeRenderer.rect(bossBarX - 4f * uiScale, bossBarY - 4f * uiScale, bossBarW + 8f * uiScale, bossBarH + 8f * uiScale);
            shapeRenderer.setColor(0.16f, 0.12f, 0.16f, 0.9f);
            shapeRenderer.rect(bossBarX, bossBarY, bossBarW, bossBarH);
            shapeRenderer.setColor(0.95f, 0.28f, 0.36f, 0.96f);
            shapeRenderer.rect(bossBarX, bossBarY, bossBarW * ratio, bossBarH);
            shapeRenderer.setColor(1.0f, 0.72f, 0.68f, 0.55f);
            shapeRenderer.rect(bossBarX, bossBarY + bossBarH - 3f * uiScale, bossBarW * ratio, 2f * uiScale);
        }

        shapeRenderer.end();

        // Pip outlines
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.75f, 0.92f, 0.84f, 0.45f);
        for (int i = 0; i < maxHp; i++) {
            shapeRenderer.rect(pipStartX + i * (pipSize + pipGap), pipY, pipSize, pipSize);
        }
        if (bossAwake && bossMaxHp > 0) {
            shapeRenderer.setColor(0.95f, 0.85f, 0.55f, 0.65f);
            shapeRenderer.rect(bossBarX - 4f * uiScale, bossBarY - 4f * uiScale, bossBarW + 8f * uiScale, bossBarH + 8f * uiScale);
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Text labels
        batch.setProjectionMatrix(screenProjection);
        batch.begin();
        font.getData().setScale(0.95f * uiScale);
        font.setColor(0.88f, 0.96f, 0.92f, 0.95f);
        font.draw(batch, "HP", hudX + padX, hudY + hudH - padY);

        if (bossAwake && bossName != null) {
            font.getData().setScale(0.95f * uiScale);
            layout.setText(font, bossName.toUpperCase());
            font.setColor(1.0f, 0.92f, 0.70f, 0.95f);
            font.draw(batch, layout, (screenW - layout.width) * 0.5f, bossBarY + bossBarH + 22f * uiScale);
        }
        font.setColor(Color.WHITE);
        batch.end();
    }

    public void dispose() {
        font.dispose();
        shapeRenderer.dispose();
    }
}
