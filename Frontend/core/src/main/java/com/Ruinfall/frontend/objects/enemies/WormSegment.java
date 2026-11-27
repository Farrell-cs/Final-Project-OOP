package com.Ruinfall.frontend.objects.enemies;

import com.Ruinfall.frontend.objects.Collidable;
import com.Ruinfall.frontend.objects.GameObject;
import com.Ruinfall.frontend.objects.Player;
import com.badlogic.gdx.graphics.Color;

/**
 * Trailing body segment of a WormBoss.
 * Shares the boss's HP pool by delegating all incoming damage to the owner.
 */
public class WormSegment extends GameObject {
    private final WormBoss owner;
    private final int segmentIndex;
    private float rotationDeg;

    public WormSegment(WormBoss owner, int segmentIndex, float x, float y, float width, float height, Color color) {
        super(x, y, width, height, 0f, color);
        this.owner = owner;
        this.segmentIndex = segmentIndex;
    }

    public boolean takeDamage(int damage) {
        if (owner == null || !active) {
            return false;
        }
        return owner.takeDamage(damage);
    }

    @Override
    public void onCollision(Collidable other) {
        if (!active || owner == null) {
            return;
        }
        if (other instanceof Player player) {
            owner.onSegmentCollision(this, player);
        }
    }

    public WormBoss getOwner() {
        return owner;
    }

    public int getSegmentIndex() {
        return segmentIndex;
    }

    public float getRotationDeg() {
        return rotationDeg;
    }

    public void setRotationDeg(float rotationDeg) {
        this.rotationDeg = rotationDeg;
    }

    public void setCenter(float cx, float cy) {
        this.x = cx - width * 0.5f;
        this.y = cy - height * 0.5f;
    }

    public float getCenterX() {
        return x + width * 0.5f;
    }

    public float getCenterY() {
        return y + height * 0.5f;
    }
}
