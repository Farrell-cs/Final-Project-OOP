package com.Ruinfall.frontend.states;

public class PlayerStateMachine {
    private PlayerState currentState;
    private PlayerState previousState;

    public void initialize(PlayerState startingState) {
        currentState = startingState;
        previousState = null;
        if (currentState != null) {
            currentState.enter();
        }
    }

    public void changeState(PlayerState nextState) {
        if (nextState == null || nextState == currentState) {
            return;
        }
        previousState = currentState;
        if (currentState != null) {
            currentState.exit();
        }
        currentState = nextState;
        currentState.enter();
    }

    public PlayerState getCurrentState() {
        return currentState;
    }

    public PlayerState getPreviousState() {
        return previousState;
    }
}
