package com.Ruinfall.frontend.objects.Interactables;

import com.Ruinfall.frontend.objects.GameObject;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class GrapplePoint extends GameObject {

    public enum GrappleState {
        IDLE,       // Player is out of range
        IN_RANGE,   // Player is inside grappleRadius (ready to hook)
        ACTIVE      // Player is currently grappling to this point
    }

    private GrappleState currentState = GrappleState.IDLE;
    private float grappleRadius;
    private float cooldownTimer = 0f;
    private final float cooldownDuration = 0.35f;
    private boolean targeted = false;

    private final Circle rangeCircle;
    private final Vector2 centerPos;

    public GrapplePoint(float x, float y, float width, float height, float grappleRadius) {
        super(x, y, width, height, 0f, Color.CYAN);
        this.grappleRadius = grappleRadius;
        this.centerPos = new Vector2(x + width * 0.5f, y + height * 0.5f);
        this.rangeCircle = new Circle(centerPos.x, centerPos.y, grappleRadius);
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        centerPos.set(x + width * 0.5f, y + height * 0.5f);
        rangeCircle.setPosition(centerPos);

        if (cooldownTimer > 0f) {
            cooldownTimer = Math.max(0f, cooldownTimer - delta);
            if (currentState != GrappleState.ACTIVE) {
                currentState = GrappleState.IDLE;
            }
        }
    }

    // THIS IS checkPlayerRange
    public void checkPlayerRange(float playerCenterX, float playerCenterY) {
        if (!active || cooldownTimer > 0f || currentState == GrappleState.ACTIVE) {
            return;
        }

        if (rangeCircle.contains(playerCenterX, playerCenterY)) {
            currentState = GrappleState.IN_RANGE;
        } else {
            currentState = GrappleState.IDLE;
            targeted = false;
        }
    }

    public boolean canGrapple() {
        return active && currentState == GrappleState.IN_RANGE && cooldownTimer <= 0f;
    }

    public float distanceSquaredTo(float px, float py) {
        float dx = centerPos.x - px;
        float dy = centerPos.y - py;
        return dx * dx + dy * dy;
    }

    public void setTargeted(boolean targeted) {
        this.targeted = targeted;
    }

    public boolean isTargeted() {
        return targeted;
    }

    public float getGrappleRadius() {
        return grappleRadius;
    }

    public void engage() {
        if (canGrapple()) {
            currentState = GrappleState.ACTIVE;
            targeted = true;
        }
    }

    public void release() {
        currentState = GrappleState.IDLE;
        targeted = false;
        cooldownTimer = cooldownDuration;
    }

    public void render(SpriteBatch batch, Texture blockTexture) {
        if (batch == null || blockTexture == null || !active) return;

        float pulse = 0.5f + 0.5f * MathUtils.sin(stateTime * 5.0f);

        // Draw circular latch-range ring when player is within radius or actively grappling
        if (currentState == GrappleState.IN_RANGE || currentState == GrappleState.ACTIVE) {
            if (currentState == GrappleState.ACTIVE) {
                drawRing(batch, blockTexture, centerPos.x, centerPos.y, grappleRadius, 0.06f, 32, 0.35f, 1.0f, 0.55f, 0.28f);
            } else if (targeted) {
                float ringAlpha = 0.28f + 0.16f * pulse;
                drawRing(batch, blockTexture, centerPos.x, centerPos.y, grappleRadius, 0.065f, 32, 1.0f, 0.92f, 0.30f, ringAlpha);
                // Tight pulsing lock-on reticle around anchor
                float lockRadius = Math.max(width, height) * (0.95f + 0.18f * pulse);
                drawRing(batch, blockTexture, centerPos.x, centerPos.y, lockRadius, 0.06f, 16, 1.0f, 0.96f, 0.45f, 0.85f);
            } else {
                drawRing(batch, blockTexture, centerPos.x, centerPos.y, grappleRadius, 0.045f, 28, 0.55f, 0.92f, 1.0f, 0.16f);
            }
        }

        // Subtle ambient glow halo around the anchor node
        float haloScale = (targeted || currentState == GrappleState.ACTIVE)
            ? (1.75f + 0.22f * pulse)
            : (1.45f + 0.12f * pulse);
        float haloW = width * haloScale;
        float haloH = height * haloScale;
        if (currentState == GrappleState.ACTIVE) {
            batch.setColor(0.30f, 1.0f, 0.55f, 0.32f);
        } else if (currentState == GrappleState.IN_RANGE) {
            batch.setColor(1.0f, 0.92f, 0.30f, targeted ? 0.36f : 0.22f);
        } else {
            batch.setColor(0.25f, 0.85f, 1.0f, 0.18f + 0.06f * pulse);
        }
        batch.draw(blockTexture, centerPos.x - haloW * 0.5f, centerPos.y - haloH * 0.5f, haloW, haloH);

        // Dark outer rim for crisp contrast against arena background
        float rimPad = 0.06f;
        batch.setColor(0.08f, 0.12f, 0.18f, 0.95f);
        batch.draw(blockTexture, x - rimPad, y - rimPad, width + rimPad * 2f, height + rimPad * 2f);

        // Tint anchor body by state
        switch (currentState) {
            case IDLE:
                batch.setColor(0.25f, 0.88f, 0.98f, 1f);
                break;
            case IN_RANGE:
                batch.setColor(targeted ? Color.GOLD : new Color(0.95f, 0.95f, 0.55f, 1f));
                break;
            case ACTIVE:
                batch.setColor(0.35f, 1.0f, 0.52f, 1f);
                break;
        }
        batch.draw(blockTexture, x, y, width, height);

        // Bright center pip
        float pipW = width * 0.42f;
        float pipH = height * 0.42f;
        batch.setColor(1f, 1f, 1f, 0.92f);
        batch.draw(blockTexture, centerPos.x - pipW * 0.5f, centerPos.y - pipH * 0.5f, pipW, pipH);
        batch.setColor(Color.WHITE);
    }

    private static void drawRing(
        SpriteBatch batch,
        Texture tex,
        float cx,
        float cy,
        float radius,
        float thickness,
        int segments,
        float r,
        float g,
        float b,
        float a
    ) {
        batch.setColor(r, g, b, a);
        float segLen = (MathUtils.PI2 * radius) / segments * 1.05f;
        int texW = tex.getWidth();
        int texH = tex.getHeight();
        for (int i = 0; i < segments; i++) {
            float angleRad = (i / (float) segments) * MathUtils.PI2;
            float px = cx + MathUtils.cos(angleRad) * radius;
            float py = cy + MathUtils.sin(angleRad) * radius;
            float tangentDeg = angleRad * MathUtils.radiansToDegrees + 90f;
            batch.draw(
                tex,
                px - segLen * 0.5f,
                py - thickness * 0.5f,
                segLen * 0.5f,
                thickness * 0.5f,
                segLen,
                thickness,
                1f, 1f,
                tangentDeg,
                0, 0,
                texW, texH,
                false, false
            );
        }
    }

    public void renderTether(SpriteBatch batch, Texture blockTexture, float playerCenterX, float playerCenterY) {
        if (currentState != GrappleState.ACTIVE) return;

        float dx = centerPos.x - playerCenterX;
        float dy = centerPos.y - playerCenterY;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float angleDeg = (float) Math.toDegrees(Math.atan2(dy, dx));
        float thickness = 0.08f;

        batch.setColor(Color.WHITE);
        batch.draw(
            blockTexture,
            playerCenterX,
            playerCenterY - thickness * 0.5f,
            0f,
            thickness * 0.5f,
            length,
            thickness,
            1f, 1f,
            angleDeg,
            0, 0,
            blockTexture.getWidth(),
            blockTexture.getHeight(),
            false, false
        );
    }

    @Override
    public Rectangle getGrazeHitbox() {
        return new Rectangle(
            centerPos.x - grappleRadius,
            centerPos.y - grappleRadius,
            grappleRadius * 2f,
            grappleRadius * 2f
        );
    }

    public Vector2 getCenter() {
        return centerPos;
    }

    public GrappleState getState() {
        return currentState;
    }
}
