package com.Ruinfall.frontend.states;

import com.Ruinfall.frontend.PlayerController;
import com.Ruinfall.frontend.core.KinematicMotor2D;
import com.Ruinfall.frontend.core.MovementMath;
import com.Ruinfall.frontend.data.PlayerMovementStats;
import com.Ruinfall.frontend.input.PlayerInputHandler;

public class ClimbState extends PlayerState {
    private int wallDir;

    public ClimbState(PlayerController player, PlayerStateMachine stateMachine) {
        super(player, stateMachine);
    }

    @Override
    public String getStateName() {
        return "Climb";
    }

    @Override
    public void enter() {
        wallDir = player.getFacingDirection();
        player.getMotor().setDucking(false);
        player.getMotor().setAllowDashFloorPop(false);
        player.cancelVarJump();
        player.clearPendingUltra();
        player.getVelocity().set(0.0f, player.getVelocity().y * 0.25f);
    }

    @Override
    public void physicsUpdate(float dt) {
        PlayerMovementStats stats = player.getStats();
        PlayerInputHandler input = player.getInput();
        KinematicMotor2D motor = player.getMotor();

        if (player.tryStartDashFromBuffer()) {
            return;
        }

        if (input.hasBufferedJump()) {
            if (input.getMoveX() == -wallDir) {
                player.performWallJump(-wallDir);
            } else {
                player.performClimbJump(wallDir);
            }
            stateMachine.changeState(player.getNormalState());
            return;
        }

        if (!input.isGrabActive()) {
            stateMachine.changeState(player.getNormalState());
            return;
        }

        if (!motor.checkWall(wallDir, 0.10f)) {
            if (player.getVelocity().y > 0.0f || input.getMoveY() > 0) {
                performLedgeClimbHop();
            } else {
                stateMachine.changeState(player.getNormalState());
            }
            return;
        }

        if (player.getStamina().isExhausted()) {
            float slipVy = MovementMath.approach(player.getVelocity().y, -stats.climbSlipSpeed, stats.climbAccel * dt);
            player.getVelocity().set(0.0f, slipVy);
            if (motor.isGrounded()) {
                stateMachine.changeState(player.getNormalState());
            }
            return;
        }

        float targetVy = 0.0f;
        if (input.getMoveY() > 0) {
            targetVy = stats.climbUpSpeed;
            player.getStamina().drainClimbUp(dt);
        } else if (input.getMoveY() < 0) {
            if (motor.isGrounded()) {
                stateMachine.changeState(player.getNormalState());
                return;
            }
            targetVy = -stats.climbDownSpeed;
        } else {
            player.getStamina().drainHold(dt);
        }

        float nextVy = MovementMath.approach(player.getVelocity().y, targetVy, stats.climbAccel * dt);
        player.getVelocity().set(0.0f, nextVy);
    }

    private void performLedgeClimbHop() {
        PlayerMovementStats stats = player.getStats();
        if (player.getMotor().checkOpenSpaceAtOffset(wallDir * 0.25f, 0.20f)) {
            player.getVelocity().set(wallDir * stats.climbHopX, stats.climbHopY);
            player.forceMoveX(wallDir, stats.climbHopForceTime);
            player.setGrabCooldown(0.14f);
        }
        stateMachine.changeState(player.getNormalState());
    }
}
