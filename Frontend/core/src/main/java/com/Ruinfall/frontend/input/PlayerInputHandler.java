package com.Ruinfall.frontend.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.Ruinfall.frontend.core.MovementMath;

public class PlayerInputHandler {
    private final Vector2 rawMove = new Vector2();
    private int moveX;
    private int moveY;

    private boolean jumpHeld;
    private boolean grabActive;
    private boolean respawnPressedThisFrame;
    private boolean toggleDashesPressedThisFrame;

    private float jumpBufferTimer;
    private float dashBufferTimer;
    private boolean crouchDashBuffered;

    private float jumpBufferDuration = 0.10f;
    private float dashBufferDuration = 0.10f;

    public void configure(float jumpBuffer, float dashBuffer) {
        jumpBufferDuration = jumpBuffer;
        dashBufferDuration = dashBuffer;
    }

    public void update() {
        boolean left = Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A);
        boolean right = Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D);
        boolean down = Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S);
        boolean up = Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W);

        moveX = (right ? 1 : 0) - (left ? 1 : 0);
        moveY = (up ? 1 : 0) - (down ? 1 : 0);
        rawMove.set(moveX, moveY);

        boolean jumpPressed = Gdx.input.isKeyJustPressed(Input.Keys.C) ||
                              Gdx.input.isKeyJustPressed(Input.Keys.SPACE) ||
                              Gdx.input.isKeyJustPressed(Input.Keys.K);

        jumpHeld = Gdx.input.isKeyPressed(Input.Keys.C) ||
                   Gdx.input.isKeyPressed(Input.Keys.SPACE) ||
                   Gdx.input.isKeyPressed(Input.Keys.K);

        if (jumpPressed) {
            jumpBufferTimer = jumpBufferDuration;
        }

        boolean normalDash = Gdx.input.isKeyJustPressed(Input.Keys.X) ||
                            Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT) ||
                            Gdx.input.isKeyJustPressed(Input.Keys.J);

        boolean crouchDash = Gdx.input.isKeyJustPressed(Input.Keys.V) ||
                            Gdx.input.isKeyJustPressed(Input.Keys.CONTROL_LEFT);

        if (normalDash || crouchDash) {
            dashBufferTimer = dashBufferDuration;
            crouchDashBuffered = crouchDash;
        }

        grabActive = Gdx.input.isKeyPressed(Input.Keys.Z) ||
                     Gdx.input.isKeyPressed(Input.Keys.L) ||
                     Gdx.input.isKeyPressed(Input.Keys.E);

        respawnPressedThisFrame = Gdx.input.isKeyJustPressed(Input.Keys.R);
        toggleDashesPressedThisFrame = Gdx.input.isKeyJustPressed(Input.Keys.T);
    }

    public void tickBuffers(float dt) {
        if (jumpBufferTimer > 0.0f) {
            jumpBufferTimer = Math.max(0.0f, jumpBufferTimer - dt);
        }

        if (dashBufferTimer > 0.0f) {
            dashBufferTimer = Math.max(0.0f, dashBufferTimer - dt);
            if (dashBufferTimer <= 0.0f) {
                crouchDashBuffered = false;
            }
        }
    }

    public boolean hasBufferedJump() {
        return jumpBufferTimer > 0.0f;
    }

    public boolean hasBufferedDash() {
        return dashBufferTimer > 0.0f;
    }

    public boolean consumeJumpBuffer() {
        if (jumpBufferTimer <= 0.0f) {
            return false;
        }
        jumpBufferTimer = 0.0f;
        return true;
    }

    public boolean consumeDashBuffer(boolean[] outCrouchDash) {
        if (dashBufferTimer <= 0.0f) {
            if (outCrouchDash != null && outCrouchDash.length > 0) outCrouchDash[0] = false;
            return false;
        }
        if (outCrouchDash != null && outCrouchDash.length > 0) {
            outCrouchDash[0] = crouchDashBuffered;
        }
        dashBufferTimer = 0.0f;
        crouchDashBuffered = false;
        return true;
    }

    public Vector2 getEightWayDashDirection(int fallbackFacing) {
        return MovementMath.quantizeEightWayAim(rawMove, fallbackFacing, 0.25f);
    }

    public int getMoveX() {
        return moveX;
    }

    public int getMoveY() {
        return moveY;
    }

    public Vector2 getRawMove() {
        return rawMove;
    }

    public boolean isJumpHeld() {
        return jumpHeld;
    }

    public boolean isGrabActive() {
        return grabActive;
    }

    public boolean isRespawnPressedThisFrame() {
        return respawnPressedThisFrame;
    }

    public boolean isToggleDashesPressedThisFrame() {
        return toggleDashesPressedThisFrame;
    }
}
