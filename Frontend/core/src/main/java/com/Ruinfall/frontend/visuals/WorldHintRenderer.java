package com.Ruinfall.frontend.visuals;

import com.Ruinfall.frontend.world.LushCaveStage;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

import java.util.List;

/**
 * Renders subtle floating world-space button hints in crisp screen-space pixels
 * using LibGDX's built-in default BitmapFont (no external assets required).
 */
public class WorldHintRenderer {
    private final BitmapFont font;
    private final GlyphLayout layout = new GlyphLayout();
    private final Matrix4 screenProjection = new Matrix4();
    private final Vector3 projected = new Vector3();

    public WorldHintRenderer() {
        this.font = new BitmapFont();
        this.font.getData().setScale(1.15f);
    }

    public void render(SpriteBatch batch, OrthographicCamera worldCamera, List<LushCaveStage.HintZone> hints, float playerX, float playerY) {
        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();
        screenProjection.setToOrtho2D(0, 0, screenW, screenH);

        batch.setProjectionMatrix(screenProjection);
        batch.begin();

        for (LushCaveStage.HintZone hint : hints) {
            float dist = (float) Math.hypot(playerX - hint.x, playerY - hint.y);
            if (dist > hint.triggerRadius) continue;

            float alpha = MathUtils.clamp(1.0f - (dist / hint.triggerRadius), 0.0f, 1.0f);
            alpha = MathUtils.clamp(alpha * 1.6f, 0.0f, 0.92f);

            projected.set(hint.x, hint.y, 0.0f);
            worldCamera.project(projected);

            layout.setText(font, hint.text);
            float drawX = projected.x - layout.width * 0.5f;
            float drawY = projected.y + layout.height * 0.5f;

            // Subtle drop shadow for legibility
            font.setColor(0.02f, 0.05f, 0.06f, alpha * 0.85f);
            font.draw(batch, layout, drawX + 1.5f, drawY - 1.5f);

            // Bioluminescent pale-mint text
            font.setColor(0.78f, 0.97f, 0.88f, alpha);
            font.draw(batch, layout, drawX, drawY);
        }

        font.setColor(Color.WHITE);
        batch.end();
    }

    public void renderBanner(SpriteBatch batch, String title, String subtitle, float alpha) {
        if (alpha <= 0.01f) return;
        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();
        screenProjection.setToOrtho2D(0, 0, screenW, screenH);

        batch.setProjectionMatrix(screenProjection);
        batch.begin();

        font.getData().setScale(1.6f);
        layout.setText(font, title);
        float tx = (screenW - layout.width) * 0.5f;
        float ty = screenH * 0.82f;
        font.setColor(0.05f, 0.05f, 0.08f, alpha * 0.9f);
        font.draw(batch, layout, tx + 2f, ty - 2f);
        font.setColor(1.0f, 0.92f, 0.55f, alpha);
        font.draw(batch, layout, tx, ty);

        if (subtitle != null && !subtitle.isEmpty()) {
            font.getData().setScale(1.05f);
            layout.setText(font, subtitle);
            float sx = (screenW - layout.width) * 0.5f;
            float sy = ty - 30f;
            font.setColor(0.82f, 0.95f, 0.90f, alpha * 0.9f);
            font.draw(batch, layout, sx, sy);
        }

        font.getData().setScale(1.15f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    public void dispose() {
        font.dispose();
    }
}
