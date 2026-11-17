package com.Ruinfall.frontend.states;

import com.badlogic.gdx.math.Vector2;
import com.Ruinfall.frontend.PlayerController;
import com.Ruinfall.frontend.core.KinematicMotor2D;
import com.Ruinfall.frontend.data.PlayerMovementStats;

public class SwingState extends PlayerState {

    public enum SwingDirection {
        FRONT,
        UP,
        DOWN,
        DIAGONAL_UP,
        DIAGONAL_DOWN
    }

    private SwingDirection currentDirection = SwingDirection.FRONT;
    private final Vector2 swingVector = new Vector2();
    private final Vector2 hitboxOffset = new Vector2();

    // Tune these values or pull them from PlayerMovementStats
    private float swingDuration = 0.22f;
    private float swingTimer = 0f;
    private float reachDistance = 32f;
    private float pogoBounceVelocity = 420f;
    private boolean hasHitThisSwing = false;

    @Override
    public String getStateName() {
        return "Swing";
    }

    public SwingState(PlayerController player, PlayerStateMachine stateMachine) {
        super(player, stateMachine);
    }

    @Override
    public void enter() {
        swingTimer = 0f;
        hasHitThisSwing = false;

        determineSwingDirection();
        calculateHitboxOffset();

        // Trigger your animation or hitbox activation here
        // player.playAnimation("swing_" + currentDirection.name().toLowerCase());
        // player.activateWeaponHitbox(hitboxOffset, swingVector);
    }

    @Override
    public void exit() {
        // Deactivate weapon hitbox when leaving the state
        // player.deactivateWeaponHitbox();
    }

    @Override
    public void logicUpdate(float dt) {
        swingTimer += dt;

        // Keep hitbox synced to the player as they move during the swing
        calculateHitboxOffset();
        // player.updateWeaponHitboxPosition(hitboxOffset);

        if (swingTimer >= swingDuration) {
            transitionOut();
        }
    }

    @Override
    public void physicsUpdate(float dt) {
        // Optional: Allow the player to keep moving/falling normally while swinging
        // player.applyStandardMovement(dt);
    }

    /**
     * Resolves whether the player is swinging front, up, down, or diagonally
     * based on directional input and grounded state.
     */
    private void determineSwingDirection() {
        // Replace these getters with your actual PlayerController input/state methods
        Vector2 input = player.getMoveInput(); // Expected normalized or -1 to 1 axes
        boolean isGrounded = player.isGrounded();
        int facingDir = player.getFacingDirection(); // 1 for right, -1 for left

        float deadzone = 0.35f;
        boolean pressingUp = input.y > deadzone;
        boolean pressingDown = input.y < -deadzone;
        boolean pressingHorizontal = Math.abs(input.x) > deadzone;

        // Flip facing direction if horizontal input opposes current facing (optional)
        if (pressingHorizontal) {
            facingDir = input.x > 0 ? 1 : -1;
            // player.setFacingDirection(facingDir);
        }

        if (pressingUp && pressingHorizontal) {
            currentDirection = SwingDirection.DIAGONAL_UP;
            swingVector.set(facingDir, 1f).nor();
        } else if (pressingUp) {
            currentDirection = SwingDirection.UP;
            swingVector.set(0f, 1f);
        } else if (pressingDown && !isGrounded && pressingHorizontal) {
            // Downward swings are typically restricted to mid-air (like Hollow Knight / Dead Cells)
            currentDirection = SwingDirection.DIAGONAL_DOWN;
            swingVector.set(facingDir, -1f).nor();
        } else if (pressingDown && !isGrounded) {
            currentDirection = SwingDirection.DOWN;
            swingVector.set(0f, -1f);
        } else {
            currentDirection = SwingDirection.FRONT;
            swingVector.set(facingDir, 0f);
        }
    }

    /**
     * Computes the world-space position of the swing hitbox relative to the player.
     */
    private void calculateHitboxOffset() {
        Vector2 playerCenter = player.getPosition(); // Adjust if getPosition() is bottom-center
        hitboxOffset.set(swingVector).scl(reachDistance).add(playerCenter);
    }

    /**
     * Call this from your collision listener when the swing connects with an enemy or hazard.
     */
    public void onSwingHit(boolean isPogoable) {
        if (hasHitThisSwing) return;
        hasHitThisSwing = true;

        // Apply upward pogo bounce if swinging downward or diagonally downward
        if (isPogoable && (currentDirection == SwingDirection.DOWN || currentDirection == SwingDirection.DIAGONAL_DOWN)) {
            KinematicMotor2D motor = player.getMotor();
            if (motor != null) {
                Vector2 vel = motor.getVelocity();
                motor.setVelocity(vel.x, pogoBounceVelocity);
            }
        }
    }

    private void transitionOut() {
        // Swap with your actual Idle / Run / Fall state references
        KinematicMotor2D motor = player.getMotor();
        if (motor.isGrounded()) {
            stateMachine.changeState(pla);
        } else {
            stateMachine.changeState(player.fallState);
        }
    }

    public SwingDirection getCurrentDirection() {
        return currentDirection;
    }

    public Vector2 getSwingVector() {
        return swingVector;
    }

    public Vector2 getHitboxOffset() {
        return hitboxOffset;
    }
}
