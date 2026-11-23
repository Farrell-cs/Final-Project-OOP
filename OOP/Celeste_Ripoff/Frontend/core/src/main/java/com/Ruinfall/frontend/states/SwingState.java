package com.Ruinfall.frontend.states;

import com.badlogic.gdx.math.Vector2;
import com.Ruinfall.frontend.PlayerController;
import com.Ruinfall.frontend.commands.SwingCommand;
import com.Ruinfall.frontend.core.KinematicMotor2D;
import com.Ruinfall.frontend.data.PlayerMovementStats;
import com.Ruinfall.frontend.systems.GameEventBus;

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
    private float reachDistance = 1.5f;
    private float pogoBounceVelocity = 18.0f;
    private boolean hasHitThisSwing = false;

    private SwingCommand lastCommand;

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

        lastCommand = new SwingCommand(this, player);
        lastCommand.execute();
    }

    @Override
    public void exit() {
        // Deactivate weapon hitbox when leaving the state
    }

    @Override
    public void logicUpdate(float dt) {
        swingTimer += dt;

        // Keep hitbox synced to the player as they move during the swing
        calculateHitboxOffset();

        if (swingTimer >= swingDuration) {
            transitionOut();
        }
    }

    @Override
    public void physicsUpdate(float dt) {
        // Allow the player to keep moving/falling normally while swinging
        PlayerMovementStats stats = player.getStats();
        boolean grounded = player.isGrounded();
        int effectiveMoveX = player.getEffectiveMoveX();

        if (effectiveMoveX != 0) {
            player.setFacing(effectiveMoveX);
        }

        float nextVx = com.Ruinfall.frontend.core.MovementMath.computeHorizontalVelocity(
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

        if (grounded && player.getVelocity().y <= 0.0f) {
            player.getVelocity().y = 0.0f;
        } else {
            float nextVy = com.Ruinfall.frontend.core.MovementMath.approach(
                player.getVelocity().y,
                -stats.maxFall,
                stats.gravity * dt
            );
            player.getVelocity().y = nextVy;
        }
    }

    /**
     * Resolves whether the player is swinging front, up, down, or diagonally
     * based on directional input and grounded state.
     */
    private void determineSwingDirection() {
        Vector2 input = player.getMoveInput();
        boolean isGrounded = player.isGrounded();
        int facingDir = player.getFacingDirection();

        float deadzone = 0.35f;
        boolean pressingUp = input.y > deadzone;
        boolean pressingDown = input.y < -deadzone;
        boolean pressingHorizontal = Math.abs(input.x) > deadzone;

        if (pressingHorizontal) {
            facingDir = input.x > 0 ? 1 : -1;
            player.setFacing(facingDir);
        }

        if (pressingUp && pressingHorizontal) {
            currentDirection = SwingDirection.DIAGONAL_UP;
            swingVector.set(facingDir, 1f).nor();
        } else if (pressingUp) {
            currentDirection = SwingDirection.UP;
            swingVector.set(0f, 1f);
        } else if (pressingDown && !isGrounded && pressingHorizontal) {
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
        Vector2 playerCenter = player.getPosition();
        hitboxOffset.set(swingVector).scl(reachDistance).add(playerCenter);
    }

    /**
     * Call this from your collision listener when the swing connects with an enemy, breakable, or hazard.
     */
    public void onSwingHit(boolean isPogoable) {
        if (hasHitThisSwing) return;
        hasHitThisSwing = true;

        // Apply upward pogo bounce if swinging downward or diagonally downward
        if (isPogoable && (currentDirection == SwingDirection.DOWN || currentDirection == SwingDirection.DIAGONAL_DOWN)) {
            // Apply upward velocity directly on player
            player.getVelocity().set(player.getVelocity().x, pogoBounceVelocity);
            player.getDashes().tryRefillOnGround(); // Pogo resets dash count like in platformers
            GameEventBus.getInstance().publish(GameEventBus.GameEvent.POGO_BOUNCE, player);
            System.out.println("[SwingState] POGO BOUNCE triggered! Velocity set to: " + pogoBounceVelocity);
        }
    }

    private void transitionOut() {
        stateMachine.changeState(player.getNormalState());
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

    public float getPogoBounceVelocity() {
        return pogoBounceVelocity;
    }

    public void setPogoBounceVelocity(float pogoBounceVelocity) {
        this.pogoBounceVelocity = pogoBounceVelocity;
    }
}
