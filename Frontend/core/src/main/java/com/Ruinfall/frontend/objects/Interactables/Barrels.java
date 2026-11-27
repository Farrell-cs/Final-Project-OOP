package com.Ruinfall.frontend.objects.Interactables;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.Ruinfall.frontend.objects.Collidable;
import com.Ruinfall.frontend.objects.bullets.Bullet;
import com.Ruinfall.frontend.objects.enemies.WormBoss;
import com.Ruinfall.frontend.objects.enemies.WormSegment;
import com.Ruinfall.frontend.objects.items.Item;
import com.Ruinfall.frontend.objects.items.ItemType;
import com.Ruinfall.frontend.states.SwingState;
import com.Ruinfall.frontend.systems.BulletManager;
import com.Ruinfall.frontend.systems.GameEventBus;
import com.Ruinfall.frontend.systems.ItemFactory;

import java.util.List;

/**
 * Barrel — a heavy stationary explosive container anchored to its floor or platform slot.
 *
 * <p>Extends {@link Rock} with:
 * <ul>
 *   <li>Anchored drop-in-place physics (zero bounce, slide, or knockback drift)</li>
 *   <li>Higher durability (3 hits from player slashes, instant detonation on heavy impact)</li>
 *   <li>Visual crack states, hit-flash, and wood/metal spark feedback on non-lethal hits</li>
 *   <li>Drops 2 items cleanly in place on break</li>
 *   <li>Publishes {@link GameEventBus.GameEvent#BARREL_HIT} and {@link GameEventBus.GameEvent#BARREL_EXPLODED}</li>
 * </ul>
 */
public class Barrels extends Rock {
    private static final float HIT_FLASH_DURATION = 0.14f;
    private static final float HIT_SHAKE_DURATION = 0.10f;

    private final float initialX;
    private final float initialY;
    private final int maxHealth = 3;
    private float explosionRadius = 4.5f;
    private int explosionDamage = 30;
    private boolean detonated = false;

    private float hitFlashTimer = 0f;
    private float hitShakeTimer = 0f;

    // ------------------------------------------------------------------ //
    //  Constructor
    // ------------------------------------------------------------------ //

    public Barrels(float x, float y) {
        super(x, y, 1.4f, 1.6f, 3,
              ItemType.BOMB,
              new Color(0.52f, 0.31f, 0.13f, 1f));  // warm wood brown
        this.initialX = x;
        this.initialY = y;
        this.knockbackVx = 0f;
        this.knockbackVy = 0f;
    }

    public void reset() {
        this.x = initialX;
        this.y = initialY;
        this.health = maxHealth;
        this.active = true;
        this.detonated = false;
        this.knockbackVx = 0f;
        this.knockbackVy = 0f;
        this.hitFlashTimer = 0f;
        this.hitShakeTimer = 0f;
        this.drops.clear();
    }

    @Override
    public void update(float delta) {
        stateTime += delta;
        if (!active) return;

        // Keep barrel strictly anchored in place — no sliding, bouncing, or horizontal drift
        this.x = initialX;
        this.y = initialY;
        this.knockbackVx = 0f;
        this.knockbackVy = 0f;

        if (hitFlashTimer > 0f) {
            hitFlashTimer = Math.max(0f, hitFlashTimer - delta);
        }
        if (hitShakeTimer > 0f) {
            hitShakeTimer = Math.max(0f, hitShakeTimer - delta);
        }
    }

    @Override
    public void onSwingHit(SwingState.SwingDirection direction, int facingDir) {
        if (!active) return;
        // Barrels stay anchored in place and do not slide or bounce on player slashes
        this.knockbackVx = 0f;
        this.knockbackVy = 0f;
        takeHit(1);
    }

    /**
     * Heavy impact (falling stalactite or boss collision) detonates the barrel immediately.
     */
    public void onHeavyImpact() {
        if (!active) return;
        takeHit(Math.max(1, health));
    }

    @Override
    public void takeHit(int damage) {
        if (!active || damage <= 0) return;

        int previousHp = health;
        super.takeHit(damage);

        // Non-lethal hit: trigger hit-flash, micro-shake, and spark/splinter burst
        if (active && health > 0 && health < previousHp) {
            hitFlashTimer = HIT_FLASH_DURATION;
            hitShakeTimer = HIT_SHAKE_DURATION;
            GameEventBus.getInstance().publish(
                GameEventBus.GameEvent.BARREL_HIT,
                new BarrelHitPayload(x + width * 0.5f, y + height * 0.5f, health, maxHealth)
            );
        }
    }

    @Override
    public void onCollision(Collidable other) {
        if (!active) return;

        if (other instanceof WormBoss || other instanceof WormSegment || other instanceof Stalactite) {
            onHeavyImpact();
            return;
        }
        super.onCollision(other);
    }

    /**
     * Returns visual crack stage:
     * 0 = intact (full HP), 1 = lightly cracked, 2 = heavily cracked / critical (1 hit left).
     */
    public int getDamageStage() {
        if (health >= maxHealth) return 0;
        if (health == 1) return 2;
        return 1;
    }

    public float getHitFlashTimer() {
        return hitFlashTimer;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public void render(SpriteBatch batch, Texture blockTexture) {
        if (batch == null || blockTexture == null || !active) return;

        float shakeX = hitShakeTimer > 0f ? MathUtils.sin(hitShakeTimer * 160f) * 0.06f : 0f;
        float drawX = x + shakeX;
        float drawY = y;
        int damageStage = getDamageStage();

        // 1. Base barrel body (warms slightly when heavily cracked, flashes bright white/gold on hit)
        if (hitFlashTimer > 0f) {
            batch.setColor(1.0f, 0.94f, 0.72f, 1.0f);
        } else if (damageStage == 2) {
            float emberPulse = 0.5f + 0.5f * MathUtils.sin(stateTime * 12.0f);
            batch.setColor(0.62f + 0.14f * emberPulse, 0.28f + 0.06f * emberPulse, 0.12f, 1.0f);
        } else if (damageStage == 1) {
            batch.setColor(0.55f, 0.30f, 0.13f, 1.0f);
        } else {
            batch.setColor(color);
        }
        batch.draw(blockTexture, drawX, drawY, width, height);

        // 2. Iron reinforcement bands (top & bottom hoops)
        Color bandColor = hitFlashTimer > 0f
            ? new Color(1.0f, 0.96f, 0.85f, 1.0f)
            : new Color(0.22f, 0.24f, 0.28f, 1.0f);
        batch.setColor(bandColor);
        float bandH = height * 0.11f;
        batch.draw(blockTexture, drawX - 0.03f, drawY + height * 0.16f, width + 0.06f, bandH);
        batch.draw(blockTexture, drawX - 0.03f, drawY + height * 0.73f, width + 0.06f, bandH);

        // 3. Visual crack overlays based on damage stage
        if (damageStage >= 1) {
            Color crackColor = (damageStage == 2)
                ? new Color(1.0f, 0.52f, 0.12f, 0.95f)  // glowing ember cracks at 1 HP
                : new Color(0.14f, 0.08f, 0.04f, 0.92f); // dark split wood cracks at 2 HP
            batch.setColor(crackColor);

            // Primary diagonal crack across center
            drawCrackLine(batch, blockTexture, drawX + width * 0.25f, drawY + height * 0.72f, width * 0.48f, 0.07f, -38f);
            drawCrackLine(batch, blockTexture, drawX + width * 0.52f, drawY + height * 0.45f, width * 0.34f, 0.06f, 28f);

            if (damageStage >= 2) {
                // Secondary deep fracture lines + hot inner seam
                drawCrackLine(batch, blockTexture, drawX + width * 0.22f, drawY + height * 0.30f, width * 0.54f, 0.075f, 34f);
                drawCrackLine(batch, blockTexture, drawX + width * 0.44f, drawY + height * 0.58f, width * 0.38f, 0.065f, -62f);

                float glow = 0.55f + 0.45f * MathUtils.sin(stateTime * 14.0f);
                batch.setColor(1.0f, 0.85f, 0.30f, 0.75f * glow);
                drawCrackLine(batch, blockTexture, drawX + width * 0.34f, drawY + height * 0.56f, width * 0.36f, 0.045f, -38f);
            }
        }

        batch.setColor(Color.WHITE);
    }

    private static void drawCrackLine(
        SpriteBatch batch,
        Texture tex,
        float startX,
        float startY,
        float length,
        float thickness,
        float angleDeg
    ) {
        batch.draw(
            tex,
            startX,
            startY,
            0f,
            thickness * 0.5f,
            length,
            thickness,
            1f, 1f,
            angleDeg,
            0, 0,
            tex.getWidth(),
            tex.getHeight(),
            false, false
        );
    }

    /**
     * Applies AoE damage to nearby WormBoss hitboxes and knocks back nearby enemy projectiles.
     */
    public void detonate(WormBoss boss, BulletManager bulletManager) {
        if (detonated) {
            return;
        }
        detonated = true;

        float cx = x + width * 0.5f;
        float cy = y + height * 0.5f;
        float radiusSq = explosionRadius * explosionRadius;

        if (boss != null && boss.isActive() && boss.isAlive()) {
            for (Rectangle box : boss.getActiveHitboxes()) {
                float bx = box.x + box.width * 0.5f;
                float by = box.y + box.height * 0.5f;
                float dx = bx - cx;
                float dy = by - cy;
                if (dx * dx + dy * dy <= radiusSq) {
                    boss.takeDamage(explosionDamage);
                    break;
                }
            }
        }

        if (bulletManager != null) {
            for (Bullet b : bulletManager.getActiveBullets()) {
                if (b.isDestroyed() || b.getIsPlayerBullet()) continue;
                float bx = b.getX() + b.getWidth() * 0.5f;
                float by = b.getY() + b.getHeight() * 0.5f;
                float dx = bx - cx;
                float dy = by - cy;
                float distSq = dx * dx + dy * dy;
                if (distSq <= radiusSq) {
                    float dist = (float) Math.sqrt(Math.max(0.0001f, distSq));
                    float speed = Math.max(12.0f, (float) Math.hypot(b.getVx(), b.getVy()) * 1.4f);
                    b.setVx((dx / dist) * speed);
                    b.setVy((dy / dist) * speed);
                }
            }
        }
    }

    public boolean hasDetonated() {
        return detonated;
    }

    public float getExplosionRadius() {
        return explosionRadius;
    }

    public int getExplosionDamage() {
        return explosionDamage;
    }

    // ------------------------------------------------------------------ //
    //  Drops — 2 items
    // ------------------------------------------------------------------ //

    @Override
    protected void spawnDrops() {
        drops.clear();

        // Spawn drops cleanly in place at the barrel's anchor coordinates
        float dropY = initialY + 0.25f;
        Item bomb = ItemFactory.createItem(initialX + width * 0.12f, dropY, ItemType.BOMB);
        drops.add(bomb);

        // Second drop: random between LIFE and POINT
        ItemType second = (Math.random() < 0.5) ? ItemType.LIFE : ItemType.POINT;
        Item secondItem = ItemFactory.createItem(initialX + width * 0.55f, dropY, second);
        drops.add(secondItem);

        // Notify: barrel exploded (visual effect hook)
        GameEventBus.getInstance().publish(
                GameEventBus.GameEvent.BARREL_EXPLODED,
                new BarrelExplosionPayload(drops, initialX, initialY));

        System.out.println("[Barrel] EXPLODED — dropped BOMB + " + second
                + " at (" + initialX + ", " + initialY + ")");
    }

    // ------------------------------------------------------------------ //
    //  Payloads
    // ------------------------------------------------------------------ //

    public static class BarrelHitPayload {
        public final float x, y;
        public final int remainingHp;
        public final int maxHp;

        public BarrelHitPayload(float x, float y, int remainingHp, int maxHp) {
            this.x = x;
            this.y = y;
            this.remainingHp = remainingHp;
            this.maxHp = maxHp;
        }
    }

    public static class BarrelExplosionPayload {
        public final List<Item> items;
        public final float x, y;

        public BarrelExplosionPayload(List<Item> items, float x, float y) {
            this.items = items;
            this.x     = x;
            this.y     = y;
        }
    }
}
