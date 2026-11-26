package com.Ruinfall.frontend.objects.Interactables;

import com.Ruinfall.frontend.objects.GameObject;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Circle;
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
    private final float cooldownDuration = 0.4f;

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
            cooldownTimer -= delta;
            currentState = GrappleState.IDLE;
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
        }
    }

    public boolean canGrapple() {
        return active && currentState == GrappleState.IN_RANGE && cooldownTimer <= 0f;
    }

    public void engage() {
        if (canGrapple()) {
            currentState = GrappleState.ACTIVE;
        }
    }

    public void release() {
        currentState = GrappleState.IDLE;
        cooldownTimer = cooldownDuration;
    }

    public void render(SpriteBatch batch, Texture blockTexture) {
        if (batch == null || blockTexture == null || !active) return;

        // Draw faint range indicator when player is in range or active
        if (currentState == GrappleState.IN_RANGE || currentState == GrappleState.ACTIVE) {
            batch.setColor(1f, 1f, 0.3f, 0.12f);
            batch.draw(
                blockTexture,
                centerPos.x - grappleRadius,
                centerPos.y - grappleRadius,
                grappleRadius * 2f,
                grappleRadius * 2f
            );
        }

        // Tint anchor by state
        switch (currentState) {
            case IDLE:
                batch.setColor(Color.CYAN);
                break;
            case IN_RANGE:
                batch.setColor(Color.YELLOW);
                break;
            case ACTIVE:
                batch.setColor(Color.GREEN);
                break;
        }

        batch.draw(blockTexture, x, y, width, height);
        batch.setColor(Color.WHITE);
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

    public float getGrappleRadius() {
        return grappleRadius;
    }

    public GrappleState getState() {
        return currentState;
    }
}
