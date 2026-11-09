package com.Ruinfall.frontend;

import com.badlogic.gdx.math.Vector2;
import com.Ruinfall.frontend.core.KinematicMotor2D;
import com.Ruinfall.frontend.core.MovementMath;
import com.Ruinfall.frontend.data.PlayerMovementStats;
import com.Ruinfall.frontend.input.PlayerInputHandler;
import com.Ruinfall.frontend.states.ClimbState;
import com.Ruinfall.frontend.states.DashState;
import com.Ruinfall.frontend.states.NormalState;
import com.Ruinfall.frontend.states.PlayerStateMachine;
import com.Ruinfall.frontend.subsystems.DashManager;
import com.Ruinfall.frontend.subsystems.StaminaSystem;

public class PlayerController {
    public enum JumpKind {
        GROUND,
        WALL_KICK,
        NEUTRAL_WALL,
        CLIMB,
        SUPER,
        HYPER,
        WALLBOUNCE
    }

    public interface PlayerEventListener {
        void onJumped(JumpKind kind);
        void onDashed(Vector2 dir);
        void onLanded(float impactSpeed);
        void onRespawned();
    }

    private final PlayerMovementStats stats = new PlayerMovementStats();
    private final KinematicMotor2D motor = new KinematicMotor2D();
    private final PlayerInputHandler input = new PlayerInputHandler();
    private final StaminaSystem stamina = new StaminaSystem();
    private final DashManager dashes = new DashManager();
    private final PlayerStateMachine stateMachine = new PlayerStateMachine();

    private final NormalState normalState;
    private final ClimbState climbState;
    private final DashState dashState;

    private final Vector2 velocity = new Vector2();
    private int facingDirection = 1;

    private float coyoteTimer;
    private float varJumpTimer;
    private float varJumpSpeed;
    private float grabCooldownTimer;

    private final Vector2 lastDashDirection = new Vector2();
    private boolean lastDashWasDemodash;
    private final Vector2 spawnPosition = new Vector2(0.0f, 0.05f);

    private int forcedMoveXDir;
    private float forcedMoveXTimer;
    private boolean wasGroundedLastStep;
    private boolean pendingAirborneUltra;
    private int pendingUltraSignX;

    private PlayerEventListener listener;

    public PlayerController() {
        normalState = new NormalState(this, stateMachine);
        climbState = new ClimbState(this, stateMachine);
        dashState = new DashState(this, stateMachine);

        applyConfiguration();
        stateMachine.initialize(normalState);
    }

    public void setEventListener(PlayerEventListener listener) {
        this.listener = listener;
    }

    public void applyConfiguration() {
        motor.configure(
            stats.normalColliderSize,
            stats.duckColliderSize,
            stats.cornerCorrectionDistance,
            stats.dashFloorPopDistance,
            stats.maxLiftBoostX,
            stats.maxLiftBoostY,
            stats.liftBoostGraceTime
        );

        input.configure(stats.jumpBufferTime, stats.dashBufferTime);

        stamina.configure(
            stats.climbMaxStamina,
            stats.climbUpCost,
            stats.climbStillCost,
            stats.climbJumpCost,
            stats.climbTiredThreshold
        );

        dashes.configure(
            stats.maxDashes,
            stats.dashCooldown,
            stats.dashRefillCooldown,
            stats.dashAttackTime,
            stats.dashFreezeTime
        );
    }

    public void updateInput() {
        input.update();

        if (input.isRespawnPressedThisFrame()) {
            respawn();
            return;
        }

        if (input.isToggleDashesPressedThisFrame()) {
            int nextMax = dashes.getMaxDashes() == 1 ? 2 : 1;
            dashes.setMaxDashes(nextMax);
        }

        if (stateMachine.getCurrentState() != null) {
            stateMachine.getCurrentState().logicUpdate(0.0166667f);
        }
    }

    public void stepPhysics(float dt) {
        boolean wasFrozen = dashes.isFrozen();
        dashes.tick(dt);

        if (wasFrozen || dashes.isFrozen()) {
            return;
        }

        input.tickBuffers(dt);
        motor.tickTimers(dt);
        tickMovementTimers(dt);

        motor.updateGroundState(velocity.y);
        if (motor.isGrounded()) {
            coyoteTimer = stats.coyoteTime;
            stamina.refill();
            dashes.tryRefillOnGround();

            if (!wasGroundedLastStep) {
                handleLandingTransition();
            }
        }
        wasGroundedLastStep = motor.isGrounded();

        if (stateMachine.getCurrentState() != null) {
            stateMachine.getCurrentState().physicsUpdate(dt);
        }

        motor.setPreferredCeilingNudgeDir(Math.abs(velocity.x) > 0.1f ? (velocity.x > 0.0f ? 1 : -1) : 0);

        motor.move(velocity, dt, this::handleHorizontalCollision, this::handleVerticalCollision);

        if (motor.isGrounded()) {
            coyoteTimer = stats.coyoteTime;
            stamina.refill();
            dashes.tryRefillOnGround();

            if (!wasGroundedLastStep) {
                handleLandingTransition();
                wasGroundedLastStep = true;
            }
        }
    }

    public boolean tryStartDashFromBuffer() {
        if (!input.hasBufferedDash() || !dashes.canDash()) {
            return false;
        }

        boolean[] crouchDash = new boolean[1];
        if (!input.consumeDashBuffer(crouchDash)) {
            return false;
        }

        if (!dashes.tryConsumeDash()) {
            return false;
        }

        clearPendingUltra();
        dashState.prepareDash(crouchDash[0]);
        stateMachine.changeState(dashState);
        return true;
    }

    public void recordDashLaunch(Vector2 dir, boolean wasDemodash) {
        lastDashDirection.set(dir);
        lastDashWasDemodash = wasDemodash;
        if (listener != null) {
            listener.onDashed(dir);
        }
    }

    public void performGroundJump() {
        input.consumeJumpBuffer();
        coyoteTimer = 0.0f;
        motor.setDucking(false);

        Vector2 lift = motor.getClampedLiftBoost();
        int moveX = getEffectiveMoveX();

        float vx = velocity.x + moveX * stats.jumpHBoost + lift.x;
        float vy = stats.jumpSpeed + lift.y;

        velocity.set(vx, vy);
        varJumpSpeed = vy;
        varJumpTimer = stats.varJumpTime;

        if (listener != null) {
            listener.onJumped(JumpKind.GROUND);
        }
    }

    public void performSuperOrHyperJump(boolean isHyper) {
        input.consumeJumpBuffer();
        coyoteTimer = 0.0f;
        dashes.cancelDashAttack();
        clearPendingUltra();

        if (input.getMoveX() != 0) {
            setFacing(input.getMoveX());
        }

        float vx = facingDirection * stats.superJumpH;
        float vy = stats.superJumpSpeed;

        if (isHyper) {
            vx *= stats.hyperJumpHMult;
            vy *= stats.hyperJumpVertMult;
            motor.setDucking(true);
        } else {
            motor.setDucking(false);
        }

        Vector2 lift = motor.getClampedLiftBoost();
        vx += lift.x;
        vy += lift.y;

        velocity.set(vx, vy);
        varJumpSpeed = vy;
        varJumpTimer = stats.varJumpTime;

        if (listener != null) {
            listener.onJumped(isHyper ? JumpKind.HYPER : JumpKind.SUPER);
        }
    }

    public void performWallJump(int pushDir) {
        input.consumeJumpBuffer();
        coyoteTimer = 0.0f;
        motor.setDucking(false);
        setGrabCooldown(0.12f);

        boolean isNeutral = input.getMoveX() == 0;
        if (!isNeutral) {
            forceMoveX(pushDir, stats.wallJumpForceTime);
            setFacing(pushDir);
        }

        Vector2 lift = motor.getClampedLiftBoost();
        float vx = pushDir * stats.wallJumpHSpeed + lift.x;
        float vy = stats.jumpSpeed + lift.y;

        velocity.set(vx, vy);
        varJumpSpeed = vy;
        varJumpTimer = stats.varJumpTime;

        if (listener != null) {
            listener.onJumped(isNeutral ? JumpKind.NEUTRAL_WALL : JumpKind.WALL_KICK);
        }
    }

    public void performClimbJump(int wallDir) {
        input.consumeJumpBuffer();
        coyoteTimer = 0.0f;
        stamina.drainClimbJump();
        setGrabCooldown(0.10f);

        Vector2 lift = motor.getClampedLiftBoost();
        float vx = velocity.x + lift.x;
        float vy = stats.jumpSpeed + lift.y;

        velocity.set(vx, vy);
        varJumpSpeed = vy;
        varJumpTimer = stats.varJumpTime;

        if (listener != null) {
            listener.onJumped(JumpKind.CLIMB);
        }
    }

    public void performWallbounce(int pushDir) {
        input.consumeJumpBuffer();
        coyoteTimer = 0.0f;
        dashes.cancelDashAttack();
        forcedMoveXTimer = 0.0f;
        motor.setDucking(false);
        setFacing(pushDir);

        Vector2 lift = motor.getClampedLiftBoost();
        float vx = pushDir * stats.superWallJumpH + lift.x;
        float vy = stats.superWallJumpSpeed + lift.y;

        velocity.set(vx, vy);
        varJumpSpeed = vy;
        varJumpTimer = stats.superWallJumpVarTime;

        if (listener != null) {
            listener.onJumped(JumpKind.WALLBOUNCE);
        }
    }

    public void respawn() {
        velocity.setZero();
        cancelVarJump();
        clearPendingUltra();
        forcedMoveXTimer = 0.0f;
        coyoteTimer = 0.0f;
        motor.setDucking(false);
        motor.teleport(spawnPosition.x, spawnPosition.y);
        stamina.refill();
        dashes.forceRefill();
        dashes.cancelDashAttack();
        stateMachine.changeState(normalState);

        if (listener != null) {
            listener.onRespawned();
        }
    }

    public int getEffectiveMoveX() {
        return forcedMoveXTimer > 0.0f ? forcedMoveXDir : input.getMoveX();
    }

    public void forceMoveX(int dir, float duration) {
        forcedMoveXDir = dir;
        forcedMoveXTimer = duration;
    }

    public void setGrabCooldown(float duration) {
        grabCooldownTimer = Math.max(grabCooldownTimer, duration);
    }

    public void setFacing(int dir) {
        if (dir != 0) {
            facingDirection = dir > 0 ? 1 : -1;
        }
    }

    public void cancelVarJump() {
        varJumpTimer = 0.0f;
    }

    public void markPendingAirborneUltra(int dirSignX) {
        pendingAirborneUltra = true;
        pendingUltraSignX = dirSignX;
    }

    public void clearPendingUltra() {
        pendingAirborneUltra = false;
    }

    private void tickMovementTimers(float dt) {
        if (coyoteTimer > 0.0f) {
            coyoteTimer = Math.max(0.0f, coyoteTimer - dt);
        }
        if (varJumpTimer > 0.0f) {
            varJumpTimer = Math.max(0.0f, varJumpTimer - dt);
        }
        if (forcedMoveXTimer > 0.0f) {
            forcedMoveXTimer = Math.max(0.0f, forcedMoveXTimer - dt);
        }
        if (grabCooldownTimer > 0.0f) {
            grabCooldownTimer = Math.max(0.0f, grabCooldownTimer - dt);
        }
    }

    private void handleHorizontalCollision() {
        clearPendingUltra();
        velocity.x = 0.0f;
    }

    private void handleVerticalCollision() {
        if (velocity.y > 0.0f) {
            cancelVarJump();
        } else if (velocity.y < 0.0f && stateMachine.getCurrentState() == dashState) {
            dashState.onVerticalGroundHitDuringDash();
            return;
        }
        velocity.y = 0.0f;
    }

    private void handleLandingTransition() {
        float impactSpeed = Math.abs(velocity.y);
        if (pendingAirborneUltra &&
            (int) Math.signum(velocity.x) == pendingUltraSignX &&
            Math.abs(velocity.x) >= stats.maxRun) {
            velocity.x *= stats.ultraSpeedMult;
            motor.setDucking(true);
        }

        clearPendingUltra();
        if (listener != null) {
            listener.onLanded(impactSpeed);
        }
    }

    public PlayerMovementStats getStats() { return stats; }
    public KinematicMotor2D getMotor() { return motor; }
    public PlayerInputHandler getInput() { return input; }
    public StaminaSystem getStamina() { return stamina; }
    public DashManager getDashes() { return dashes; }
    public PlayerStateMachine getStateMachine() { return stateMachine; }
    public NormalState getNormalState() { return normalState; }
    public ClimbState getClimbState() { return climbState; }
    public DashState getDashState() { return dashState; }
    public Vector2 getVelocity() { return velocity; }
    public void setVelocity(float x, float y) { velocity.set(x, y); }
    public int getFacingDirection() { return facingDirection; }
    public float getCoyoteTimer() { return coyoteTimer; }
    public float getVarJumpTimer() { return varJumpTimer; }
    public float getVarJumpSpeed() { return varJumpSpeed; }
    public float getGrabCooldownTimer() { return grabCooldownTimer; }
    public Vector2 getLastDashDirection() { return lastDashDirection; }
    public boolean isLastDashDemodash() { return lastDashWasDemodash; }
}
