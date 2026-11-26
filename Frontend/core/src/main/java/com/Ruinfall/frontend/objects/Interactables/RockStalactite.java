package com.Ruinfall.frontend.objects.Interactables;

import com.badlogic.gdx.graphics.Color;
import com.Ruinfall.frontend.PlayerController;
import com.Ruinfall.frontend.objects.GameObject;
import com.Ruinfall.frontend.objects.items.Item;
import com.Ruinfall.frontend.objects.items.ItemType;
import com.Ruinfall.frontend.objects.patterns.HeavyFallBehavior;
import com.Ruinfall.frontend.systems.GameEventBus;
import com.Ruinfall.frontend.systems.ItemFactory;

/**
 * RockStalactite: Variant of Stalactite that plummets onto enemies and the player,
 * shattering upon ground or entity impact.
 */
public class RockStalactite extends Stalactite {

    public RockStalactite() {
        this(0, 0);
    }

    public RockStalactite(float x, float y) {
        super(x, y, 0.8f, 1.5f, new Color(0.35f, 0.35f, 0.35f, 1f));
        this.fallGravity = 30.0f;
        this.fallBehavior = new HeavyFallBehavior();
    }

    @Override
    public void onEntityHit(GameObject entity) {
        if (!active) return;

        System.out.println("[RockStalactite] Slammed onto entity: " + entity.getClass().getSimpleName());

        if (entity instanceof PlayerController player) {
            player.takeDamage(2);
            player.getVelocity().set(0, -100f);
        }

        Item drop = ItemFactory.createItem(x, y, ItemType.LIFE);
        GameEventBus.getInstance().publish(GameEventBus.GameEvent.ROCK_SHATTERED, drop);
        destroy();
    }

    @Override
    public void onGroundHit() {
        if (!active) return;
        System.out.println("[RockStalactite] Shattered upon hitting the ground!");

        Item drop = ItemFactory.createItem(x, y, ItemType.BOMB);
        GameEventBus.getInstance().publish(GameEventBus.GameEvent.ROCK_SHATTERED, drop);
        destroy();
    }

    @Override
    public boolean isPogoable() {
        return true;
    }
}
