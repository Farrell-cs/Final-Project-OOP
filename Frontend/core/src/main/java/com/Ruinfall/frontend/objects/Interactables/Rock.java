package com.Ruinfall.frontend.objects.Interactables;

import com.badlogic.gdx.graphics.Color;
import com.Ruinfall.frontend.PlayerController;
import com.Ruinfall.frontend.objects.Collidable;
import com.Ruinfall.frontend.objects.GameObject;
import com.Ruinfall.frontend.objects.bullets.Bullet;
import com.Ruinfall.frontend.objects.items.Item;
import com.Ruinfall.frontend.objects.items.ItemType;
import com.Ruinfall.frontend.objects.patterns.BreakBehavior;
import com.Ruinfall.frontend.objects.patterns.StandardBreakBehavior;
import com.Ruinfall.frontend.states.SwingState;
import com.Ruinfall.frontend.systems.GameEventBus;
import com.Ruinfall.frontend.systems.ItemFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Rock: Breakable container that reacts to directional player swings or projectiles,
 * applying knockback and dropping items upon destruction.
 */
public class Rock extends GameObject {
    protected int health = 2;
    protected ItemType dropType;
    protected BreakBehavior breakBehavior;
    protected float knockbackVx = 0f;
    protected float knockbackVy = 0f;
    protected final List<Item> drops = new ArrayList<>();

    public Rock() {
        this(0, 0);
    }

    public Rock(float x, float y) {
        this(x, y, ItemType.LIFE);
    }

    public Rock(float x, float y, ItemType dropType) {
        this(x, y, 24f, 24f, 2, dropType, new Color(0.45f, 0.45f, 0.45f, 1f));
    }

    public Rock(float x, float y, float width, float height, int health, ItemType dropType, Color color) {
        super(x, y, width, height, 0f, color);
        this.health = health;
        this.dropType = dropType;
        this.breakBehavior = new StandardBreakBehavior();
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        if (!active) return;

        // Apply knockback with friction decay
        if (Math.abs(knockbackVx) > 0.1f || Math.abs(knockbackVy) > 0.1f) {
            x += knockbackVx * delta;
            y += knockbackVy * delta;

            knockbackVx *= (float) Math.pow(0.1, delta);
            knockbackVy *= (float) Math.pow(0.1, delta);
        }
    }

    public void onSwingHit(SwingState.SwingDirection direction, int facingDir) {
        if (!active) return;

        float knockForce = 150f;
        switch (direction) {
            case FRONT -> knockbackVx = facingDir * knockForce;
            case UP -> knockbackVy = knockForce;
            case DOWN -> knockbackVy = -knockForce;
            case DIAGONAL_UP -> {
                knockbackVx = facingDir * (knockForce * 0.7f);
                knockbackVy = knockForce * 0.7f;
            }
            case DIAGONAL_DOWN -> {
                knockbackVx = facingDir * (knockForce * 0.7f);
                knockbackVy = -knockForce * 0.7f;
            }
        }

        takeHit(1);
    }

    public void takeHit(int damage) {
        health -= damage;
        if (breakBehavior != null) {
            breakBehavior.onHit(this, damage);
        }

        if (health <= 0) {
            if (breakBehavior != null) {
                breakBehavior.onDestroy(this);
            }
            spawnDrops();
            destroy();
        }
    }

    protected void spawnDrops() {
        drops.clear();
        Item item = ItemFactory.createItem(x, y, dropType != null ? dropType : ItemType.LIFE);
        drops.add(item);
        GameEventBus.getInstance().publish(GameEventBus.GameEvent.ROCK_BROKEN, this);
        System.out.println("[Rock] Destroyed and dropped " + (dropType != null ? dropType : ItemType.LIFE));
    }

    public boolean isPogoable() {
        return true;
    }

    @Override
    public void onCollision(Collidable other) {
        if (!active) return;

        if (other instanceof Bullet bullet) {
            takeHit(bullet.getDamage());
            bullet.destroy();
        } else if (other instanceof PlayerController player) {
            // Check if player is swinging down to bounce
            SwingState swingState = player.getSwingState();
            if (swingState != null && player.getStateMachine().getCurrentState() == swingState) {
                onSwingHit(swingState.getCurrentDirection(), player.getFacingDirection());
                swingState.onSwingHit(isPogoable());
            }
        }
    }

    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = health; }
    public BreakBehavior getBreakBehavior() { return breakBehavior; }
    public void setBreakBehavior(BreakBehavior breakBehavior) { this.breakBehavior = breakBehavior; }
    public List<Item> getDrops() { return drops; }
}
