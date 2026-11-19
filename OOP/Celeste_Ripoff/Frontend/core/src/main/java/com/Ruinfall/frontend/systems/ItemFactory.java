package com.Ruinfall.frontend.systems;

import com.Ruinfall.frontend.objects.items.Item;
import com.Ruinfall.frontend.objects.items.ItemType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Factory pattern — creates {@link Item} instances.
 *
 * <p>All item construction should go through this class so that
 * construction logic is centralised and easy to modify or extend.
 */
public class ItemFactory {

    private static final Random RANDOM = new Random();

    // All item types available for random selection
    private static final ItemType[] ALL_TYPES = ItemType.values();

    // Private constructor — static factory only
    private ItemFactory() {}

    // ------------------------------------------------------------------ //
    //  Factory methods
    // ------------------------------------------------------------------ //

    /**
     * Create a single item of the given type at (x, y).
     */
    public static Item createItem(float x, float y, ItemType type) {
        return new Item(x, y, type);
    }

    /**
     * Create an item of a random {@link ItemType} at (x, y).
     */
    public static Item createRandomItem(float x, float y) {
        ItemType randomType = ALL_TYPES[RANDOM.nextInt(ALL_TYPES.length)];
        return new Item(x, y, randomType);
    }

    /**
     * Create a list of items — one per supplied type — all spawned at (x, y).
     *
     * @param x     spawn X position
     * @param y     spawn Y position
     * @param types one or more ItemType values
     * @return a mutable list of newly created Items
     */
    public static List<Item> createDrop(float x, float y, ItemType... types) {
        List<Item> result = new ArrayList<>(types.length);
        for (ItemType type : types) {
            result.add(createItem(x, y, type));
        }
        return result;
    }
}
