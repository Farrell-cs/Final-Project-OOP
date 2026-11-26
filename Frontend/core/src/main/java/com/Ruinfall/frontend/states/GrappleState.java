package com.Ruinfall.frontend.states;

import com.badlogic.gdx.math.Vector2;
import com.Ruinfall.frontend.PlayerController;
import com.Ruinfall.frontend.core.KinematicMotor2D;
import com.Ruinfall.frontend.data.PlayerMovementStats;
import com.Ruinfall.frontend.objects.Interactables.GrapplePoint;

public class GrappleState extends PlayerState {
    private GrapplePoint targetPoint;
    private float grappleTimer;
    private boolean jumpCancelRequested;

    private final Vector2 direction = new Vector2();
    private final Vector2 playerCenter = new Vector2();

    // Tune these to match your PlayerMovementStats scale
    private float grapplePullSpeed = 24f;
    private float grappleEndSpeed = 17.0f;
    private float releaseDistance = 0.75f;
    private float grappleJumpBoostY = 13.5f;
    private float maxGrappleTime = 1.2f;

    public GrappleState(PlayerController player, PlayerStateMachine stateMachine) {
        super(player, stateMachine);
    }

    @Override
    public String getStateName() {
        return "Grapple";
    }

    public void prepareGrapple(GrapplePoint point) {
        this.targetPoint = point;
    }

    @Override
    public void enter() {
        grappleTimer = maxGrappleTime;
        jumpCancelRequested = false;

        player.cancelVarJump();
        player.clearPendingUltra();
        player.getVelocity().setZero();
        player.getMotor().setDucking(false);

        if (targetPoint != null) {
            targetPoint.engage();
            updateDirection();
            if (Math.abs(direction.x) > 0.01f) {
                player.setFacing(direction.x > 0.0f ? 1 : -1);
            }
        }
    }

    @Override
    public void exit() {
        if (targetPoint != null) {
            targetPoint.release();
            targetPoint = null;
        }
    }

    @Override
    public void logicUpdate(float dt) {
        if (player.getInput().hasBufferedJump()) {
            jumpCancelRequested = true;
        }
    }

    @Override
    public void physicsUpdate(float dt) {
        KinematicMotor2D motor = player.getMotor();

        // 1. Validate target still exists
        if (targetPoint == null || !targetPoint.isActive()) {
            finishGrapple(false);
            return;
        }

        // 2. Jump cancel out of grapple (momentum slingshot)
        if (jumpCancelRequested || player.getInput().hasBufferedJump()) {
            performGrappleJump();
            return;
        }

        // 3. Recalculate vector from player center to grapple point center
        float distance = updateDirection();

        // 4. Arrived at the grapple point
        if (distance <= releaseDistance) {
            finishGrapple(true);
            return;
        }

        // 5. Face direction of pull & apply constant pull velocity
        if (Math.abs(direction.x) > 0.01f) {
            player.setFacing(direction.x > 0.0f ? 1 : -1);
        }

        player.getVelocity().set(direction).scl(grapplePullSpeed);

        // 6. Check if player slammed into a wall mid-pull
        if (Math.abs(direction.x) > 0.1f && motor.checkWall((int) Math.signum(direction.x), 0.05f)) {
            finishGrapple(false);
            return;
        }

        // 7. Failsafe timeout
        grappleTimer -= dt;
        if (grappleTimer <= 0.0f) {
            finishGrapple(false);
        }
    }

    private float updateDirection() {
        KinematicMotor2D motor = player.getMotor();
        PlayerMovementStats stats = player.getStats();

        Vector2 size = motor.isDucking() ? stats.duckColliderSize : stats.normalColliderSize;

        // Swap motor.getPosition() to motor.getX()/getY() if your KinematicMotor2D uses separate floats
        playerCenter.set(
            motor.getPosition().x,
            motor.getPosition().y + size.y * 0.5f
        );

        direction.set(targetPoint.getCenter()).sub(playerCenter);
        float distance = direction.len();

        if (distance > 0.0001f) {
            direction.scl(1.0f / distance);
        } else {
            direction.setZero();
        }

        return distance;
    }

    private void performGrappleJump() {
        player.getInput().consumeJumpBuffer();

        Vector2 lift = player.getMotor().getClampedLiftBoost();
        float cancelSpeed = 20.0f;
        float vx = direction.x * cancelSpeed;
        if (Math.abs(direction.x) > 0.1f && Math.abs(vx) < 19.5f) {
            vx = Math.signum(direction.x) * 19.5f;
        }
        vx += lift.x;
        float vy = Math.max(direction.y * cancelSpeed, 18.5f) + lift.y;

        player.getVelocity().set(vx, vy);
        player.startGrappleCoast(0.55f);
        transitionOut();
    }

    private void finishGrapple(boolean reachedTarget) {
        if (reachedTarget) {
            Vector2 endVel = new Vector2(direction).scl(grappleEndSpeed);
            endVel.y = Math.max(endVel.y, grappleJumpBoostY * 0.85f);
            player.getVelocity().set(endVel);
            player.startGrappleCoast(0.55f);
        } else {
            player.getVelocity().scl(0.5f);
        }

        transitionOut();
    }

    private void transitionOut() {
        if (player.getInput().isGrabActive() && !player.getStamina().isExhausted() &&
            player.getMotor().checkWall(player.getFacingDirection(), 0.08f)) {
            stateMachine.changeState(player.getClimbState());
        } else {
            stateMachine.changeState(player.getNormalState());
        }
    }

    public GrapplePoint getActiveTarget() {
        return targetPoint;
    }
}
