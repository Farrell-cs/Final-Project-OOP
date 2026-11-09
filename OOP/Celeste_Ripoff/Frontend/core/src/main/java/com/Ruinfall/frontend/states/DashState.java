package com.Ruinfall.frontend.states;

import com.badlogic.gdx.math.Vector2;
import com.Ruinfall.frontend.PlayerController;
import com.Ruinfall.frontend.core.KinematicMotor2D;
import com.Ruinfall.frontend.data.PlayerMovementStats;

public class DashState extends PlayerState {
    private float dashTimer;
    private boolean dashBurstLaunched;
    private boolean isCrouchDash;
    private final Vector2 preDashVelocity = new Vector2();
    private final Vector2 lockedDirection = new Vector2();
    private boolean appliedMidDashUltra;

    public DashState(PlayerController player, PlayerStateMachine stateMachine) {
        super(player, stateMachine);
    }

    @Override
    public String getStateName() {
        return "Dash";
    }

    public void prepareDash(boolean crouchDashRequested) {
        isCrouchDash = crouchDashRequested || player.getMotor().isDucking();
    }

    @Override
    public void enter() {
        PlayerMovementStats stats = player.getStats();
        dashTimer = stats.dashTime;
        dashBurstLaunched = false;
        appliedMidDashUltra = false;

        preDashVelocity.set(player.getVelocity());
        lockedDirection.set(player.getInput().getEightWayDashDirection(player.getFacingDirection()));

        player.cancelVarJump();
        player.getVelocity().setZero();

        if (isCrouchDash) {
            player.getMotor().setDucking(true);
        } else if (lockedDirection.y >= -0.01f) {
            player.getMotor().setDucking(false);
        }

        if (!player.getDashes().isFrozen()) {
            launchDashBurst();
        }
    }

    @Override
    public void exit() {
        player.getMotor().setAllowDashFloorPop(false);
    }

    @Override
    public void logicUpdate(float dt) {
        if (!dashBurstLaunched && player.getDashes().isFrozen()) {
            Vector2 updatedAim = player.getInput().getEightWayDashDirection(player.getFacingDirection());
            if (player.getInput().getRawMove().len2() >= 0.0625f) {
                lockedDirection.set(updatedAim);
            }
        }
    }

    @Override
    public void physicsUpdate(float dt) {
        PlayerMovementStats stats = player.getStats();
        KinematicMotor2D motor = player.getMotor();

        if (!dashBurstLaunched) {
            launchDashBurst();
        }

        if (player.getInput().hasBufferedJump()) {
            if (lockedDirection.y > 0.99f && Math.abs(lockedDirection.x) < 0.01f) {
                if (motor.checkWall(1, stats.superWallJumpCheckDist)) {
                    player.performWallbounce(-1);
                    stateMachine.changeState(player.getNormalState());
                    return;
                }
                if (motor.checkWall(-1, stats.superWallJumpCheckDist)) {
                    player.performWallbounce(1);
                    stateMachine.changeState(player.getNormalState());
                    return;
                }
            }

            if ((motor.isGrounded() || player.getCoyoteTimer() > 0.0f) &&
                Math.abs(lockedDirection.x) > 0.01f &&
                lockedDirection.y <= 0.01f) {
                boolean isHyper = motor.isDucking() || lockedDirection.y < -0.01f || isCrouchDash;
                player.performSuperOrHyperJump(isHyper);
                stateMachine.changeState(player.getNormalState());
                return;
            }
        }

        dashTimer -= dt;
        if (dashTimer <= 0.0f) {
            finishDashNaturally();
        }
    }

    public void onVerticalGroundHitDuringDash() {
        if (!appliedMidDashUltra && Math.abs(lockedDirection.x) > 0.01f && lockedDirection.y < -0.01f) {
            appliedMidDashUltra = true;
            lockedDirection.set(Math.signum(lockedDirection.x), 0.0f);
            player.getMotor().setDucking(true);
            player.getVelocity().x *= player.getStats().ultraSpeedMult;
            player.getVelocity().y = 0.0f;
        }
    }

    private void launchDashBurst() {
        PlayerMovementStats stats = player.getStats();
        dashBurstLaunched = true;

        if (player.getInput().getRawMove().len2() >= 0.0625f) {
            lockedDirection.set(player.getInput().getEightWayDashDirection(player.getFacingDirection()));
        }

        if (Math.abs(lockedDirection.x) > 0.01f) {
            player.setFacing(lockedDirection.x > 0.0f ? 1 : -1);
        }

        Vector2 dashVel = new Vector2(lockedDirection).scl(stats.dashSpeed);

        if (Math.abs(lockedDirection.y) < 0.01f &&
            Math.signum(preDashVelocity.x) == Math.signum(dashVel.x) &&
            Math.abs(preDashVelocity.x) > Math.abs(dashVel.x)) {
            dashVel.x = preDashVelocity.x;
        }

        Vector2 liftBoost = player.getMotor().getClampedLiftBoost().scl(0.35f);
        dashVel.add(liftBoost);

        player.getMotor().setAllowDashFloorPop(Math.abs(lockedDirection.y) < 0.01f && Math.abs(lockedDirection.x) > 0.01f);
        player.recordDashLaunch(lockedDirection, isCrouchDash);
        player.getVelocity().set(dashVel);
    }

    private void finishDashNaturally() {
        PlayerMovementStats stats = player.getStats();

        if (lockedDirection.y >= -0.01f) {
            Vector2 endVel = new Vector2(lockedDirection).scl(stats.endDashSpeed);
            if (endVel.y > 0.0f) {
                endVel.y *= stats.endDashUpMult;
            }

            if (Math.signum(endVel.x) == Math.signum(player.getVelocity().x) &&
                Math.abs(player.getVelocity().x) > Math.abs(endVel.x)) {
                endVel.x = player.getVelocity().x;
            }

            player.getVelocity().set(endVel);
        } else if (!player.getMotor().isGrounded() && Math.abs(lockedDirection.x) > 0.01f) {
            player.markPendingAirborneUltra(lockedDirection.x > 0.0f ? 1 : -1);
        }

        if (player.getInput().isGrabActive() && !player.getStamina().isExhausted() &&
            player.getMotor().checkWall(player.getFacingDirection(), 0.08f)) {
            stateMachine.changeState(player.getClimbState());
        } else {
            stateMachine.changeState(player.getNormalState());
        }
    }

    public Vector2 getActiveDashDirection() {
        return lockedDirection;
    }
}
