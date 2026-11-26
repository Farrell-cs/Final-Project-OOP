package com.Ruinfall.frontend.objects.items;

import com.badlogic.gdx.graphics.Color;
import com.Ruinfall.frontend.PlayerController;
import com.Ruinfall.frontend.objects.Collidable;
import com.Ruinfall.frontend.objects.GameObject;
import com.Ruinfall.frontend.systems.GameEventBus;

/**
 * Item: Represents collectible floating items dropped by destructibles or enemies
 * that grant upgrades/resources on pickup.
 */
public class Item extends GameObject {
    private String itemType;
    private long scoreValue;
    private ItemType itemTypeEnum;
    private boolean collected = false;
    private float floatTimer = 0f;
    private float initialY;

    public Item(float x, float y, String itemType) {
        super(x, y, 0.6f, 0.6f, 20f, Color.YELLOW);
        this.itemType = itemType;
        this.initialY = y;
        try {
            this.itemTypeEnum = ItemType.valueOf(itemType);
            this.scoreValue = itemTypeEnum.getScoreValue();
        } catch (Exception ignored) {}
    }

    public Item(float x, float y, float width, float height, float speed, String itemType) {
        super(x, y, width, height, speed, Color.YELLOW);
        this.itemType = itemType;
        this.initialY = y;
        try {
            this.itemTypeEnum = ItemType.valueOf(itemType);
            this.scoreValue = itemTypeEnum.getScoreValue();
        } catch (Exception ignored) {}
    }

    public Item(float x, float y, float width, float height, float speed, String itemType, long scoreValue) {
        super(x, y, width, height, speed, Color.YELLOW);
        this.itemType = itemType;
        this.scoreValue = scoreValue;
        this.initialY = y;
        try {
            this.itemTypeEnum = ItemType.valueOf(itemType);
        } catch (Exception ignored) {}
    }

    public Item(float x, float y, ItemType itemTypeEnum) {
        super(x, y, 0.6f, 0.6f, 20f, Color.YELLOW);
        this.itemTypeEnum = itemTypeEnum;
        this.scoreValue = itemTypeEnum.getScoreValue();
        this.itemType = itemTypeEnum.name();
        this.initialY = y;
    }

    public Item(float x, float y, float width, float height, float speed, ItemType itemTypeEnum, long scoreValue) {
        super(x, y, width, height, speed, Color.YELLOW);
        this.itemTypeEnum = itemTypeEnum;
        this.scoreValue = scoreValue;
        this.itemType = itemTypeEnum.name();
        this.initialY = y;
    }

    @Override
    public void update(float delta) {
        super.update(delta);
        floatTimer += delta * 3f;
        // Subtle floating bob motion
        this.y = initialY + (float) Math.sin(floatTimer) * 0.15f;
    }

    public String getItemType() {
        return itemType;
    }

    public ItemType getItemTypeEnum() {
        return itemTypeEnum;
    }

    public long getScoreValue() {
        return scoreValue;
    }

    public boolean isCollected() {
        return collected;
    }

    public void applyBonus(PlayerController player) {
        if (itemTypeEnum != null) {
            switch (itemTypeEnum) {
                case BOMB -> System.out.println("[Item] Collected BOMB! +1 Bomb resource");
                case LIFE -> {
                    if (player != null) {
                        player.heal(1);
                    }
                    System.out.println("[Item] Collected LIFE! +1 HP");
                }
                case SHIELD -> System.out.println("[Item] Collected SHIELD! Shield barrier active");
                case MANTIS_CLAW -> System.out.println("[Item] Collected MANTIS CLAW! Wall-reach upgraded");
                case POWER -> System.out.println("[Item] Collected POWER! +Power level");
                case POINT -> System.out.println("[Item] Collected POINT! +" + scoreValue + " score");
            }
        }
    }

    @Override
    public void onCollision(Collidable other) {
        if (!active || collected) return;

        if (other instanceof PlayerController player) {
            collectItem(this, player);
        }
    }

    public void collectItem(Item item, PlayerController player) {
        if (item.isDestroyed() || collected) return;
        collected = true;
        applyBonus(player);
        GameEventBus.getInstance().publish(GameEventBus.GameEvent.ITEM_COLLECTED, this);
        destroy();
    }

    public void collectItem(Item item) {
        if (item.isDestroyed() || collected) return;
        collected = true;
        GameEventBus.getInstance().publish(GameEventBus.GameEvent.ITEM_COLLECTED, this);
        destroy();
    }
}
