package com.Ruinfall.frontend.states;

import com.badlogic.gdx.math.MathUtils;
import com.Ruinfall.frontend.PlayerController;
import com.Ruinfall.frontend.core.KinematicMotor2D;
import com.Ruinfall.frontend.core.MovementMath;
import com.Ruinfall.frontend.data.PlayerMovementStats;
import com.Ruinfall.frontend.input.PlayerInputHandler;

public class NormalState extends PlayerState {
    private float wallSlideTimer;

    public NormalState(PlayerController player, PlayerStateMachine stateMachine) {
        super(player, stateMachine);
    }

    @Override
    public String getStateName() {
        return "Normal";
    }

    @Override
    public void enter() {
        wallSlideTimer = player.getStats().wallSlideTime;
        player.getMotor().setAllowDashFloorPop(false);
    }

    @Override
    public void physicsUpdate(float dt) {
        PlayerMovementStats stats = player.getStats();
        PlayerInputHandler input = player.getInput();
        KinematicMotor2D motor = player.getMotor();

        if (player.tryStartDashFromBuffer()) {
            return;
        }

        boolean grounded = motor.isGrounded();
        if (grounded) {
            wallSlideTimer = stats.wallSlideTime;
        }

        handleDucking(grounded, input.getMoveY());

        if (tryHandleBufferedJump(grounded)) {
            return;
        }

        if (tryEnterClimb()) {
            return;
        }

        int effectiveMoveX = player.getEffectiveMoveX();
        if (effectiveMoveX != 0 && (!motor.isDucking() || !grounded)) {
            player.setFacing(effectiveMoveX);
        }

        if (motor.isDucking() && grounded) {
            float nextVx = MovementMath.approach(player.getVelocity().x, 0.0f, stats.duckFriction * dt);
            player.getVelocity().x = nextVx;
        } else {
            float nextVx = MovementMath.computeHorizontalVelocity(
                player.getVelocity().x,
                effectiveMoveX,
                stats.maxRun,
                stats.runAccel,
                stats.runReduce,
                stats.airMult,
                grounded,
                dt
            );
            player.getVelocity().x = nextVx;
        }

        applyVerticalGravityAndWallSlide(grounded, effectiveMoveX, dt);
    }

    private void handleDucking(boolean grounded, int moveY) {
        if (grounded && moveY < 0 && player.getVelocity().y <= 0.05f) {
            player.getMotor().setDucking(true);
        } else if (player.getMotor().isDucking() && (!grounded || moveY >= 0)) {
            if (grounded || player.getVelocity().y > 0.0f) {
                player.getMotor().setDucking(false);
            }
        }
    }

    private boolean tryHandleBufferedJump(boolean grounded) {
        if (!player.getInput().hasBufferedJump()) {
            return false;
        }

        PlayerMovementStats stats = player.getStats();
        KinematicMotor2D motor = player.getMotor();

        if (player.getDashes().inDashAttack() && player.getLastDashDirection().y > 0.99f && Math.abs(player.getLastDashDirection().x) < 0.01f) {
            if (motor.checkWall(1, stats.superWallJumpCheckDist)) {
                player.performWallbounce(-1);
                return true;
            }
            if (motor.checkWall(-1, stats.superWallJumpCheckDist)) {
                player.performWallbounce(1);
                return true;
            }
        }

        if (grounded || player.getCoyoteTimer() > 0.0f) {
            if (player.getDashes().inDashAttack() &&
                Math.abs(player.getLastDashDirection().x) > 0.01f &&
                player.getLastDashDirection().y <= 0.01f) {
                boolean isHyper = motor.isDucking() || player.getLastDashDirection().y < -0.01f || player.isLastDashDemodash();
                player.performSuperOrHyperJump(isHyper);
            } else {
                player.performGroundJump();
            }
            return true;
        }

        if (motor.checkWall(1, stats.wallJumpCheckDist)) {
            if (player.getInput().isGrabActive() && !player.getStamina().isExhausted() && player.getInput().getMoveX() >= 0) {
                player.performClimbJump(1);
            } else {
                player.performWallJump(-1);
            }
            return true;
        }

        if (motor.checkWall(-1, stats.wallJumpCheckDist)) {
            if (player.getInput().isGrabActive() && !player.getStamina().isExhausted() && player.getInput().getMoveX() <= 0) {
                player.performClimbJump(-1);
            } else {
                player.performWallJump(1);
            }
            return true;
        }

        return false;
    }

    private boolean tryEnterClimb() {
        if (!player.getInput().isGrabActive() || player.getStamina().isExhausted() || player.getGrabCooldownTimer() > 0.0f) {
            return false;
        }
        if (player.getVelocity().y > 2.5f) {
            return false;
        }

        int checkPrimary = player.getInput().getMoveX() != 0 ? player.getInput().getMoveX() : player.getFacingDirection();
        if (player.getMotor().checkWall(checkPrimary, 0.08f)) {
            player.setFacing(checkPrimary);
            stateMachine.changeState(player.getClimbState());
            return true;
        }

        if (player.getMotor().checkWall(player.getFacingDirection(), 0.08f)) {
            stateMachine.changeState(player.getClimbState());
            return true;
        }

        return false;
    }

    private void applyVerticalGravityAndWallSlide(boolean grounded, int effectiveMoveX, float dt) {
        PlayerMovementStats stats = player.getStats();

        if (player.getVarJumpTimer() > 0.0f) {
            if (player.getInput().isJumpHeld()) {
                player.getVelocity().y = Math.max(player.getVelocity().y, player.getVarJumpSpeed());
                return;
            }
            player.cancelVarJump();
        }

        if (grounded && player.getVelocity().y <= 0.0f) {
            player.getVelocity().y = 0.0f;
            return;
        }

        float maxFall = stats.maxFall;
        if (player.getInput().getMoveY() < 0 && player.getVelocity().y <= -stats.maxFall + 0.5f) {
            maxFall = MovementMath.approach(Math.abs(player.getVelocity().y), stats.fastMaxFall, stats.fastMaxAccel * dt);
        } else if (player.getInput().getMoveY() < 0) {
            maxFall = stats.fastMaxFall;
        }

        if (effectiveMoveX != 0 && player.getVelocity().y <= 0.0f && player.getInput().getMoveY() >= 0 && player.getMotor().checkWall(effectiveMoveX, 0.07f)) {
            float t = 1.0f - MathUtils.clamp(wallSlideTimer / stats.wallSlideTime, 0.0f, 1.0f);
            maxFall = MathUtils.lerp(stats.wallSlideStartMax, stats.maxFall, t);
            wallSlideTimer = Math.max(0.0f, wallSlideTimer - dt);
        }

        boolean inApexHang = Math.abs(player.getVelocity().y) < stats.halfGravThreshold && player.getInput().isJumpHeld();
        float gravMult = inApexHang ? 0.5f : 1.0f;

        float nextVy = MovementMath.approach(player.getVelocity().y, -maxFall, stats.gravity * gravMult * dt);
        player.getVelocity().y = nextVy;
    }
}
