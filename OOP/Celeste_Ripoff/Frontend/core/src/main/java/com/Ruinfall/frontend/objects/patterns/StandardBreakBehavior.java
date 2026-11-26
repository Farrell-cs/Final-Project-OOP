package com.Ruinfall.frontend.objects.patterns;

import com.Ruinfall.frontend.objects.Interactables.Rock;
import com.Ruinfall.frontend.systems.GameEventBus;

/**
 * Strategy pattern — standard break behaviour for a Rock.
 * Reduces HP on each hit and notifies the GameEventBus when destroyed.
 */
public class StandardBreakBehavior implements BreakBehavior {

    @Override
    public void onHit(Rock rock, int damage) {
        // Notify listeners that the rock was struck (visual effect hook)
        System.out.println("[Rock] Hit! Damage: " + damage
                + " | Remaining HP: " + rock.getHealth());
        GameEventBus.getInstance().publish(
                GameEventBus.GameEvent.ROCK_BROKEN,
                "hit:" + rock.getX() + "," + rock.getY());
    }

    @Override
    public void onDestroy(Rock rock) {
        System.out.println("[Rock] Destroyed at (" + rock.getX() + ", " + rock.getY() + ")");
        // Actual item drop / event publishing is handled by Rock.spawnDrops()
    }
}
