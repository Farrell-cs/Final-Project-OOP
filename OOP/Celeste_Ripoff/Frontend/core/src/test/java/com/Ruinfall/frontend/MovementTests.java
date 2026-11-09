package com.Ruinfall.frontend;

import com.badlogic.gdx.math.Vector2;
import com.Ruinfall.frontend.core.MovementMath;
import com.Ruinfall.frontend.subsystems.DashManager;
import com.Ruinfall.frontend.subsystems.StaminaSystem;

public class MovementTests {
    public static void main(String[] args) {
        testHorizontalAcceleration();
        testHyperMomentumRetention();
        testEightWayAimQuantization();
        testStaminaDrainOnClimbJumps();
        testDashManagerWavedashRefillWindow();

        System.out.println("ALL 5 LIBGDX MOVEMENT TESTS PASSED!");
    }

    private static void testHorizontalAcceleration() {
        float vx = MovementMath.computeHorizontalVelocity(
            0.0f,
            1,
            11.25f,
            125.0f,
            50.0f,
            0.65f,
            true,
            0.05f
        );

        assertEqual(6.25f, vx, 0.001f, "Horizontal acceleration below MaxRun");
        System.out.println("[PASS] testHorizontalAcceleration");
    }

    private static void testHyperMomentumRetention() {
        float hyperVx = 40.625f;
        float nextVx = MovementMath.computeHorizontalVelocity(
            hyperVx,
            1,
            11.25f,
            125.0f,
            50.0f,
            0.65f,
            false,
            1.0f / 60.0f
        );

        float expectedDecay = 50.0f * 0.65f * (1.0f / 60.0f);
        assertEqual(hyperVx - expectedDecay, nextVx, 0.001f, "Over-max momentum uses RunReduce rate");
        assertTrue(nextVx > 40.0f, "Hyper speed retained smoothly");
        System.out.println("[PASS] testHyperMomentumRetention");
    }

    private static void testEightWayAimQuantization() {
        Vector2 fallback = MovementMath.quantizeEightWayAim(new Vector2(0.0f, 0.0f), -1, 0.25f);
        assertEqual(-1.0f, fallback.x, 0.001f, "Fallback aim facing left");
        assertEqual(0.0f, fallback.y, 0.001f, "Fallback aim facing y");

        Vector2 downRight = MovementMath.quantizeEightWayAim(new Vector2(0.8f, -0.7f), 1, 0.25f);
        float invSqrt2 = 1.0f / (float) Math.sqrt(2.0);
        assertEqual(invSqrt2, downRight.x, 0.001f, "Diagonal snap X");
        assertEqual(-invSqrt2, downRight.y, 0.001f, "Diagonal snap Y");
        System.out.println("[PASS] testEightWayAimQuantization");
    }

    private static void testStaminaDrainOnClimbJumps() {
        StaminaSystem stamina = new StaminaSystem();
        stamina.configure(110.0f, 45.4545f, 10.0f, 27.5f, 20.0f);

        for (int i = 0; i < 3; i++) {
            stamina.drainClimbJump();
            assertTrue(!stamina.isExhausted(), "Not exhausted after hop " + (i + 1));
        }

        stamina.drainClimbJump();
        assertEqual(0.0f, stamina.getCurrentStamina(), 0.001f, "Full exhaustion after 4 climb jumps");
        assertTrue(stamina.isExhausted(), "Stamina exhausted flag");

        stamina.refill();
        assertEqual(110.0f, stamina.getCurrentStamina(), 0.001f, "Stamina refilled");
        System.out.println("[PASS] testStaminaDrainOnClimbJumps");
    }

    private static void testDashManagerWavedashRefillWindow() {
        DashManager dashes = new DashManager();
        dashes.configure(1, 0.20f, 0.10f, 0.30f, 0.05f);

        assertTrue(dashes.tryConsumeDash(), "Dash consumed");
        assertEqual(0, dashes.getCurrentDashes(), 0.0f, "0 dashes left");
        assertTrue(dashes.isFrozen(), "Dash frozen");

        dashes.tick(0.05f);
        assertTrue(!dashes.isFrozen(), "Unfrozen after freeze time");
        assertTrue(!dashes.tryRefillOnGround(), "Cannot refill during refill cooldown");

        dashes.tick(0.11f);
        assertTrue(dashes.inDashAttack(), "Still inside dash attack window");
        assertTrue(dashes.tryRefillOnGround(), "Ground refill succeeded");
        assertEqual(1, dashes.getCurrentDashes(), 0.0f, "Dash refilled for Wavedash");
        System.out.println("[PASS] testDashManagerWavedashRefillWindow");
    }

    private static void assertEqual(float expected, float actual, float tolerance, String msg) {
        if (Math.abs(expected - actual) > tolerance) {
            throw new AssertionError(msg + " - Expected: " + expected + ", Actual: " + actual);
        }
    }

    private static void assertTrue(boolean condition, String msg) {
        if (!condition) {
            throw new AssertionError("Assertion failed: " + msg);
        }
    }
}
