package com.Ruinfall.frontend;

import com.Ruinfall.frontend.core.MovementMath;
import com.Ruinfall.frontend.data.PlayerMovementStats;
import com.Ruinfall.frontend.objects.Interactables.Barrels;
import com.Ruinfall.frontend.objects.Interactables.GrapplePoint;
import com.Ruinfall.frontend.objects.Interactables.Rock;
import com.Ruinfall.frontend.objects.Interactables.Stalactite;
import com.Ruinfall.frontend.objects.enemies.WaveMantis;
import com.Ruinfall.frontend.objects.items.Item;
import com.Ruinfall.frontend.objects.items.ItemType;
import com.Ruinfall.frontend.subsystems.DashManager;
import com.Ruinfall.frontend.subsystems.StaminaSystem;
import com.Ruinfall.frontend.visuals.CameraFollow;
import com.Ruinfall.frontend.visuals.ParticleSystem;
import com.Ruinfall.frontend.world.LushCaveStage;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

public class MovementTests {
    public static void main(String[] args) {
        testHorizontalAcceleration();
        testHyperMomentumRetention();
        testEightWayAimQuantization();
        testStaminaDrainOnClimbJumps();
        testDashManagerWavedashRefillWindow();
        testStage1CriticalPathReachAndSafety();
        testRoom3GrappleTrajectories60Hz();
        testCameraFollowDeadzoneLookAheadSingleScreenAndSnap();
        testRoomResetAndBossArenaRetry();
        testParticleSystemPoolLifecycle();

        System.out.println("ALL 10 LIBGDX MOVEMENT & STAGE 1 TESTS PASSED!");
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

    private static void testStage1CriticalPathReachAndSafety() {
        PlayerMovementStats stats = new PlayerMovementStats();
        float dt = 1.0f / 60.0f;

        // Simulate exact 60Hz full-hold jump using NormalState rules
        float vy = stats.jumpSpeed;
        float vx = stats.maxRun + stats.jumpHBoost;
        float y = 0f;
        float x = 0f;
        float maxY = 0f;
        float varJumpTimer = stats.varJumpTime;

        for (int step = 0; step < 120; step++) {
            varJumpTimer = Math.max(0f, varJumpTimer - dt);
            if (varJumpTimer > 0f) {
                vy = Math.max(vy, stats.jumpSpeed);
            } else {
                float gravMult = (Math.abs(vy) < stats.halfGravThreshold) ? 0.5f : 1.0f;
                vy = MovementMath.approach(vy, -stats.maxFall, stats.gravity * gravMult * dt);
            }
            vx = MovementMath.computeHorizontalVelocity(vx, 1, stats.maxRun, stats.runAccel, stats.runReduce, stats.airMult, false, dt);
            x += vx * dt;
            y += vy * dt;
            if (y > maxY) maxY = y;
            if (y <= 0f && vy < 0f) {
                break;
            }
        }

        float realVertReach = maxY;
        float realHorizReach = x;
        float realClimbReach = stats.climbUpSpeed * (stats.climbMaxStamina / stats.climbUpCost);

        assertTrue(realVertReach > 3.2f && realVertReach < 3.8f, "Measured 60Hz vertical reach reasonable: " + realVertReach);
        assertTrue(realHorizReach > 7.2f, "Measured 60Hz horizontal reach reasonable: " + realHorizReach);

        LushCaveStage stage = new LushCaveStage();

        // 1. Room 1 opening jump over water pool [6.0, 14.0] must be 100% spike-free
        Rectangle room1OpeningZone = new Rectangle(5.5f, -3.5f, 9.0f, 6.0f);
        for (LushCaveStage.SpikeHazard spike : stage.getSpikes()) {
            assertTrue(!spike.bounds.overlaps(room1OpeningZone), "Room 1 opening jump must have no spikes");
        }

        // 2. Room 1 stepping stone (top 2.0 vs floor 0.0) & basin rim (0.0 vs -1.8) <= 0.7x vertical reach
        assertTrue(2.0f <= 0.70f * realVertReach, "Room 1 stepping stone <= 0.7x vertical reach");
        assertTrue(1.8f <= 0.70f * realVertReach, "Room 1 water basin exit <= 0.7x vertical reach");
        assertTrue(2.0f <= 0.70f * realHorizReach, "Room 1 stepping stone gap <= 0.7x horizontal reach");

        // 3. Room 2 wall climb (7.2) & alcove (4.5) <= 0.7x climb reach, horizontal gaps <= 0.7x
        assertTrue(7.2f <= 0.70f * realClimbReach, "Room 2 climb wall <= 0.7x climb reach");
        assertTrue(4.5f <= 0.70f * realClimbReach, "Room 2 alcove <= 0.7x climb reach");
        assertTrue(2.6f <= 0.70f * realHorizReach, "Room 2 top gap <= 0.7x horizontal reach");
        assertTrue(1.9f <= 0.70f * realHorizReach, "Room 2->3 entry gap <= 0.7x horizontal reach");

        // 4. Room 3 spawn inside [38.0, 58.0) and grapple points inside viewport & within 5.5f reach
        LushCaveStage.Room room3 = stage.getRooms().get(2);
        assertTrue(room3.spawnX >= room3.minX && room3.spawnX < room3.maxX,
                "Room 3 spawnX must be inside Room 3 bounds");
        GrapplePoint gp1 = stage.getGrapplePoints().get(0);
        GrapplePoint gp2 = stage.getGrapplePoints().get(1);
        assertTrue(gp1.getY() >= room3.minY && gp1.getY() + gp1.getHeight() <= room3.maxY,
                "GP1 must lie inside Room 3 vertical viewport");
        assertTrue(gp2.getY() >= room3.minY && gp2.getY() + gp2.getHeight() <= room3.maxY,
                "GP2 must lie inside Room 3 vertical viewport");
        float playerHalfH = stats.normalColliderSize.y * 0.5f;
        float distEntry386ToGp1 = (float) Math.hypot(38.6f - gp1.getCenter().x, (7.3f + playerHalfH) - gp1.getCenter().y);
        float distEntry393ToGp1 = (float) Math.hypot(39.3f - gp1.getCenter().x, (7.3f + playerHalfH) - gp1.getCenter().y);
        assertTrue(distEntry386ToGp1 <= gp1.getGrappleRadius(), "GP1 reachable directly from Room 3 spawn (38.6, 7.3): " + distEntry386ToGp1);
        assertTrue(distEntry393ToGp1 <= gp1.getGrappleRadius(), "GP1 reachable directly from Room 3 perch edge (39.3, 7.3): " + distEntry393ToGp1);
        for (float islandX : new float[]{47.5f, 47.9f, 48.5f, 48.8f, 49.2f, 49.5f, 49.8f}) {
            float distToGp2 = (float) Math.hypot(islandX - gp2.getCenter().x, (3.2f + playerHalfH) - gp2.getCenter().y);
            assertTrue(distToGp2 <= gp2.getGrappleRadius(),
                    "GP2 reachable directly from central island (" + islandX + ", 3.2): " + distToGp2 + " > " + gp2.getGrappleRadius());
        }

        // 5. Room 4 step back (2.2) <= 0.7x vertical reach
        assertTrue(2.2f <= 0.70f * realVertReach, "Room 4 ledge step <= 0.7x vertical reach");

        System.out.println("[PASS] testStage1CriticalPathReachAndSafety (vert=" + realVertReach + ", horiz=" + realHorizReach + ")");
    }

    private static void testRoom3GrappleTrajectories60Hz() {
        // 1a. Direct E + holding D from entry perch (39.3, 7.3) and (38.6, 7.3) onto GP1 -> lands on central island [46.0, 49.8]
        Vector2 landGp1From393 = simulateRoom3Grapple(39.3f, 7.3f, 0, 0, false);
        assertTrue(landGp1From393.x >= 45.65f && landGp1From393.x <= 50.15f && Math.abs(landGp1From393.y - 3.2f) < 0.05f,
                "GP1 direct E+D from (39.3, 7.3) must land on central island [46.0, 49.8], got " + landGp1From393);

        Vector2 landGp1From386 = simulateRoom3Grapple(38.6f, 7.3f, 0, 0, false);
        assertTrue(landGp1From386.x >= 45.65f && landGp1From386.x <= 50.15f && Math.abs(landGp1From386.y - 3.2f) < 0.05f,
                "GP1 direct E+D from (38.6, 7.3) must land on central island [46.0, 49.8], got " + landGp1From386);

        // 1b. Neutral E without D from entry perch (38.6, 7.3) and (39.3, 7.3) onto GP1 -> settles grounded on central island
        for (float startX : new float[]{38.6f, 39.3f}) {
            Vector2 neutralGp1 = simulateRoom3GrappleEx(startX, 7.3f, 0, 0, false, 999, false, -1, false);
            assertTrue(neutralGp1.x >= 45.65f && neutralGp1.x <= 50.15f && Math.abs(neutralGp1.y - 3.2f) < 0.06f,
                    "GP1 neutral E (no D) from (" + startX + ", 7.3) must settle on central island, got " + neutralGp1);
        }

        // 2a. Direct E + D from across central island (47.9, 48.5, 48.8, 49.2, 49.5, 49.8) onto GP2 -> lands cleanly past 58.0 on Room 4
        Vector2 landGp2From493 = simulateRoom3Grapple(49.3f, 3.2f, 0, 0, false);
        assertTrue(landGp2From493.x >= 58.0f && landGp2From493.x <= 64.8f && Math.abs(landGp2From493.y - 2.2f) < 0.05f,
                "GP2 direct E+D from (49.3, 3.2) must land solidly on Room 4 [58.0, 64.5], got " + landGp2From493);
        for (float islandX : new float[]{47.9f, 48.5f, 48.8f, 49.2f, 49.5f, 49.8f}) {
            Vector2 landGp2 = simulateRoom3GrappleEx(islandX, 3.2f, 0, 0, true, -3, false, -1, false);
            assertTrue(landGp2.x >= 58.0f && landGp2.x <= 64.8f && Math.abs(landGp2.y - 2.2f) < 0.05f,
                    "GP2 direct E+D from (" + islandX + ", 3.2) must land solidly on Room 4 [58.0, 64.5], got " + landGp2);
        }

        // 2b. Neutral E without D & early D-release (release on end & f15..f30) on GP2 -> lands cleanly on Room 4 [58.0, 64.5]
        for (float islandX : new float[]{47.9f, 48.8f, 49.3f, 49.8f}) {
            Vector2 neutralGp2 = simulateRoom3GrappleEx(islandX, 3.2f, 0, 0, false, 999, false, -1, false);
            assertTrue(neutralGp2.x >= 58.0f && neutralGp2.x <= 64.8f && Math.abs(neutralGp2.y - 2.2f) < 0.05f,
                    "GP2 neutral E (no D) from (" + islandX + ", 3.2) must settle on Room 4 [58.0, 64.5], got " + neutralGp2);
        }
        for (int relFrame : new int[]{-2, 15, 20, 25, 30}) {
            Vector2 earlyRelGp2 = simulateRoom3GrappleEx(49.3f, 3.2f, 0, 0, true, relFrame, false, -1, false);
            assertTrue(earlyRelGp2.x >= 58.0f && earlyRelGp2.x <= 64.8f && Math.abs(earlyRelGp2.y - 2.2f) < 0.05f,
                    "GP2 early D-release (" + relFrame + ") from (49.3, 3.2) must land on Room 4, got " + earlyRelGp2);
        }

        // 3a. Jump -> E from entry perch (39.3, 7.3) onto GP1 -> clears onto central island without spike hit
        Vector2 landGp1JumpE = simulateRoom3Grapple(39.3f, 7.3f, 5, 5, false);
        assertTrue(landGp1JumpE.x >= 45.605f && landGp1JumpE.x <= 50.15f && Math.abs(landGp1JumpE.y - 3.2f) < 0.05f,
                "GP1 Jump->E from (39.3, 7.3) must land on central island, got " + landGp1JumpE);

        // 3b. Jump -> E from central island (49.3, 3.2) onto GP2 -> clears onto Room 4 ledge without spike hit
        Vector2 landGp2JumpE = simulateRoom3Grapple(49.3f, 3.2f, 8, 8, false);
        assertTrue(landGp2JumpE.x >= 57.605f && landGp2JumpE.x <= 64.8f && Math.abs(landGp2JumpE.y - 2.2f) < 0.05f,
                "GP2 Jump->E from (49.3, 3.2) must land on Room 4 ledge, got " + landGp2JumpE);

        // 3c. JumpCancel (Space during grapple) on GP1 (f2..f6) & GP2 (f7..f13) -> lands safely without spike hits
        for (int cancelFrame = 2; cancelFrame <= 6; cancelFrame++) {
            Vector2 gp1Cancel = simulateRoom3GrappleEx(39.3f, 7.3f, 0, 0, true, -3, false, cancelFrame, false);
            assertTrue(gp1Cancel.x >= 45.65f && gp1Cancel.x <= 50.15f && Math.abs(gp1Cancel.y - 3.2f) < 0.05f,
                    "GP1 JumpCancel@f" + cancelFrame + " must land on central island, got " + gp1Cancel);
        }
        for (int cancelFrame = 7; cancelFrame <= 13; cancelFrame++) {
            Vector2 gp2Cancel = simulateRoom3GrappleEx(49.3f, 3.2f, 0, 0, true, -3, false, cancelFrame, false);
            assertTrue(gp2Cancel.x >= 57.65f && gp2Cancel.x <= 64.8f && Math.abs(gp2Cancel.y - 2.2f) < 0.05f,
                    "GP2 JumpCancel@f" + cancelFrame + " from 49.3 must land on Room 4, got " + gp2Cancel);
        }

        // 4a. Grapple -> Dash from central island (49.3, 3.2) onto GP2 -> clears onto Room 4 without spike hit
        Vector2 landGp2Dash = simulateRoom3Grapple(49.3f, 3.2f, 0, 0, true);
        assertTrue(landGp2Dash.x >= 57.605f && landGp2Dash.y >= 0.0f,
                "GP2 Grapple->Dash from (49.3, 3.2) must clear onto Room 4 cleanly, got " + landGp2Dash);

        // 4b. Grapple -> Dash from entry perch (39.3, 7.3) onto GP1 -> clears onto central island cleanly
        Vector2 landGp1DashIsland = simulateRoom3Grapple(39.3f, 7.3f, 0, 0, true, true);
        assertTrue(landGp1DashIsland.x >= 45.605f && landGp1DashIsland.x <= 50.15f && Math.abs(landGp1DashIsland.y - 3.2f) < 0.05f,
                "GP1 Grapple->DownRightDash from (39.3, 7.3) must land on central island, got " + landGp1DashIsland);

        System.out.println("[PASS] testRoom3GrappleTrajectories60Hz (GP1@39.3=" + landGp1From393
                + ", GP1@38.6=" + landGp1From386 + ", GP2@49.3=" + landGp2From493
                + ", GP1_JumpE=" + landGp1JumpE + ", GP2_JumpE=" + landGp2JumpE
                + ", GP2_Dash=" + landGp2Dash + ", GP1_Dash=" + landGp1DashIsland + ")");
    }

    private static Vector2 simulateRoom3Grapple(float startX, float startY, int jumpFrames, int grappleAtFrame, boolean dashAfterRelease) {
        return simulateRoom3Grapple(startX, startY, jumpFrames, grappleAtFrame, dashAfterRelease, false);
    }

    private static Vector2 simulateRoom3Grapple(float startX, float startY, int jumpFrames, int grappleAtFrame,
                                                boolean dashAfterRelease, boolean dashDownRight) {
        return simulateRoom3GrappleEx(startX, startY, jumpFrames, grappleAtFrame, true, -3, dashAfterRelease, -1, dashDownRight);
    }

    // releaseDFrame: 999=never, -2=onGrappleEnd, -3=onFirstLand, >=0=at frame
    private static Vector2 simulateRoom3GrappleEx(float startX, float startY, int jumpFrames, int grappleAtFrame,
                                                  boolean holdD, int releaseDFrame, boolean dashAfterRelease,
                                                  int jumpCancelFrame, boolean dashDownRight) {
        LushCaveStage stage = new LushCaveStage();
        PlayerController player = new PlayerController();
        player.getMotor().setSolids(stage.getBlocks());
        player.respawnAt(startX, startY);

        final Set<Integer> heldKeys = new HashSet<>();
        final Set<Integer> justPressedKeys = new HashSet<>();
        Input prevInput = Gdx.input;
        Gdx.input = (Input) Proxy.newProxyInstance(
                Input.class.getClassLoader(),
                new Class<?>[]{Input.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("isKeyPressed".equals(name) && args != null && args.length == 1) {
                        return heldKeys.contains((Integer) args[0]);
                    }
                    if ("isKeyJustPressed".equals(name) && args != null && args.length == 1) {
                        return justPressedKeys.contains((Integer) args[0]);
                    }
                    Class<?> ret = method.getReturnType();
                    if (ret == boolean.class) return false;
                    if (ret == int.class) return 0;
                    if (ret == float.class) return 0f;
                    return null;
                }
        );

        try {
            float dt = 1.0f / 60.0f;
            boolean grappleStarted = false;
            boolean grappleFinished = false;
            int grappleStartFrame = -1;
            int firstLandFrame = -1;
            Vector2 firstLandPos = null;
            boolean dReleased = false;
            int dashFrame = -1;

            for (int frame = 0; frame < 180; frame++) {
                heldKeys.clear();
                justPressedKeys.clear();

                if (releaseDFrame >= 0 && frame >= releaseDFrame) dReleased = true;
                if (releaseDFrame == -2 && grappleFinished) dReleased = true;
                if (releaseDFrame == -3 && firstLandFrame >= 0) dReleased = true;

                if (holdD && !dReleased) {
                    heldKeys.add(Input.Keys.D);
                }
                if (frame < jumpFrames) {
                    heldKeys.add(Input.Keys.SPACE);
                    if (frame == 0) justPressedKeys.add(Input.Keys.SPACE);
                }
                if (frame == grappleAtFrame) {
                    heldKeys.add(Input.Keys.E);
                    justPressedKeys.add(Input.Keys.E);
                }
                if (jumpCancelFrame >= 0 && grappleStarted && !grappleFinished && (frame - grappleStartFrame) == jumpCancelFrame) {
                    heldKeys.add(Input.Keys.SPACE);
                    justPressedKeys.add(Input.Keys.SPACE);
                }
                if (dashAfterRelease && grappleFinished && dashFrame < 0) {
                    justPressedKeys.add(Input.Keys.SHIFT_LEFT);
                    dashFrame = frame;
                }
                if (dashDownRight && dashFrame >= 0 && frame <= dashFrame + 5) {
                    heldKeys.add(Input.Keys.S);
                }

                float px = player.getMotor().getPosition().x;
                float py = player.getMotor().getPosition().y + player.getStats().normalColliderSize.y * 0.5f;
                GrapplePoint nearest = null;
                float bestDist = Float.MAX_VALUE;
                for (GrapplePoint gp : stage.getGrapplePoints()) {
                    gp.update(dt);
                    gp.checkPlayerRange(px, py);
                    if (gp.canGrapple()) {
                        float d = (float) Math.hypot(px - gp.getX(), py - gp.getY());
                        if (d < bestDist) {
                            bestDist = d;
                            nearest = gp;
                        }
                    }
                }
                if (nearest != null) {
                    player.setNearbyGrapplePoint(nearest);
                } else if (player.getNearbyGrapplePoint() != null
                        && player.getNearbyGrapplePoint().getState() != GrapplePoint.GrappleState.ACTIVE) {
                    player.setNearbyGrapplePoint(null);
                }

                player.updateInput();
                player.stepPhysics(dt);

                boolean inGrapple = player.getStateMachine().getCurrentState() == player.getGrappleState();
                if (inGrapple && !grappleStarted) {
                    grappleStarted = true;
                    grappleStartFrame = frame;
                } else if (!inGrapple && grappleStarted) {
                    grappleFinished = true;
                }

                Rectangle core = player.getCoreHitbox();
                for (LushCaveStage.SpikeHazard spike : stage.getSpikes()) {
                    assertTrue(!core.overlaps(spike.bounds),
                            "Player hit spike at frame " + frame + " pos=" + player.getMotor().getPosition());
                }

                if (grappleFinished && firstLandFrame < 0 && player.isGrounded() && frame > grappleAtFrame + 5) {
                    firstLandFrame = frame;
                    firstLandPos = new Vector2(player.getMotor().getPosition());
                }
            }

            assertTrue(grappleStarted, "Grapple never started from (" + startX + ", " + startY + ")");
            assertTrue(firstLandPos != null, "Player did not land cleanly after grapple from (" + startX + ", " + startY
                    + "), final pos=" + player.getMotor().getPosition());
            assertTrue(player.isGrounded(), "Player did not remain grounded after landing from (" + startX + ", " + startY
                    + "), final pos=" + player.getMotor().getPosition());
            return firstLandPos;
        } finally {
            Gdx.input = prevInput;
        }
    }

    private static void testCameraFollowDeadzoneLookAheadSingleScreenAndSnap() {
        LushCaveStage stage = new LushCaveStage();
        PlayerController player = new PlayerController();
        LushCaveStage.Room room1 = stage.getRooms().get(0);
        player.getMotor().getPosition().set(0.0f, 0.0f);
        player.setFacing(1);

        CameraFollow cf = new CameraFollow(24.89f, 14.0f, player);
        cf.setInitialRoom(room1);

        // 1. Calm Deadzone: small movement within deadzone does not move deadzoneFocus
        Vector2 startFocus = new Vector2(cf.getDeadzoneFocus());
        player.getMotor().getPosition().set(0.9f, 0.6f);
        cf.update(1.0f / 60.0f);
        assertEqual(startFocus.x, cf.getDeadzoneFocus().x, 0.001f, "Deadzone holds X on small movement");
        assertEqual(startFocus.y, cf.getDeadzoneFocus().y, 0.001f, "Deadzone holds Y on small movement");

        // 2. Smoothly eased look-ahead without direction-flip whip
        for (int i = 0; i < 90; i++) {
            cf.update(1.0f / 60.0f);
        }
        assertTrue(cf.getCurrentLookAheadX() > 1.30f && cf.getCurrentLookAheadX() <= 1.41f,
                "Holding right eases lookAhead smoothly toward +1.4");

        // Rapid left-right taps (< 0.18s hysteresis) cause zero whip
        player.setFacing(-1);
        cf.update(0.05f);
        player.setFacing(1);
        cf.update(0.05f);
        player.setFacing(-1);
        cf.update(0.05f);
        assertTrue(cf.getCurrentLookAheadX() > 1.30f, "Quick taps do not whip lookAheadX: " + cf.getCurrentLookAheadX());

        // Holding left past hysteresis eases smoothly toward -1.4 without snapping
        player.setFacing(-1);
        for (int i = 0; i < 15; i++) {
            cf.update(1.0f / 60.0f);
        }
        assertTrue(cf.getCurrentLookAheadX() > 0.0f && cf.getCurrentLookAheadX() < 1.35f,
                "LookAhead eases gradually after flip rather than snapping: " + cf.getCurrentLookAheadX());
        for (int i = 0; i < 90; i++) {
            cf.update(1.0f / 60.0f);
        }
        assertTrue(cf.getCurrentLookAheadX() < -1.25f, "Sustained left hold reaches negative lookAhead");

        // 3. Single-screen rooms (Rooms 2, 3, 4) lock strictly centered on both X and Y
        for (int rIdx = 1; rIdx <= 3; rIdx++) {
            LushCaveStage.Room r = stage.getRooms().get(rIdx);
            assertTrue(r.singleScreen, "Room " + (rIdx + 1) + " should be singleScreen");
            assertEqual(14.0f, r.maxY - r.minY, 0.01f, "Single-screen room height matches 14-unit viewport");
            cf.setInitialRoom(r);
            player.getMotor().getPosition().set(r.minX + 1.0f, r.minY + 1.0f);
            cf.update(0.5f);
            float expectedX = (r.minX + r.maxX) * 0.5f;
            float expectedY = (r.minY + r.maxY) * 0.5f;
            assertEqual(expectedX, cf.getCameraPos().x, 0.001f, "Single-screen X locked centered");
            assertEqual(expectedY, cf.getCameraPos().y, 0.001f, "Single-screen Y locked centered");

            player.getMotor().getPosition().set(r.maxX - 1.0f, r.maxY - 1.0f);
            cf.update(0.5f);
            assertEqual(expectedX, cf.getCameraPos().x, 0.001f, "Single-screen X zero drift");
            assertEqual(expectedY, cf.getCameraPos().y, 0.001f, "Single-screen Y zero drift");
        }

        // Room 4 tutorial stalactite fits inside Room 4 locked viewport
        LushCaveStage.Room room4 = stage.getRooms().get(3);
        Stalactite room4Stal = stage.getStalactites().get(0);
        assertTrue(room4Stal.getY() >= room4.minY && room4Stal.getY() + room4Stal.getHeight() <= room4.maxY,
                "Room 4 tutorial stalactite fits inside locked viewport");

        // 4. Zero slingshot on snapToTarget()
        LushCaveStage.Room room5 = stage.getRooms().get(4);
        cf.transitionToRoom(room5);
        cf.addTrauma(0.9f);
        assertTrue(cf.isTransitioning(), "Transition started");
        player.getMotor().getPosition().set(room5.spawnX, room5.spawnY);
        cf.snapToTarget();
        assertTrue(!cf.isTransitioning(), "snapToTarget clears transitioning");
        assertEqual(0.0f, cf.getCurrentLookAheadX(), 0.001f, "snapToTarget resets lookAheadX");
        assertEqual(cf.getBaseCamPos().x, cf.getCameraPos().x, 0.001f, "snapToTarget clears shake offset X");

        System.out.println("[PASS] testCameraFollowDeadzoneLookAheadSingleScreenAndSnap");
    }

    private static void testRoomResetAndBossArenaRetry() {
        LushCaveStage stage = new LushCaveStage();
        PlayerController player = new PlayerController();

        // 1. Player respawnAt refills dashes, stamina, HP/state
        player.getDashes().tryConsumeDash();
        player.getStamina().drainClimbJump();
        player.getStateMachine().changeState(player.getDashState());
        player.respawnAt(38.6f, 7.6f);
        assertEqual(1, player.getDashes().getCurrentDashes(), 0.0f, "respawnAt refills dash");
        assertEqual(110.0f, player.getStamina().getCurrentStamina(), 0.01f, "respawnAt refills stamina");
        assertTrue(player.getStateMachine().getCurrentState() == player.getNormalState(), "respawnAt restores NormalState");
        assertEqual(38.6f, player.getMotor().getPosition().x, 0.001f, "respawnAt sets X");

        // 2. Room 4 breakables & stalactites reset
        LushCaveStage.Room room4 = stage.getRooms().get(3);
        Rock rock = stage.getBreakables().get(0);
        Barrels barrel = (Barrels) stage.getBreakables().get(1);
        Stalactite room4Stal = stage.getStalactites().get(0);
        rock.takeHit(5);
        barrel.takeHit(5);
        room4Stal.trigger();
        room4Stal.onGroundHit();
        assertTrue(!rock.isActive() && !barrel.isActive() && !room4Stal.isActive(), "Room 4 interactables destroyed");
        stage.resetRoomInteractables(room4);
        assertTrue(rock.isActive() && barrel.isActive() && room4Stal.isActive(), "resetRoomInteractables restores Room 4");
        assertEqual(2, rock.getHealth(), 0.0f, "Rock health restored");
        assertEqual(3, barrel.getHealth(), 0.0f, "Barrel health restored");

        // 3. Player HP + LIFE item heal
        assertEqual(5, player.getHp(), 0.0f, "Player starts with 5 HP");
        player.takeDamage(2);
        assertEqual(3, player.getHp(), 0.0f, "Player HP decremented");
        Item lifeItem = new Item(0f, 0f, ItemType.LIFE);
        lifeItem.applyBonus(player);
        assertEqual(4, player.getHp(), 0.0f, "LIFE item heals +1 HP");
        player.resetHealth();
        assertEqual(5, player.getHp(), 0.0f, "resetHealth restores max HP");

        // 4. WaveMantis parry -> daze -> stalactite crush & reset
        WaveMantis boss = new WaveMantis(92.0f, 0.0f);
        stage.lockArena();
        assertTrue(stage.isArenaLocked(), "Arena locked");
        boss.activate();
        // Advance until DASH_ATTACK
        for (int i = 0; i < 160 && boss.getState() != WaveMantis.State.DASH_ATTACK; i++) {
            boss.updateBoss(1.0f / 60.0f, 78.0f, 0.0f);
        }
        assertTrue(boss.getState() == WaveMantis.State.DASH_ATTACK, "Boss enters DASH_ATTACK");
        boolean parried = boss.tryParry(boss.getCoreHitbox());
        assertTrue(parried, "Parry succeeds during DASH_ATTACK");
        assertTrue(boss.getState() == WaveMantis.State.DAZED, "Parry puts boss into DAZED");
        assertTrue(boss.onStalactiteHit(), "Stalactite damages dazed boss");
        assertEqual(65, boss.getHp(), 0.0f, "Dazed stalactite deals 35 damage");

        // Reset boss & arena
        boss.reset();
        stage.resetArena();
        assertTrue(boss.isAlive(), "Boss alive after reset");
        assertEqual(100, boss.getHp(), 0.0f, "Boss HP restored after reset");
        assertTrue(boss.getState() == WaveMantis.State.SLEEPING, "Boss back to SLEEPING");
        assertEqual(92.0f, boss.getX(), 0.001f, "Boss X restored");
        assertTrue(!stage.isArenaLocked(), "Arena gate unlocked on resetArena");

        System.out.println("[PASS] testRoomResetAndBossArenaRetry");
    }

    private static void testParticleSystemPoolLifecycle() {
        ParticleSystem ps = new ParticleSystem();
        assertEqual(0, ps.getActiveCount(), 0.0f, "ParticleSystem starts empty");
        ps.emitSpark(0f, 0f, 1f, 1f, 0.2f, 0.2f, Color.CYAN, Color.WHITE, true);
        ps.emitDust(0f, 0f, -1f, 0.5f, 0.2f, 0.2f, Color.WHITE);
        assertEqual(2, ps.getActiveCount(), 0.0f, "Particles spawned into pool");
        ps.update(0.1f);
        assertEqual(2, ps.getActiveCount(), 0.0f, "Particles stay alive mid-life");
        ps.update(0.15f);
        assertEqual(0, ps.getActiveCount(), 0.0f, "Expired particles reclaimed by pool");

        // Saturation ring-overwrite check: fill pool with 0.2s particles, then burst 512 1.0s particles
        for (int i = 0; i < ParticleSystem.MAX_PARTICLES; i++) {
            ps.emitSpark(0f, 0f, 0f, 0f, 0.1f, 0.2f, Color.CYAN, Color.WHITE, false);
        }
        assertEqual(ParticleSystem.MAX_PARTICLES, ps.getActiveCount(), 0.0f, "Pool capped at MAX_PARTICLES");
        for (int i = 0; i < 512; i++) {
            ps.emitSpark(0f, 0f, 0f, 0f, 0.1f, 1.0f, Color.GOLD, Color.WHITE, false);
        }
        assertEqual(ParticleSystem.MAX_PARTICLES, ps.getActiveCount(), 0.0f, "Pool stays capped on overflow");
        ps.update(0.3f);
        assertEqual(512, ps.getActiveCount(), 0.0f, "Ring overwrite cursor preserved all 512 newer burst particles");
        ps.update(0.8f);
        assertEqual(0, ps.getActiveCount(), 0.0f, "All particles decay cleanly to 0");
        System.out.println("[PASS] testParticleSystemPoolLifecycle");
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
