package com.Ruinfall.frontend.data;

import com.badlogic.gdx.math.Vector2;

public class PlayerMovementStats {
    public Vector2 normalColliderSize = new Vector2(0.8f, 1.375f);
    public Vector2 duckColliderSize = new Vector2(0.8f, 0.75f);
    public float cornerCorrectionDistance = 0.50f;
    public float dashFloorPopDistance = 0.50f;

    public float maxRun = 11.25f;
    public float runAccel = 125.0f;
    public float runReduce = 50.0f;
    public float airMult = 0.65f;
    public float duckFriction = 62.5f;

    public float gravity = 112.5f;
    public float halfGravThreshold = 5.0f;
    public float maxFall = 20.0f;
    public float fastMaxFall = 30.0f;
    public float fastMaxAccel = 37.5f;

    public float jumpSpeed = 13.125f;
    public float varJumpTime = 0.20f;
    public float jumpHBoost = 5.0f;
    public float coyoteTime = 0.10f;
    public float jumpBufferTime = 0.10f;

    public float wallSlideStartMax = 2.5f;
    public float wallSlideTime = 1.20f;
    public float wallJumpCheckDist = 0.375f;
    public float wallJumpHSpeed = 16.25f;
    public float wallJumpForceTime = 0.16f;

    public float climbMaxStamina = 110.0f;
    public float climbUpSpeed = 5.625f;
    public float climbDownSpeed = 10.0f;
    public float climbSlipSpeed = 3.75f;
    public float climbAccel = 112.5f;
    public float climbUpCost = 45.4545f;
    public float climbStillCost = 10.0f;
    public float climbJumpCost = 27.5f;
    public float climbTiredThreshold = 20.0f;
    public float climbHopY = 15.0f;
    public float climbHopX = 12.5f;
    public float climbHopForceTime = 0.10f;

    public int maxDashes = 1;
    public float dashSpeed = 30.0f;
    public float endDashSpeed = 20.0f;
    public float endDashUpMult = 0.75f;
    public float dashTime = 0.15f;
    public float dashAttackTime = 0.30f;
    public float dashCooldown = 0.20f;
    public float dashRefillCooldown = 0.10f;
    public float dashFreezeTime = 0.05f;
    public float dashBufferTime = 0.10f;

    public float superJumpSpeed = 13.125f;
    public float superJumpH = 32.5f;
    public float hyperJumpVertMult = 0.50f;
    public float hyperJumpHMult = 1.25f;
    public float superWallJumpCheckDist = 0.625f;
    public float superWallJumpSpeed = 20.0f;
    public float superWallJumpH = 21.25f;
    public float superWallJumpVarTime = 0.25f;
    public float ultraSpeedMult = 1.20f;

    public float maxLiftBoostX = 31.25f;
    public float maxLiftBoostY = 16.25f;
    public float liftBoostGraceTime = 0.15f;
}
