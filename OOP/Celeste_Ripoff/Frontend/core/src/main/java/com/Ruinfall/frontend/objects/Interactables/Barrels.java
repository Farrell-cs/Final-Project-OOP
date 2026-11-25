package com.Ruinfall.frontend.objects.Interactables;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.Ruinfall.frontend.objects.Collidable;
import com.Ruinfall.frontend.objects.bullets.Bullet;
import com.Ruinfall.frontend.objects.enemies.WormBoss;
import com.Ruinfall.frontend.objects.enemies.WormSegment;
import com.Ruinfall.frontend.objects.items.Item;
import com.Ruinfall.frontend.objects.items.ItemType;
import com.Ruinfall.frontend.systems.BulletManager;
import com.Ruinfall.frontend.systems.GameEventBus;
import com.Ruinfall.frontend.systems.ItemFactory;

import java.util.List;

/**
 * Barrel — a heavy breakable container that explodes with extra force.
 *
 * <p>Extends {@link Rock} with:
 * <ul>
 *   <li>Higher HP (3 hits)</li>
 *   <li>Brown colouring</li>
 *   <li>Drops 2 items on break</li>
 *   <li>Publishes {@link GameEventBus.GameEvent#BARREL_EXPLODED} on destruction</li>
 * </ul>
 */
public class Barrels extends Rock {
    private final float initialX;
    private final float initialY;
    private final int maxHealth = 3;
    private float explosionRadius = 4.5f;
    private int explosionDamage = 30;
    private boolean detonated = false;

    // ------------------------------------------------------------------ //
    //  Constructor
    // ------------------------------------------------------------------ //

    public Barrels(float x, float y) {
        super(x, y, 1.4f, 1.6f, 3,
              ItemType.BOMB,
              new Color(0.5f, 0.3f, 0.1f, 1f));  // brown
        this.initialX = x;
        this.initialY = y;
    }

    public void reset() {
        this.x = initialX;
        this.y = initialY;
        this.health = maxHealth;
        this.active = true;
        this.detonated = false;
        this.knockbackVx = 0f;
        this.knockbackVy = 0f;
        this.drops.clear();
    }

    @Override
    public void onCollision(Collidable other) {
        if (!active) return;

        if (other instanceof WormBoss || other instanceof WormSegment) {
            takeHit(health);
            return;
        }
        super.onCollision(other);
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

        // First drop: always BOMB
        Item bomb = ItemFactory.createItem(x, y, ItemType.BOMB);
        drops.add(bomb);

        // Second drop: random between LIFE and POINT
        ItemType second = (Math.random() < 0.5) ? ItemType.LIFE : ItemType.POINT;
        Item secondItem = ItemFactory.createItem(x + 8f, y, second);
        drops.add(secondItem);

        // Notify: barrel exploded (visual effect hook)
        GameEventBus.getInstance().publish(
                GameEventBus.GameEvent.BARREL_EXPLODED,
                new BarrelExplosionPayload(drops, x, y));

        System.out.println("[Barrel] EXPLODED — dropped BOMB + " + second
                + " at (" + x + ", " + y + ")");
    }

    // ------------------------------------------------------------------ //
    //  Payload
    // ------------------------------------------------------------------ //

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
