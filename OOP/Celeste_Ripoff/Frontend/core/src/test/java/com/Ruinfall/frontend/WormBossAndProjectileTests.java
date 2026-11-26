package com.Ruinfall.frontend;

import com.Ruinfall.frontend.objects.Interactables.Barrels;
import com.Ruinfall.frontend.objects.Interactables.GrapplePoint;
import com.Ruinfall.frontend.objects.Interactables.RegeneratingStalactite;
import com.Ruinfall.frontend.objects.items.Item;
import com.Ruinfall.frontend.objects.Player;
import com.Ruinfall.frontend.objects.bullets.Bullet;
import com.Ruinfall.frontend.objects.enemies.Boss;
import com.Ruinfall.frontend.objects.enemies.WormBoss;
import com.Ruinfall.frontend.objects.enemies.WormSegment;
import com.Ruinfall.frontend.objects.hazards.SlimeHazard;
import com.Ruinfall.frontend.objects.patterns.projectile.BubbleBullet;
import com.Ruinfall.frontend.objects.patterns.projectile.LinearBullet;
import com.Ruinfall.frontend.objects.patterns.projectile.RadialBurst;
import com.Ruinfall.frontend.objects.patterns.projectile.SingleShot;
import com.Ruinfall.frontend.objects.patterns.projectile.SpreadShot;
import com.Ruinfall.frontend.objects.patterns.projectile.SpiralStream;
import com.Ruinfall.frontend.states.SwingState;
import com.Ruinfall.frontend.systems.BulletManager;
import com.Ruinfall.frontend.visuals.CameraShake;
import com.Ruinfall.frontend.visuals.EffectManager;
import com.Ruinfall.frontend.visuals.HitstopManager;
import com.Ruinfall.frontend.visuals.ParticleSystem;
import com.Ruinfall.frontend.world.BoxRoom;
import com.Ruinfall.frontend.world.OneWayPlatform;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.util.List;

public class WormBossAndProjectileTests {
    public static void main(String[] args) {
        testShootingPatterns();
        testLinearBulletLifecycleAndPlayerHit();
        testBubbleBulletDragTtlAndCardinalBurst();
        testWormBossSegmentationAndSharedHp();
        testWormBossTerrainDependentSteering();
        testWormBossThreeStagesAndCleanup();
        testUpDashAndDiagonalDash();
        testDiagonalSwingAndPogo();
        testTwoPlatformArenaOneWayAndDropThrough();
        testDashCooldownNerfBarrelsAndRegeneratingStalactite();
        testVfxParticlePoolShakeHitstopAndEvents();
        testExpandedArenaGrappleNetworkStalactiteRandomizationAndBarrelPhysics();

        System.out.println("ALL WORM BOSS & PROJECTILE TESTS PASSED!");
    }

    private static void testShootingPatterns() {
        BulletManager bm = new BulletManager();
        LinearBullet linear = new LinearBullet();

        SingleShot single = new SingleShot(0.5f, 10f, 2);
        single.execute(0f, 0f, 10f, 0f, bm, linear, false);
        assertEqual(1, bm.getActiveBullets().size(), "SingleShot spawns 1 bullet");
        assertNear(10f, bm.getActiveBullets().get(0).getVx(), 0.01f, "SingleShot vx toward +X");
        bm.clearAll();

        SpreadShot spread = new SpreadShot(0.5f, 10f, 1, 5, 60f);
        spread.execute(0f, 0f, 0f, 10f, bm, linear, false);
        assertEqual(5, bm.getActiveBullets().size(), "SpreadShot spawns 5 bullets");
        bm.clearAll();

        RadialBurst radial = new RadialBurst(0.5f, 8f, 1, 8);
        radial.execute(0f, 0f, 0f, 0f, bm, linear, false);
        assertEqual(8, bm.getActiveBullets().size(), "RadialBurst spawns 8 bullets");
        bm.clearAll();

        SpiralStream spiral = new SpiralStream(0.1f, 8f, 1, 4, 20f);
        spiral.update(0.1f, 0f, 0f, 0f, 0f, bm, linear, false);
        assertEqual(4, bm.getActiveBullets().size(), "SpiralStream spawns 4 arms on cadence");
        float firstAngle = bm.getActiveBullets().get(0).getAngle();
        bm.clearAll();
        spiral.update(0.1f, 0f, 0f, 0f, 0f, bm, linear, false);
        float secondAngle = bm.getActiveBullets().get(0).getAngle();
        assertTrue(Math.abs(secondAngle - firstAngle) > 1f, "SpiralStream rotates each cadence");
    }

    private static void testLinearBulletLifecycleAndPlayerHit() {
        BulletManager bm = new BulletManager();
        Rectangle bounds = new Rectangle(-10f, 0f, 20f, 10f);
        LinearBullet linear = new LinearBullet(bounds, 1f);

        Bullet b1 = bm.spawnEnemyBullet(0f, 5f, 20f, 0f, 3);
        b1.setBehavior(linear.copy());
        bm.update(1.0f);
        assertEqual(0, bm.getActiveBullets().size(), "LinearBullet destroys when off-screen");

        final int[] playerDamageTaken = {0};
        Player dummyPlayer = new Player() {
            @Override
            public String getName() { return "TestPlayer"; }
            @Override
            public void takeDamage(int damage) { playerDamageTaken[0] += damage; }
            @Override
            public Rectangle getCoreHitbox() { return new Rectangle(0f, 0f, 1f, 1f); }
            @Override
            public Rectangle getGrazeHitbox() { return new Rectangle(0f, 0f, 1f, 1f); }
            @Override
            public void onCollision(com.Ruinfall.frontend.objects.Collidable other) {}
        };

        Bullet b2 = bm.spawnEnemyBullet(0f, 0f, 5f, 0f, 4);
        b2.setBehavior(linear.copy());
        b2.onCollision(dummyPlayer);
        assertEqual(4, playerDamageTaken[0], "LinearBullet damages player on collision");
        assertTrue(b2.isDestroyed(), "LinearBullet destroys on player hit");
    }

    private static void testBubbleBulletDragTtlAndCardinalBurst() {
        BulletManager bm = new BulletManager();
        Rectangle bounds = new Rectangle(-14f, 0f, 28f, 14f);
        BubbleBullet bubble = new BubbleBullet(0.2f, 0.25f, 3.0f, 0.5f, 3.0f, 9f, 1, false, bounds);

        Bullet b = bm.spawnEnemyBullet(0f, 5f, 6f, 0f, 1);
        b.setBehavior(bubble);
        float initialVx = b.getVx();

        bm.update(0.1f);
        assertTrue(b.getVx() < initialVx, "BubbleBullet applies drag");
        assertEqual(1, bm.getActiveBullets().size(), "BubbleBullet still alive before TTL");

        bm.update(0.2f);
        assertEqual(4, bm.getActiveBullets().size(), "BubbleBullet timeout triggers 4-way LinearBullet burst");
        for (Bullet child : bm.getActiveBullets()) {
            assertTrue(child.getBehavior() instanceof LinearBullet, "Burst children use LinearBullet");
        }

        // Verify clearEnemyBullets does NOT trigger BubbleBullet burst
        bm.clearAll();
        Bullet b2 = bm.spawnEnemyBullet(0f, 5f, 6f, 0f, 1);
        b2.setBehavior(bubble.copy());
        bm.clearEnemyBullets();
        assertEqual(0, bm.getActiveBullets().size(), "clearEnemyBullets removes lingering bubbles without bursting");
    }

    private static void testWormBossSegmentationAndSharedHp() {
        Rectangle bounds = new Rectangle(-14f, 0f, 28f, 14f);
        WormBoss boss = new WormBoss(0f, 7f, "Eater", 2000, bounds, 6, 1.0f);
        assertTrue(boss instanceof Boss, "WormBoss inherits from Boss");
        assertEqual(6, boss.getSegments().size(), "WormBoss creates N trailing segments");

        WormSegment seg0 = boss.getSegments().get(0);
        WormSegment seg5 = boss.getSegments().get(5);
        float dist05 = (float) Math.hypot(seg0.getCenterX() - seg5.getCenterX(), seg0.getCenterY() - seg5.getCenterY());
        assertNear(5.0f, dist05, 0.15f, "Segments spaced at fixed distance intervals");

        seg5.takeDamage(50);
        assertEqual(1950, boss.getHp(), "Damage to trailing segment deducts from shared boss HP");
    }

    private static void testWormBossTerrainDependentSteering() {
        Rectangle bounds = new Rectangle(-14f, 0f, 28f, 14f);
        WormBoss insideBoss = new WormBoss(0f, 7f, "Inside", 2000, bounds, 4, 1.0f);
        insideBoss.setCurrentHeadingRad(0f); // Heading +X
        insideBoss.steerTerrainDependent(-10f, 7f, 10f, 0.2f); // Target behind it (-X)
        float insideTurn = Math.abs(insideBoss.getCurrentHeadingRad());

        WormBoss outsideBoss = new WormBoss(20f, 7f, "Outside", 2000, bounds, 4, 1.0f);
        outsideBoss.setCurrentHeadingRad(0f); // Heading +X
        outsideBoss.steerTerrainDependent(-10f, 7f, 10f, 0.2f); // Target behind it (-X)
        float outsideTurn = Math.abs(outsideBoss.getCurrentHeadingRad());

        assertTrue(outsideTurn > insideTurn * 3.0f, "Outside bounds allows much sharper U-turn than inside bounds");
    }

    private static void testWormBossThreeStagesAndCleanup() {
        Rectangle bounds = new Rectangle(-14f, 0f, 28f, 14f);
        WormBoss boss = new WormBoss(0f, 7f, "Wyrm", 2000, bounds, 6, 1.0f);
        BulletManager bm = new BulletManager();
        PlayerController player = new PlayerController();
        player.getMotor().teleport(2f, 2f);

        // Stage 1: 100% - 66%
        assertEqual(WormBoss.Stage.STAGE_1_BURROW_RUSH, boss.getCurrentStage(), "Starts in Stage 1");
        for (int i = 0; i < 75; i++) {
            boss.updateBoss(1f / 60f, player, bm);
            bm.update(1f / 60f);
        }
        assertTrue(!bm.getActiveBullets().isEmpty(), "Stage 1 fires LinearBullet volleys while inside bounds");

        // Transition to Stage 2: 66% - 33%
        boss.takeDamage(800); // HP = 1200 / 2000 = 60%
        assertEqual(WormBoss.Stage.STAGE_2_SIDE_SWEEPS, boss.getCurrentStage(), "Transitions to Stage 2 at 60% HP");
        assertEqual(0, bm.getActiveBullets().size(), "Stage transition cleans up lingering bullets");

        for (int i = 0; i < 120; i++) {
            boss.updateBoss(1f / 60f, player, bm);
            bm.update(1f / 60f);
        }
        assertTrue(!boss.getSlimeHazards().isEmpty(), "Stage 2 drops SlimeHazard zones");

        // Test SlimeHazard overlap debuff & dash lock
        SlimeHazard slime = boss.getSlimeHazards().get(0);
        player.getMotor().teleport(slime.getX() + slime.getWidth() * 0.5f, slime.getY());
        boss.updateBoss(1f / 60f, player, bm);
        assertTrue(!player.isCanDash(), "Overlapping SlimeHazard sets canDash = false");
        assertNear(slime.getSpeedMultiplier(), player.getMovementSpeedMultiplier(), 0.001f, "Overlapping SlimeHazard applies configured speed multiplier");

        // Exit slime -> restores dash and speed
        player.getMotor().teleport(-100f, -100f);
        boss.updateBoss(1f / 60f, player, bm);
        assertTrue(player.isCanDash(), "Exiting SlimeHazard restores canDash = true");
        assertNear(1.0f, player.getMovementSpeedMultiplier(), 0.001f, "Exiting SlimeHazard restores normal speed");

        // Transition to Stage 3: 33% - 0%
        boss.takeDamage(600); // HP = 600 / 2000 = 30%
        assertEqual(WormBoss.Stage.STAGE_3_BULLET_HELL_CORE, boss.getCurrentStage(), "Transitions to Stage 3 at 30% HP");
        assertEqual(0, boss.getSlimeHazards().size(), "Stage 3 transition cleans up SlimeHazard zones");
        assertEqual(0, bm.getActiveBullets().size(), "Stage 3 transition cleans up lingering bubbles/bullets");

        boss.setStage3CycleDuration(0.3f);
        for (int i = 0; i < 24; i++) { // 0.4s -> cycles from 0 to 1
            boss.updateBoss(1f / 60f, player, bm);
            bm.update(1f / 60f);
        }
        assertTrue(!boss.isUsingSegmentedHitboxes(), "Stage 3 swaps to single central vertical hitbox");
        assertEqual(1, boss.getActiveHitboxes().size(), "Stage 3 has exactly 1 active hitbox");
        assertEqual(1, boss.getStage3PatternIndex(), "Stage 3 cycles to second bullet-hell pattern");
        for (int i = 0; i < 18; i++) { // +0.3s (0.7s total) -> cycles from 1 to 2
            boss.updateBoss(1f / 60f, player, bm);
            bm.update(1f / 60f);
        }
        assertEqual(2, boss.getStage3PatternIndex(), "Stage 3 cycles to third bullet-hell pattern");
        assertTrue(!bm.getActiveBullets().isEmpty(), "Stage 3 fires dense LinearBullet patterns");

        // Boss death cleanup
        boss.takeDamage(600);
        assertTrue(!boss.isAlive(), "Boss defeated at 0 HP");
        assertEqual(0, bm.getActiveBullets().size(), "Boss death cleans up all active enemy bullets");
        assertEqual(0, boss.getSlimeHazards().size(), "Boss death cleans up all slime hazards");
    }

    private static void testUpDashAndDiagonalDash() {
        BoxRoom room = new BoxRoom();
        PlayerController player = new PlayerController();
        player.getMotor().setSolids(room.getBlocks());
        player.getMotor().teleport(0f, 0.01f);

        // 1. Up-Dash: input (0, 1)
        player.getInput().setMoveInputForTest(0, 1);
        player.getInput().triggerDashForTest(false);
        for (int i = 0; i < 6; i++) { // > dashFreezeTime (0.05s)
            player.stepPhysics(1f / 60f);
        }
        assertTrue(player.getStateMachine().getCurrentState() == player.getDashState(), "Up-Dash enters DashState");
        assertNear(0f, player.getVelocity().x, 0.01f, "Up-Dash preserves zero X velocity");
        assertNear(player.getStats().dashSpeed, player.getVelocity().y, 0.01f, "Up-Dash preserves full upward dashSpeed");
        assertTrue(player.getPosition().y > 0.5f, "Up-Dash moves player upward");

        // 2. Diagonal Up-Right Dash: input (1, 1)
        player.respawn();
        player.getMotor().teleport(0f, 0.01f);
        player.getInput().setMoveInputForTest(1, 1);
        player.getInput().triggerDashForTest(false);
        for (int i = 0; i < 6; i++) {
            player.stepPhysics(1f / 60f);
        }
        float expectedDiag = player.getStats().dashSpeed / (float) Math.sqrt(2.0);
        assertNear(expectedDiag, player.getVelocity().x, 0.05f, "Diagonal Dash preserves X component");
        assertNear(expectedDiag, player.getVelocity().y, 0.05f, "Diagonal Dash preserves Y component");
    }

    private static void testDiagonalSwingAndPogo() {
        PlayerController player = new PlayerController();
        player.getMotor().teleport(0f, 5.0f); // Airborne

        // Diagonal Up-Right swing
        player.getInput().setMoveInputForTest(1, 1);
        player.getInput().triggerSwingForTest(true);
        assertTrue(player.tryStartSwing(), "Enters SwingState");
        SwingState swing = player.getSwingState();
        assertEqual(SwingState.SwingDirection.DIAGONAL_UP, swing.getCurrentDirection(), "Swing resolves DIAGONAL_UP");
        float invSqrt2 = 1.0f / (float) Math.sqrt(2.0);
        assertNear(invSqrt2, swing.getSwingVector().x, 0.01f, "Diagonal Up swingVector.x normalized");
        assertNear(invSqrt2, swing.getSwingVector().y, 0.01f, "Diagonal Up swingVector.y normalized");

        // Diagonal Down-Right pogo swing while airborne
        player.getStateMachine().changeState(player.getNormalState());
        player.getInput().setMoveInputForTest(1, -1);
        player.getInput().triggerSwingForTest(true);
        assertTrue(player.tryStartSwing(), "Enters SwingState for diagonal down");
        assertEqual(SwingState.SwingDirection.DIAGONAL_DOWN, swing.getCurrentDirection(), "Swing resolves DIAGONAL_DOWN");
        swing.onSwingHit(true);
        assertNear(swing.getPogoBounceVelocity(), player.getVelocity().y, 0.01f, "Diagonal Down pogo applies upward bounce");
    }

    private static void testTwoPlatformArenaOneWayAndDropThrough() {
        BoxRoom room = new BoxRoom();
        List<OneWayPlatform> platforms = room.getPlatforms();
        assertEqual(9, platforms.size(), "Expanded BoxRoom has 9 multi-tiered elevated platforms");
        assertTrue(Math.abs(platforms.get(0).getBounds().y - platforms.get(3).getBounds().y) < 0.001f, "Lower platforms are symmetrical");
        assertTrue(platforms.get(4).getBounds().y > platforms.get(0).getBounds().y, "Mid platforms are above lower tier");
        assertTrue(platforms.get(7).getBounds().y > platforms.get(4).getBounds().y, "Upper platforms are above mid tier");

        OneWayPlatform left = platforms.get(0);
        float platCenterX = left.getBounds().x + left.getBounds().width * 0.5f;
        float platTopY = left.getBounds().y + left.getBounds().height;

        PlayerController player = new PlayerController();
        player.getMotor().setSolids(room.getBlocks());

        // Pass-through from below
        player.getMotor().teleport(platCenterX, left.getBounds().y - 1.0f);
        player.setVelocity(0f, 15.0f);
        for (int i = 0; i < 12; i++) {
            player.getMotor().move(player.getVelocity(), 1f / 60f, null, null);
        }
        assertTrue(player.getPosition().y > platTopY, "Player passes upward through one-way platform from below");

        // Land on top from above
        player.getMotor().teleport(platCenterX, platTopY + 0.5f);
        player.setVelocity(0f, -10.0f);
        for (int i = 0; i < 10; i++) {
            player.getMotor().move(player.getVelocity(), 1f / 60f, null, () -> player.setVelocity(0f, 0f));
        }
        assertNear(platTopY, player.getPosition().y, 0.05f, "Player lands cleanly on top of one-way platform");
        assertTrue(player.getMotor().isStandingOnOneWayPlatform(), "Motor detects standing on one-way platform");

        // Drop-through on Down + Jump
        player.getInput().setMoveInputForTest(0, -1);
        player.getMotor().startDropThrough(0.18f);
        player.setVelocity(0f, -5.0f);
        for (int i = 0; i < 10; i++) {
            player.getMotor().move(player.getVelocity(), 1f / 60f, null, null);
        }
        assertTrue(player.getPosition().y < left.getBounds().y, "Player drops through one-way platform cleanly");

        // Verify WormBoss matches arena dimensions
        WormBoss boss = new WormBoss(-10f, 10f, "ArenaWorm", 2000, room.getArenaBounds(), 8, 1.0f);
        assertEqual(room.getArenaBounds(), boss.getMapBounds(), "WormBoss mapBounds matches BoxRoom arena bounds");
    }

    private static void testDashCooldownNerfBarrelsAndRegeneratingStalactite() {
        // 1. Dash cooldown nerf (>= 1.2s) & progress indicator
        PlayerController player = new PlayerController();
        assertTrue(player.getStats().dashCooldown >= 1.2f, "Dash cooldown nerfed to >= 1.2s");
        assertTrue(player.getDashes().tryConsumeDash(), "Initial dash succeeds");
        assertTrue(!player.getDashes().canDash(), "Cannot dash immediately after consuming");
        player.getDashes().tick(0.5f);
        assertTrue(player.getDashes().getCooldownRemaining() > 0.5f, "Dash still on cooldown after 0.5s");
        assertTrue(player.getDashes().getCooldownProgress() < 1.0f, "Cooldown progress < 1.0 while recharging");
        player.getDashes().tick(1.0f);
        player.getDashes().forceRefill();
        assertNear(1.0f, player.getDashes().getCooldownProgress(), 0.001f, "Cooldown progress reaches 1.0 when ready");

        // 2. Barrel AoE detonation & projectile knockback & reset
        BoxRoom room = new BoxRoom();
        WormBoss boss = new WormBoss(0.5f, 0.5f, "BarrelTarget", 2000, room.getArenaBounds(), 6, 1.0f);
        BulletManager bm = new BulletManager();
        Bullet enemyBullet = bm.spawnEnemyBullet(2.0f, 1.0f, -5.0f, 0.0f, 1);
        enemyBullet.setSize(0.45f, 0.45f);

        Barrels barrel = new Barrels(0.0f, 0.0f);
        barrel.takeHit(3);
        assertTrue(!barrel.isActive(), "Barrel breaks after 3 damage");
        barrel.detonate(boss, bm);
        assertTrue(boss.getHp() < 2000, "Barrel detonation deals AoE damage to nearby WormBoss");
        assertTrue(enemyBullet.getVx() > 0.0f, "Barrel detonation knocks back nearby enemy projectile");
        barrel.reset();
        assertTrue(barrel.isActive() && !barrel.hasDetonated(), "Barrel resets cleanly");

        // 3. RegeneratingStalactite 4-state lifecycle
        RegeneratingStalactite stalactite = new RegeneratingStalactite(0.0f, 12.5f);
        stalactite.setSolids(room.getBlocks());
        stalactite.setRegenCooldown(1.0f);
        assertEqual(RegeneratingStalactite.State.INTACT, stalactite.getState(), "Stalactite starts INTACT");
        assertTrue(stalactite.checkProximityTrigger(0.2f, 5.0f), "Stalactite triggers when entity passes underneath");
        assertEqual(RegeneratingStalactite.State.SHAKING, stalactite.getState(), "Stalactite enters SHAKING");

        for (int i = 0; i < 35; i++) {
            stalactite.update(1f / 60f);
        }
        assertEqual(RegeneratingStalactite.State.FALLING, stalactite.getState(), "Stalactite transitions to FALLING");

        // Simulate falling until impact -> REGENERATING
        for (int i = 0; i < 120 && stalactite.getState() == RegeneratingStalactite.State.FALLING; i++) {
            stalactite.update(1f / 60f);
        }
        assertEqual(RegeneratingStalactite.State.REGENERATING, stalactite.getState(), "Stalactite shatters into REGENERATING");
        stalactite.update(0.5f);
        assertTrue(stalactite.getRenderScale() > 0.3f && stalactite.getRenderScale() < 0.8f, "Regenerating stalactite scales up smoothly");
        stalactite.update(0.6f);
        assertEqual(RegeneratingStalactite.State.INTACT, stalactite.getState(), "Stalactite respawns as INTACT after cooldown");
        assertNear(1.0f, stalactite.getRenderScale(), 0.001f, "Regenerated stalactite has full scale 1.0");
    }

    private static void testVfxParticlePoolShakeHitstopAndEvents() {
        // 1. ParticleSystem pooling
        ParticleSystem ps = new ParticleSystem();
        int initialPool = ps.getPooledParticleCount();
        ps.emitBurst(0f, 0f, 10, 2f, 5f, 1f, 0f, 0.2f, 0.2f, 0.1f, 0.15f, com.badlogic.gdx.graphics.Color.WHITE);
        assertEqual(10, ps.getActiveParticleCount(), "ParticleSystem activates 10 burst particles");
        assertEqual(initialPool - 10, ps.getPooledParticleCount(), "Particles drawn from pool");
        ps.update(0.3f);
        assertEqual(0, ps.getActiveParticleCount(), "Expired particles deactivated");
        assertEqual(initialPool, ps.getPooledParticleCount(), "Expired particles returned to pool");

        // 2. CameraShake trauma decay
        CameraShake shake = new CameraShake();
        shake.addTrauma(0.6f);
        shake.update(0.016f);
        assertTrue(Math.abs(shake.getOffsetX()) > 0f || Math.abs(shake.getOffsetY()) > 0f, "CameraShake produces offset under trauma");
        for (int i = 0; i < 60; i++) {
            shake.update(0.02f);
        }
        assertNear(0f, shake.getTrauma(), 0.001f, "CameraShake trauma decays to 0");

        // 3. HitstopManager freeze frames
        HitstopManager hitstop = new HitstopManager();
        hitstop.trigger(0.05f);
        assertTrue(hitstop.isActive(), "Hitstop active after trigger");
        float pausedDt = hitstop.computeEffectiveDt(0.02f);
        assertTrue(pausedDt < 0.005f, "Hitstop scales down dt while active");
        hitstop.computeEffectiveDt(0.05f);
        assertTrue(!hitstop.isActive(), "Hitstop expires after duration");
        assertNear(0.02f, hitstop.computeEffectiveDt(0.02f), 0.0001f, "Hitstop returns full dt once expired");

        // 4. EffectManager event-driven VFX (Barrel + Stage transition + Bubble pop)
        EffectManager fx = EffectManager.getInstance();
        fx.clearAll();
        Barrels barrel = new Barrels(1f, 1f);
        barrel.takeHit(3); // publishes BARREL_EXPLODED
        assertTrue(fx.getParticles().getActiveParticleCount() > 0, "Barrel explosion emits particles via GameEventBus");
        assertTrue(fx.getParticles().getActiveRingCount() > 0, "Barrel explosion emits shockwave ring");
        assertTrue(fx.getCameraShake().getTrauma() > 0.3f, "Barrel explosion adds camera trauma");
        assertTrue(fx.getHitstop().isActive(), "Barrel explosion triggers hitstop");
        fx.clearAll();
    }

    private static void testExpandedArenaGrappleNetworkStalactiteRandomizationAndBarrelPhysics() {
        // 1. Expanded arena width (~1.5x-1.8x wider than 28f) & WormBoss Stage 3 center alignment
        BoxRoom room = new BoxRoom();
        Rectangle bounds = room.getArenaBounds();
        assertTrue(bounds.width >= 42.0f && bounds.width <= 52.0f, "Arena width expanded ~1.5x-1.8x");
        assertNear(0.0f, bounds.x + bounds.width * 0.5f, 0.001f, "Expanded arena remains horizontally centered at X=0");

        WormBoss boss = new WormBoss("ExpandedWorm", 300);
        assertEqual(bounds, boss.getMapBounds(), "Default WormBoss bounds match expanded BoxRoom arena");

        // 2. Grapple network aerial chaining coverage across ceiling lane
        List<GrapplePoint> anchors = room.createGrappleNetwork();
        assertTrue(anchors.size() >= 10, "Grapple network provides rich anchor coverage across wider arena");
        // Verify consecutive ceiling anchors (indices 0..4) have overlapping latch radii
        for (int i = 0; i < 4; i++) {
            GrapplePoint a = anchors.get(i);
            GrapplePoint b = anchors.get(i + 1);
            float dist = a.getCenter().dst(b.getCenter());
            assertTrue(dist < a.getGrappleRadius() + b.getGrappleRadius(), "Adjacent ceiling grapple nodes overlap for aerial chaining");
        }

        // 3. Randomized stalactite jitter, respawn cooldown range, and arming stagger
        RegeneratingStalactite rs = new RegeneratingStalactite(8.5f, 12.5f);
        rs.configureRandomization(1.1f, 0.25f, 0.90f, 3.5f, 7.5f, 1.5f);
        assertTrue(Math.abs(rs.getX() - 8.5f) <= 1.11f, "Stalactite X-jitter stays within lane bounds");
        assertTrue(rs.getRegenCooldown() >= 3.5f && rs.getRegenCooldown() <= 7.5f, "Stalactite regen cooldown within randomized range");
        assertTrue(rs.getArmingDelayTimer() >= 0.0f && rs.getArmingDelayTimer() <= 1.5f, "Stalactite initial arming stagger within configured range");

        // 4. Barrel stationary physics, 3-hit durability, single-hit-per-swing, damage feedback, and heavy impact
        EffectManager fx = EffectManager.getInstance();
        fx.clearAll();
        Barrels barrel = new Barrels(-5.0f, 2.8f);
        assertEqual(0, barrel.getDamageStage(), "Fresh barrel starts at damage stage 0");

        PlayerController player = new PlayerController();
        player.getInput().setMoveInputForTest(1, 0);
        player.getInput().triggerSwingForTest(true);
        player.tryStartSwing();

        // Simulate multiple collision frames during the same swing -> must only deal 1 hit
        barrel.onCollision(player);
        barrel.onCollision(player);
        barrel.onCollision(player);
        barrel.update(0.05f);

        assertEqual(2, barrel.getHealth(), "Single player slash only deals 1 hit across multiple frames");
        assertEqual(1, barrel.getDamageStage(), "Barrel enters damage stage 1 after first slash");
        assertTrue(barrel.getHitFlashTimer() > 0f, "Non-lethal barrel hit triggers hit-flash");
        assertTrue(fx.getParticles().getActiveParticleCount() > 0, "Non-lethal barrel hit emits wood/metal spark burst");
        assertNear(-5.0f, barrel.getX(), 0.0001f, "Barrel does not slide horizontally after hit");
        assertNear(2.8f, barrel.getY(), 0.0001f, "Barrel does not bounce vertically after hit");

        // Second slash -> damage stage 2 (critical, still active)
        barrel.takeHit(1);
        assertTrue(barrel.isActive(), "Barrel survives 2 hits");
        assertEqual(2, barrel.getDamageStage(), "Barrel enters critical damage stage 2 at 1 HP");

        // Third slash -> breaks and drops items cleanly in place near anchor
        barrel.takeHit(1);
        assertTrue(!barrel.isActive(), "Barrel detonates on 3rd hit");
        assertEqual(2, barrel.getDrops().size(), "Broken barrel spawns 2 drops");
        for (Item drop : barrel.getDrops()) {
            assertTrue(Math.abs(drop.getX() - (-5.0f)) <= 1.5f, "Barrel drop spawns in place near anchor X");
        }

        // Heavy impact from falling stalactite detonates full-HP barrel immediately
        barrel.reset();
        RegeneratingStalactite heavyStalactite = new RegeneratingStalactite(-5.0f, 12.5f);
        heavyStalactite.configureRandomization(0f, 0.05f, 0.05f, 1.0f, 1.0f, 0f);
        heavyStalactite.trigger();
        heavyStalactite.update(0.1f); // enters FALLING
        heavyStalactite.onCollision(barrel);
        assertTrue(!barrel.isActive(), "Falling stalactite heavy impact detonates full-HP barrel immediately");
        fx.clearAll();
    }

    private static void assertTrue(boolean cond, String msg) {
        if (!cond) {
            throw new AssertionError("FAILED: " + msg);
        }
        System.out.println("[PASS] " + msg);
    }

    private static void assertEqual(Object expected, Object actual, String msg) {
        if (!expected.equals(actual)) {
            throw new AssertionError("FAILED: " + msg + " (expected=" + expected + ", actual=" + actual + ")");
        }
        System.out.println("[PASS] " + msg);
    }

    private static void assertNear(float expected, float actual, float eps, String msg) {
        if (Math.abs(expected - actual) > eps) {
            throw new AssertionError("FAILED: " + msg + " (expected=" + expected + ", actual=" + actual + ")");
        }
        System.out.println("[PASS] " + msg);
    }
}
