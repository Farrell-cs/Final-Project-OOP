package com.Ruinfall.frontend.objects.enemies;

import com.Ruinfall.frontend.systems.GameEventBus;
import com.Ruinfall.frontend.world.LushCaveStage;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Wave Mantis: Stage 1 (Lush Cave) Boss.
 * Uses ground shockwaves and a high-speed dash.
 * Can be dazed by either parrying its dash with a melee swing or luring it into an arena wall,
 * then heavily damaged by dropping ceiling stalactites onto it.
 */
public class WaveMantis extends Boss {

    public enum State {
        SLEEPING,
        STALK,
        SHOCKWAVE_WINDUP,
        SHOCKWAVE_RECOVER,
        DASH_WINDUP,
        DASH_ATTACK,
        DAZED,
        DEFEATED
    }

    public static class Shockwave {
        public float x, y, width, height, vx, life;
        public boolean active = true;

        public Shockwave(float x, float y, float vx) {
            this.x = x;
            this.y = y;
            this.width = 1.4f;
            this.height = 1.2f;
            this.vx = vx;
            this.life = 2.6f;
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }
    }

    private final float initialX;
    private final float initialY;

    private State state = State.SLEEPING;
    private float stateTimer = 0f;
    private int facing = -1; // -1 left, +1 right
    private float dashSpeed = 24.0f;
    private float stalkSpeed = 4.2f;
    private float hitFlashTimer = 0f;
    private float swingHurtCooldown = 0f;
    private int attackToggle = 0;

    private final List<Shockwave> shockwaves = new ArrayList<>();

    private final Color bodyNormal = new Color(0.18f, 0.72f, 0.56f, 1.0f);
    private final Color bodyWindupShock = new Color(0.25f, 0.92f, 0.98f, 1.0f);
    private final Color bodyWindupDash = new Color(1.0f, 0.84f, 0.25f, 1.0f);
    private final Color bodyDash = new Color(1.0f, 0.45f, 0.20f, 1.0f);
    private final Color bodyDazed = new Color(0.75f, 0.55f, 0.95f, 1.0f);
    private final Color shockwaveColor = new Color(0.32f, 0.95f, 0.85f, 0.95f);

    public WaveMantis(float x, float y) {
        super(x, y, "Wave Mantis", 100);
        this.initialX = x;
        this.initialY = y;
        this.width = 2.1f;
        this.height = 2.7f;
        this.color = bodyNormal;
    }

    public void reset() {
        this.x = initialX;
        this.y = initialY;
        this.hp = maxHP;
        this.active = true;
        this.state = State.SLEEPING;
        this.stateTimer = 0f;
        this.facing = -1;
        this.attackToggle = 0;
        this.hitFlashTimer = 0f;
        this.swingHurtCooldown = 0f;
        this.shockwaves.clear();
        this.color = bodyNormal;
    }

    public void activate() {
        if (state == State.SLEEPING) {
            state = State.STALK;
            stateTimer = 1.4f;
        }
    }

    public void updateBoss(float dt, float playerX, float playerY) {
        if (state == State.DEFEATED || !isAlive()) {
            updateShockwaves(dt);
            return;
        }

        if (hitFlashTimer > 0f) hitFlashTimer -= dt;
        if (swingHurtCooldown > 0f) swingHurtCooldown -= dt;

        updateShockwaves(dt);

        if (state == State.SLEEPING) {
            return;
        }

        stateTimer -= dt;
        float centerX = x + width * 0.5f;

        switch (state) {
            case STALK -> {
                facing = (playerX >= centerX) ? 1 : -1;
                x += facing * stalkSpeed * dt;
                clampToArena();
                if (stateTimer <= 0f) {
                    attackToggle++;
                    if (attackToggle % 2 == 1) {
                        state = State.DASH_WINDUP;
                        stateTimer = 0.65f;
                    } else {
                        state = State.SHOCKWAVE_WINDUP;
                        stateTimer = 0.75f;
                    }
                }
            }
            case SHOCKWAVE_WINDUP -> {
                facing = (playerX >= centerX) ? 1 : -1;
                if (stateTimer <= 0f) {
                    // Spawn dual ground shockwaves
                    shockwaves.add(new Shockwave(centerX - 0.7f, 0.0f, -13.5f));
                    shockwaves.add(new Shockwave(centerX - 0.7f, 0.0f, 13.5f));
                    GameEventBus.getInstance().publish(GameEventBus.GameEvent.BOSS_SHOCKWAVE, this);
                    state = State.SHOCKWAVE_RECOVER;
                    stateTimer = 0.9f;
                }
            }
            case SHOCKWAVE_RECOVER -> {
                if (stateTimer <= 0f) {
                    state = State.STALK;
                    stateTimer = 1.2f;
                }
            }
            case DASH_WINDUP -> {
                facing = (playerX >= centerX) ? 1 : -1;
                if (stateTimer <= 0f) {
                    state = State.DASH_ATTACK;
                    stateTimer = 0.95f;
                }
            }
            case DASH_ATTACK -> {
                x += facing * dashSpeed * dt;
                // Check wall crash -> dazes Wave Mantis!
                if (x <= LushCaveStage.ARENA_MIN_X + 0.2f || x + width >= LushCaveStage.ARENA_MAX_X - 0.2f) {
                    clampToArena();
                    enterDazed(3.5f);
                } else if (stateTimer <= 0f) {
                    state = State.STALK;
                    stateTimer = 1.0f;
                }
            }
            case DAZED -> {
                if (stateTimer <= 0f) {
                    state = State.STALK;
                    stateTimer = 1.1f;
                }
            }
            default -> {}
        }
    }

    private void updateShockwaves(float dt) {
        Iterator<Shockwave> it = shockwaves.iterator();
        while (it.hasNext()) {
            Shockwave sw = it.next();
            sw.x += sw.vx * dt;
            sw.life -= dt;
            if (sw.life <= 0f || sw.x < LushCaveStage.ARENA_MIN_X - 2f || sw.x > LushCaveStage.ARENA_MAX_X + 2f) {
                sw.active = false;
                it.remove();
            }
        }
    }

    private void clampToArena() {
        x = MathUtils.clamp(x, LushCaveStage.ARENA_MIN_X + 0.1f, LushCaveStage.ARENA_MAX_X - width - 0.1f);
    }

    private void enterDazed(float duration) {
        state = State.DAZED;
        stateTimer = duration;
        GameEventBus.getInstance().publish(GameEventBus.GameEvent.BOSS_DAZED, this);
    }

    /**
     * Attempts to parry Wave Mantis's dash with the player's melee swing.
     * @return true if a parry occurred.
     */
    public boolean tryParry(Rectangle swingRect) {
        if (state == State.DASH_ATTACK && swingRect != null && swingRect.overlaps(getCoreHitbox())) {
            hitFlashTimer = 0.25f;
            enterDazed(3.8f);
            GameEventBus.getInstance().publish(GameEventBus.GameEvent.BOSS_PARRIED, this);
            return true;
        }
        return false;
    }

    /**
     * Called when a falling stalactite strikes Wave Mantis.
     */
    public boolean onStalactiteHit() {
        if (state == State.DEFEATED || !isAlive()) return false;
        int dmg = (state == State.DAZED) ? 35 : 12;
        hitFlashTimer = 0.22f;
        boolean defeated = takeDamage(dmg);
        if (defeated) {
            onDefeated();
        }
        return true;
    }

    /**
     * Called when the player's sword swing hits Wave Mantis while dazed.
     */
    public boolean onPlayerSwingHit() {
        if (state != State.DAZED || swingHurtCooldown > 0f || !isAlive()) return false;
        swingHurtCooldown = 0.28f;
        hitFlashTimer = 0.12f;
        boolean defeated = takeDamage(8);
        if (defeated) {
            onDefeated();
        }
        return true;
    }

    private void onDefeated() {
        state = State.DEFEATED;
        shockwaves.clear();
        GameEventBus.getInstance().publish(GameEventBus.GameEvent.BOSS_DEFEATED, this);
    }

    public State getState() { return state; }
    public List<Shockwave> getShockwaves() { return shockwaves; }
    public float getCenterX() { return x + width * 0.5f; }
    public float getCenterY() { return y + height * 0.5f; }

    public void render(SpriteBatch batch, Texture blockTexture) {
        // Render active shockwaves
        batch.setColor(shockwaveColor);
        for (Shockwave sw : shockwaves) {
            batch.draw(blockTexture, sw.x, sw.y, sw.width, sw.height);
            batch.setColor(1.0f, 1.0f, 1.0f, 0.85f);
            batch.draw(blockTexture, sw.x + 0.2f, sw.y + 0.2f, sw.width - 0.4f, sw.height * 0.45f);
            batch.setColor(shockwaveColor);
        }

        if (state == State.DEFEATED || !isAlive()) {
            batch.setColor(Color.WHITE);
            return;
        }

        // Determine body color based on state & hit flash
        Color drawColor = bodyNormal;
        if (hitFlashTimer > 0f) {
            drawColor = Color.WHITE;
        } else if (state == State.SHOCKWAVE_WINDUP) {
            drawColor = bodyWindupShock;
        } else if (state == State.DASH_WINDUP) {
            // Rapid amber-white telegraph pulse for parry timing
            drawColor = ((int) (stateTimer * 20f) % 2 == 0) ? Color.WHITE : bodyWindupDash;
        } else if (state == State.DASH_ATTACK) {
            drawColor = bodyDash;
        } else if (state == State.DAZED) {
            drawColor = bodyDazed;
        }

        // Main torso
        batch.setColor(drawColor);
        batch.draw(blockTexture, x, y, width, height);

        // Mantis scythe-arms accent
        batch.setColor(0.90f, 0.98f, 0.92f, 0.9f);
        float armX = (facing > 0) ? (x + width - 0.1f) : (x - 0.7f);
        batch.draw(blockTexture, armX, y + height * 0.45f, 0.8f, 0.35f);

        // Dazed indicator bars above head
        if (state == State.DAZED) {
            batch.setColor(1.0f, 0.95f, 0.35f, 0.95f);
            float wobble = MathUtils.sin(stateTimer * 12f) * 0.45f;
            batch.draw(blockTexture, getCenterX() + wobble - 0.2f, y + height + 0.35f, 0.4f, 0.22f);
        }

        // Boss HP bar above head (when awake)
        if (state != State.SLEEPING) {
            float barW = 2.6f;
            float barH = 0.22f;
            float barX = getCenterX() - barW * 0.5f;
            float barY = y + height + 0.75f;
            float ratio = MathUtils.clamp((float) hp / (float) maxHP, 0f, 1f);

            batch.setColor(0.1f, 0.1f, 0.14f, 0.85f);
            batch.draw(blockTexture, barX, barY, barW, barH);
            batch.setColor(0.95f, 0.25f, 0.32f, 0.95f);
            batch.draw(blockTexture, barX + 0.04f, barY + 0.04f, (barW - 0.08f) * ratio, barH - 0.08f);
        }

        batch.setColor(Color.WHITE);
    }
}
