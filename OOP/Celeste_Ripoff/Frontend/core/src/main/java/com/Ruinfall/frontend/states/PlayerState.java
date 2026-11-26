package com.Ruinfall.frontend.states;

import com.Ruinfall.frontend.PlayerController;

public abstract class PlayerState {
    protected final PlayerController player;
    protected final PlayerStateMachine stateMachine;

    public abstract String getStateName();

    protected PlayerState(PlayerController player, PlayerStateMachine stateMachine) {
        this.player = player;
        this.stateMachine = stateMachine;
    }

    public void enter() {}
    public void exit() {}
    public void logicUpdate(float dt) {}
    public void physicsUpdate(float dt) {}
}
