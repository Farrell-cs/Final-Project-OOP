package com.Ruinfall.frontend.objects.Interactables;

import com.badlogic.gdx.graphics.Color;
import com.Ruinfall.frontend.objects.items.Item;
import com.Ruinfall.frontend.objects.items.ItemType;
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

    // ------------------------------------------------------------------ //
    //  Constructor
    // ------------------------------------------------------------------ //

    public Barrels(float x, float y) {
        super(x, y, 28f, 32f, 3,
              ItemType.BOMB,
              new Color(0.5f, 0.3f, 0.1f, 1f));  // brown
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
