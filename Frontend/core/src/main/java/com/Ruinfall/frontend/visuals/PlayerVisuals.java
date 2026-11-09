package com.Ruinfall.frontend.visuals;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.Ruinfall.frontend.PlayerController;
import com.Ruinfall.frontend.core.MovementMath;

public class PlayerVisuals implements PlayerController.PlayerEventListener {
    private static final Color ONE_DASH_COLOR = new Color(0.92f, 0.23f, 0.23f, 1.0f);
    private static final Color ZERO_DASH_COLOR = new Color(0.18f, 0.68f, 1.00f, 1.0f);
    private static final Color TWO_DASH_COLOR = new Color(1.00f, 0.46f, 0.72f, 1.0f);

    private final PlayerController player;
    private final GhostTrailEmitter ghostTrail = new GhostTrailEmitter();
    private final Vector2 currentScale = new Vector2(1.0f, 1.0f);
    private final float scaleReturnSpeed = 12.0f;
    private float flashTimer;

    public PlayerVisuals(PlayerController player) {
        this.player = player;
        player.setEventListener(this);
    }

    public void update(float dt) {
        currentScale.x = MathUtils.clamp(MovementMath.approach(currentScale.x, 1.0f, scaleReturnSpeed * dt), 0.3f, 2.0f);
        currentScale.y = MathUtils.clamp(MovementMath.approach(currentScale.y, 1.0f, scaleReturnSpeed * dt), 0.3f, 2.0f);

        flashTimer += dt;

        boolean emitTrail = player.getStateMachine().getCurrentState() == player.getDashState() ||
                            Math.abs(player.getVelocity().x) > 24.0f;

        Color trailColor = player.getDashes().getMaxDashes() >= 2 && player.getDashes().getCurrentDashes() >= 1
            ? new Color(1.0f, 0.46f, 0.72f, 0.55f)
            : new Color(0.18f, 0.72f, 1.0f, 0.55f);

        Rectangle bounds = computeCurrentRenderBounds();
        ghostTrail.update(emitTrail, bounds, trailColor, dt);
    }

    public void render(SpriteBatch batch, Texture blockTexture) {
        ghostTrail.render(batch, blockTexture);

        Rectangle bounds = computeCurrentRenderBounds();
        Color targetColor = resolveBlockColor();

        Color prev = batch.getColor();
        batch.setColor(targetColor);
        batch.draw(blockTexture, bounds.x, bounds.y, bounds.width, bounds.height);
        batch.setColor(prev);
    }

    private Rectangle computeCurrentRenderBounds() {
        Vector2 pos = player.getMotor().getPosition();
        Vector2 colliderSize = player.getMotor().getCurrentColliderSize();

        float width = colliderSize.x * currentScale.x;
        float height = colliderSize.y * currentScale.y;
        float x = pos.x - width * 0.5f;
        float y = pos.y;

        return new Rectangle(x, y, width, height);
    }

    private Color resolveBlockColor() {
        int dashes = player.getDashes().getCurrentDashes();
        Color base = dashes >= 2 ? TWO_DASH_COLOR : (dashes == 1 ? ONE_DASH_COLOR : ZERO_DASH_COLOR);

        if (player.getStamina().isLowStamina()) {
            boolean flashOn = ((int) (flashTimer * 14.0f)) % 2 == 0;
            return flashOn ? new Color(1.0f, 0.45f, 0.45f, 1.0f) : base;
        }

        return base;
    }

    @Override
    public void onJumped(PlayerController.JumpKind kind) {
        if (kind == PlayerController.JumpKind.HYPER) {
            currentScale.set(1.32f, 0.68f);
        } else {
            currentScale.set(0.70f, 1.32f);
        }
    }

    @Override
    public void onDashed(Vector2 dir) {
        if (Math.abs(dir.x) > Math.abs(dir.y)) {
            currentScale.set(1.38f, 0.66f);
        } else if (Math.abs(dir.y) > Math.abs(dir.x)) {
            currentScale.set(0.66f, 1.38f);
        } else {
            currentScale.set(1.22f, 0.82f);
        }
    }

    @Override
    public void onLanded(float impactSpeed) {
        float t = MathUtils.clamp(impactSpeed / player.getStats().maxFall, 0.0f, 1.0f);
        currentScale.x = MathUtils.lerp(1.10f, 1.36f, t);
        currentScale.y = MathUtils.lerp(0.90f, 0.68f, t);
    }

    @Override
    public void onRespawned() {
        currentScale.set(0.6f, 1.4f);
        ghostTrail.clear();
    }
}
