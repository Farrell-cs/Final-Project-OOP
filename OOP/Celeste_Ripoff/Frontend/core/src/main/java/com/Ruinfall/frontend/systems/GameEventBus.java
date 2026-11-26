package com.Ruinfall.frontend.systems;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton + Observer pattern implementation.
 * Central event bus for game-wide events such as item pickups,
 * stalactite falls, pogo bounces, and more.
 */
public class GameEventBus {

    // ------------------------------------------------------------------ //
    //  Singleton
    // ------------------------------------------------------------------ //

    private static GameEventBus instance;

    private GameEventBus() {}

    public static GameEventBus getInstance() {
        if (instance == null) {
            instance = new GameEventBus();
        }
        return instance;
    }

    // ------------------------------------------------------------------ //
    //  Events
    // ------------------------------------------------------------------ //

    public enum GameEvent {
        ITEM_COLLECTED,
        POGO_BOUNCE,
        STALACTITE_TRIGGERED,
        STALACTITE_LANDED,
        CRYSTAL_SHATTERED,
        ROCK_SHATTERED,
        ROCK_BROKEN,
        BARREL_HIT,
        BARREL_EXPLODED,
        SWING_EXECUTED,
        PLAYER_DASH_START,
        PLAYER_DASH_END,
        GRAPPLE_LATCHED,
        BOSS_TUNNEL_BREACH,
        BOSS_STAGE_CHANGED,
        BULLET_SPAWNED,
        BUBBLE_POPPED,
        BULLET_HIT
    }

    // ------------------------------------------------------------------ //
    //  Listener interface
    // ------------------------------------------------------------------ //

    public interface GameEventListener {
        /**
         * Called when the subscribed event fires.
         *
         * @param event   the event that occurred
         * @param payload optional context object (may be null)
         */
        void onEvent(GameEvent event, Object payload);
    }

    // ------------------------------------------------------------------ //
    //  Internal listener registry
    // ------------------------------------------------------------------ //

    private final Map<GameEvent, List<GameEventListener>> listeners =
            new EnumMap<>(GameEvent.class);

    // ------------------------------------------------------------------ //
    //  Public API
    // ------------------------------------------------------------------ //

    /**
     * Subscribe a listener to a specific event.
     *
     * @param event    the event to listen for
     * @param listener the listener to register
     */
    public void subscribe(GameEvent event, GameEventListener listener) {
        listeners.computeIfAbsent(event, k -> new ArrayList<>()).add(listener);
    }

    /**
     * Unsubscribe a listener from a specific event.
     *
     * @param event    the event to stop listening for
     * @param listener the listener to remove
     */
    public void unsubscribe(GameEvent event, GameEventListener listener) {
        List<GameEventListener> list = listeners.get(event);
        if (list != null) {
            list.remove(listener);
        }
    }

    /**
     * Publish an event to all registered listeners.
     *
     * @param event   the event to fire
     * @param payload optional context object (may be null)
     */
    public void publish(GameEvent event, Object payload) {
        List<GameEventListener> list = listeners.get(event);
        if (list == null || list.isEmpty()) {
            return;
        }
        // Iterate over a snapshot to allow listeners to unsubscribe during dispatch
        List<GameEventListener> snapshot = new ArrayList<>(list);
        for (GameEventListener listener : snapshot) {
            listener.onEvent(event, payload);
        }
    }

    /**
     * Convenience overload — publish with no payload.
     */
    public void publish(GameEvent event) {
        publish(event, null);
    }

    /**
     * Remove all listeners (useful for scene transitions / testing).
     */
    public void clearAll() {
        listeners.clear();
    }
}
