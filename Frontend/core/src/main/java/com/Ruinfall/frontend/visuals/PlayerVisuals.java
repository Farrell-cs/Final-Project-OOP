package com.Ruinfall.frontend.visuals;

import com.Ruinfall.frontend.PlayerController;
import com.Ruinfall.frontend.core.MovementMath;
import com.Ruinfall.frontend.systems.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * Renders the player using the pixel-art sprite sheet from AssetManager
 * (with automatic procedural fallback), squash-and-stretch, facing flips,
 * dash-state tinting, low-stamina blink, and ghost trails.
 */
public class PlayerVisuals implements PlayerController.PlayerEventListener {
    private static final Color ONE_DASH_COLOR = new Color(0.92f, 0.23f, 0.23f, 1.0f);
    private static final Color ZERO_DASH_COLOR = new Color(0.18f, 0.68f, 1.00f, 1.0f);
    private static final Color TWO_DASH_COLOR = new Color(1.00f, 0.46f, 0.72f, 1.0f);

    private final PlayerController player;
    private final GhostTrailEmitter ghostTrail = new GhostTrailEmitter();
    private final Vector2 currentScale = new Vector2(1.0f, 1.0f);
    private final float scaleReturnSpeed = 12.0f;

    private float flashTimer = 0f;
    private float animTimer = 0f;
    private String currentAnimKey = "idle";

    public PlayerVisuals(PlayerController player) {
        this.player = player;
        player.setEventListener(this);
        AssetManager.getInstance().loadPlayerSprites();
    }

    public void update(float dt) {
        currentScale.x = MathUtils.clamp(MovementMath.approach(currentScale.x, 1.0f, scaleReturnSpeed * dt), 0.3f, 2.0f);
        currentScale.y = MathUtils.clamp(MovementMath.approach(currentScale.y, 1.0f, scaleReturnSpeed * dt), 0.3f, 2.0f);

        flashTimer += dt;

        String nextKey = resolveAnimationKey();
        if (!nextKey.equals(currentAnimKey)) {
            currentAnimKey = nextKey;
            animTimer = 0f;
        } else {
            animTimer += dt;
        }

        boolean emitTrail = player.getStateMachine().getCurrentState() == player.getDashState() ||
                            Math.abs(player.getVelocity().x) > 24.0f;

        Color trailColor = player.getDashes().getMaxDashes() >= 2 && player.getDashes().getCurrentDashes() >= 1
            ? new Color(1.0f, 0.46f, 0.72f, 0.55f)
            : new Color(0.18f, 0.72f, 1.0f, 0.55f);

        Rectangle bounds = computeCurrentRenderBounds();
        ghostTrail.update(emitTrail, bounds, trailColor, dt);
    }

    private String resolveAnimationKey() {
        var state = player.getStateMachine().getCurrentState();
        if (state == player.getDashState()) return "dash";
        if (state == player.getSwingState()) return "swing";
        if (state == player.getClimbState()) return "climb";

        if (!player.getMotor().isGrounded()) {
            return (player.getVelocity().y > 0.5f) ? "jump" : "fall";
        }
        if (Math.abs(player.getVelocity().x) > 0.6f) {
            return "run";
        }
        return "idle";
    }

    public void render(SpriteBatch batch, Texture blockTexture) {
        ghostTrail.render(batch, blockTexture);

        Rectangle bounds = computeCurrentRenderBounds();
        Color prev = batch.getColor();

        if (AssetManager.getInstance().hasPlayerSprites()) {
            Animation<TextureRegion> anim = AssetManager.getInstance().getPlayerAnimation(currentAnimKey);
            if (anim == null) anim = AssetManager.getInstance().getPlayerAnimation("idle");
            TextureRegion frame = anim.getKeyFrame(animTimer);

            // Preserve crisp pixel-art square aspect ratio anchored at player feet
            float spriteSize = bounds.height * 1.18f;
            float drawW = spriteSize * (currentScale.x / Math.max(0.01f, currentScale.y)) * 0.5f + spriteSize * 0.5f;
            float drawH = spriteSize;
            float centerX = bounds.x + bounds.width * 0.5f;
            float drawX = centerX - drawW * 0.5f;
            float drawY = bounds.y - 0.04f;

            batch.setColor(resolveSpriteTint());
            if (player.getFacingDirection() < 0) {
                batch.draw(frame, drawX + drawW, drawY, -drawW, drawH);
            } else {
                batch.draw(frame, drawX, drawY, drawW, drawH);
            }
        } else {
            batch.setColor(resolveBlockColor());
            batch.draw(blockTexture, bounds.x, bounds.y, bounds.width, bounds.height);
        }

        // Visual slash effect when in SwingState
        if (player.getStateMachine().getCurrentState() == player.getSwingState()) {
            Vector2 hitbox = player.getSwingState().getHitboxOffset();
            batch.setColor(1.0f, 0.95f, 0.4f, 0.8f);
            batch.draw(blockTexture, hitbox.x - 0.4f, hitbox.y - 0.4f, 0.8f, 0.8f);
        }

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

    private Color resolveSpriteTint() {
        if (player.getStamina().isLowStamina()) {
            boolean flashOn = ((int) (flashTimer * 14.0f)) % 2 == 0;
            if (flashOn) return new Color(1.0f, 0.45f, 0.45f, 1.0f);
        }
        int dashes = player.getDashes().getCurrentDashes();
        if (dashes == 0) {
            // Subtle cool cyan tint when dash is spent (Celeste readability)
            return new Color(0.62f, 0.88f, 1.0f, 1.0f);
        }
        if (dashes >= 2) {
            return new Color(1.0f, 0.78f, 0.90f, 1.0f);
        }
        return Color.WHITE;
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
