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

    private ParticleSystem particles;
    private float runDustTimer = 0f;
    private float wallDustTimer = 0f;
    private float dashTrailTimer = 0f;

    private float flashTimer = 0f;
    private float animTimer = 0f;
    private String currentAnimKey = "idle";

    public PlayerVisuals(PlayerController player) {
        this.player = player;
        player.setEventListener(this);
        AssetManager.getInstance().loadPlayerSprites();
    }

    public void setParticles(ParticleSystem particles) {
        this.particles = particles;
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

        boolean inDash = player.getStateMachine().getCurrentState() == player.getDashState();
        boolean emitTrail = inDash || Math.abs(player.getVelocity().x) > 24.0f;

        Color trailColor = player.getDashes().getMaxDashes() >= 2 && player.getDashes().getCurrentDashes() >= 1
            ? new Color(1.0f, 0.46f, 0.72f, 0.55f)
            : new Color(0.18f, 0.72f, 1.0f, 0.55f);

        Rectangle bounds = computeCurrentRenderBounds();
        ghostTrail.update(emitTrail, bounds, trailColor, dt);

        updateMovementParticles(dt, inDash, bounds);
    }

    private void updateMovementParticles(float dt, boolean inDash, Rectangle bounds) {
        if (particles == null || dt <= 0f) return;

        Vector2 pos = player.getMotor().getPosition();
        Vector2 vel = player.getVelocity();
        boolean grounded = player.getMotor().isGrounded();

        // Running footstep dust motes
        if (grounded && Math.abs(vel.x) > 4.2f) {
            runDustTimer -= dt;
            if (runDustTimer <= 0f) {
                runDustTimer = 0.11f;
                float dir = Math.signum(vel.x);
                particles.emitDust(
                        pos.x - dir * 0.25f, pos.y + 0.06f,
                        -dir * MathUtils.random(1.0f, 2.6f), MathUtils.random(0.6f, 1.6f),
                        MathUtils.random(0.12f, 0.20f), MathUtils.random(0.22f, 0.36f),
                        new Color(0.68f, 0.86f, 0.76f, 0.65f)
                );
            }
        } else {
            runDustTimer = 0f;
        }

        // Wall-slide / climb friction dust
        boolean inClimb = player.getStateMachine().getCurrentState() == player.getClimbState();
        boolean slidingWall = !grounded && !inClimb && vel.y < -0.5f && player.getInput().getMoveX() != 0
                && player.getMotor().checkWall(player.getInput().getMoveX(), 0.08f);
        if (inClimb && (Math.abs(vel.y) > 0.6f || player.getStamina().isLowStamina()) || slidingWall) {
            wallDustTimer -= dt;
            if (wallDustTimer <= 0f) {
                wallDustTimer = 0.08f;
                int wallDir = inClimb ? player.getFacingDirection() : player.getInput().getMoveX();
                float wx = pos.x + wallDir * (bounds.width * 0.48f);
                float wy = pos.y + bounds.height * MathUtils.random(0.25f, 0.75f);
                Color col = player.getStamina().isLowStamina()
                        ? new Color(1.0f, 0.55f, 0.45f, 0.78f)
                        : new Color(0.55f, 0.82f, 0.66f, 0.65f);
                particles.emitDust(
                        wx, wy,
                        -wallDir * MathUtils.random(0.8f, 2.0f), MathUtils.random(0.4f, 1.4f),
                        MathUtils.random(0.11f, 0.18f), MathUtils.random(0.20f, 0.34f),
                        col
                );
            }
        } else {
            wallDustTimer = 0f;
        }

        // Shimmering dash pixel trail
        if (inDash) {
            dashTrailTimer -= dt;
            if (dashTrailTimer <= 0f) {
                dashTrailTimer = 0.018f;
                boolean twoDash = player.getDashes().getMaxDashes() >= 2;
                Color startCol = twoDash
                        ? new Color(1.0f, 0.52f, 0.80f, 0.95f)
                        : new Color(0.32f, 0.94f, 1.0f, 0.95f);
                Color endCol = new Color(1.0f, 1.0f, 1.0f, 0.0f);
                for (int i = 0; i < 2; i++) {
                    particles.emitSpark(
                            pos.x + MathUtils.random(-0.28f, 0.28f),
                            pos.y + bounds.height * MathUtils.random(0.15f, 0.85f),
                            -vel.x * 0.12f + MathUtils.random(-1.5f, 1.5f),
                            -vel.y * 0.12f + MathUtils.random(-1.5f, 1.5f),
                            MathUtils.random(0.10f, 0.17f),
                            MathUtils.random(0.18f, 0.30f),
                            startCol, endCol, false
                    );
                }
            }
        } else {
            dashTrailTimer = 0f;
        }
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

        if (particles == null) return;
        Vector2 pos = player.getMotor().getPosition();
        boolean boosted = kind == PlayerController.JumpKind.SUPER
                || kind == PlayerController.JumpKind.HYPER
                || kind == PlayerController.JumpKind.WALLBOUNCE;
        int count = boosted ? 10 : 6;
        for (int i = 0; i < count; i++) {
            float vx = MathUtils.random(-3.4f, 3.4f);
            float vy = MathUtils.random(0.4f, 2.4f);
            if (boosted) {
                particles.emitSpark(
                        pos.x + MathUtils.random(-0.25f, 0.25f), pos.y + 0.08f,
                        vx * 1.4f, vy * 1.2f,
                        MathUtils.random(0.11f, 0.18f), MathUtils.random(0.22f, 0.36f),
                        new Color(0.45f, 0.96f, 1.0f, 0.9f),
                        new Color(1.0f, 1.0f, 1.0f, 0.0f),
                        false
                );
            } else {
                particles.emitDust(
                        pos.x + MathUtils.random(-0.25f, 0.25f), pos.y + 0.05f,
                        vx, vy,
                        MathUtils.random(0.14f, 0.24f), MathUtils.random(0.22f, 0.36f),
                        new Color(0.78f, 0.92f, 0.84f, 0.72f)
                );
            }
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

        if (particles == null) return;
        Vector2 pos = player.getMotor().getPosition();
        float cy = pos.y + player.getStats().normalColliderSize.y * 0.5f;
        float baseAngle = MathUtils.atan2(-dir.y, -dir.x);
        boolean twoDash = player.getDashes().getMaxDashes() >= 2;
        Color startCol = twoDash ? new Color(1.0f, 0.52f, 0.80f, 1.0f) : new Color(0.32f, 0.95f, 1.0f, 1.0f);
        Color endCol = new Color(1.0f, 1.0f, 1.0f, 0.0f);

        for (int i = 0; i < 16; i++) {
            float angle = baseAngle + MathUtils.random(-0.85f, 0.85f);
            float spd = MathUtils.random(4.5f, 11.5f);
            particles.emitSpark(
                    pos.x, cy,
                    MathUtils.cos(angle) * spd, MathUtils.sin(angle) * spd,
                    MathUtils.random(0.11f, 0.20f),
                    MathUtils.random(0.20f, 0.38f),
                    startCol, endCol,
                    i % 2 == 0
            );
        }
    }

    @Override
    public void onLanded(float impactSpeed) {
        float t = MathUtils.clamp(impactSpeed / player.getStats().maxFall, 0.0f, 1.0f);
        currentScale.x = MathUtils.lerp(1.10f, 1.36f, t);
        currentScale.y = MathUtils.lerp(0.90f, 0.68f, t);

        if (particles == null) return;
        Vector2 pos = player.getMotor().getPosition();
        int count = Math.round(MathUtils.lerp(4f, 14f, t));
        for (int i = 0; i < count; i++) {
            float side = (i % 2 == 0) ? 1f : -1f;
            float vx = side * MathUtils.random(1.4f, MathUtils.lerp(3.2f, 6.5f, t));
            float vy = MathUtils.random(0.3f, MathUtils.lerp(1.2f, 2.8f, t));
            particles.emitDust(
                    pos.x + side * MathUtils.random(0.05f, 0.32f),
                    pos.y + 0.04f,
                    vx, vy,
                    MathUtils.random(0.14f, MathUtils.lerp(0.22f, 0.32f, t)),
                    MathUtils.random(0.20f, 0.38f),
                    new Color(0.75f, 0.90f, 0.80f, 0.75f)
            );
        }
    }

    @Override
    public void onRespawned() {
        currentScale.set(0.6f, 1.4f);
        ghostTrail.clear();

        if (particles == null) return;
        Vector2 pos = player.getMotor().getPosition();
        float cy = pos.y + player.getStats().normalColliderSize.y * 0.5f;
        for (int i = 0; i < 14; i++) {
            float angle = i * (MathUtils.PI2 / 14f);
            float spd = MathUtils.random(2.4f, 5.2f);
            particles.emitSpark(
                    pos.x, cy,
                    MathUtils.cos(angle) * spd, MathUtils.sin(angle) * spd,
                    MathUtils.random(0.14f, 0.24f),
                    MathUtils.random(0.28f, 0.45f),
                    new Color(0.45f, 0.98f, 0.90f, 0.95f),
                    new Color(1.0f, 1.0f, 1.0f, 0.0f),
                    false
            );
        }
    }
}
