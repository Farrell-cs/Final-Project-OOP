package com.Ruinfall.frontend.commands;

/**
 * Command pattern — base interface for all undoable game commands.
 */
public interface Command {
    /** Execute the command. */
    void execute();

    /** Undo the command, restoring pre-execute state where possible. */
    void undo();
}
