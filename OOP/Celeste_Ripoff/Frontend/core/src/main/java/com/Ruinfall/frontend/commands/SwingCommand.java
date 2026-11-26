package com.Ruinfall.frontend.commands;

import com.badlogic.gdx.math.Vector2;
import com.Ruinfall.frontend.PlayerController;
import com.Ruinfall.frontend.states.SwingState;
import com.Ruinfall.frontend.systems.GameEventBus;

/**
 * Command pattern — encapsulates a single melee swing action.
 *
 * <p>Captured at the moment the swing begins, {@link #execute()} saves the player's
 * pre-swing velocity and broadcasts the {@link GameEventBus.GameEvent#SWING_EXECUTED}
 * event. {@link #undo()} restores the saved velocity — useful for replays or
 * gameplay rollback systems.
 */
public class SwingCommand implements Command {

    // ------------------------------------------------------------------ //
    //  Fields
    // ------------------------------------------------------------------ //

    private final SwingState  swingState;
    private final PlayerController player;
    private final SwingState.SwingDirection direction;

    /** Velocity snapshot taken at the moment execute() is called. */
    private final Vector2 savedVelocity = new Vector2();

    // ------------------------------------------------------------------ //
    //  Constructor
    // ------------------------------------------------------------------ //

    /**
     * @param swingState the SwingState that owns this command
     * @param player     the PlayerController performing the swing
     */
    public SwingCommand(SwingState swingState, PlayerController player) {
        this.swingState = swingState;
        this.player     = player;
        // Capture direction at construction time (before anything changes)
        this.direction  = swingState.getCurrentDirection();
    }

    // ------------------------------------------------------------------ //
    //  Command implementation
    // ------------------------------------------------------------------ //

    @Override
    public void execute() {
        // Save pre-swing velocity for potential undo
        savedVelocity.set(player.getVelocity());

        // Observer: broadcast that a swing was executed
        GameEventBus.getInstance().publish(
                GameEventBus.GameEvent.SWING_EXECUTED, direction);

        System.out.println("[SwingCommand] Executed swing: " + direction);
    }

    @Override
    public void undo() {
        // Restore pre-swing velocity (rollback / replay use-case)
        player.setVelocity(savedVelocity.x, savedVelocity.y);
        System.out.println("[SwingCommand] Undone swing: " + direction
                + " — velocity restored to " + savedVelocity);
    }

    // ------------------------------------------------------------------ //
    //  Getters
    // ------------------------------------------------------------------ //

    public SwingState.SwingDirection getDirection() { return direction; }
    public Vector2 getSavedVelocity()               { return savedVelocity; }
}
