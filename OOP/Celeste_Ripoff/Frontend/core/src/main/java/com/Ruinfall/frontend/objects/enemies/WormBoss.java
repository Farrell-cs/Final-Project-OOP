package com.Ruinfall.frontend.objects.enemies;

import com.Ruinfall.frontend.PlayerController;
import com.Ruinfall.frontend.objects.Collidable;
import com.Ruinfall.frontend.objects.Player;
import com.Ruinfall.frontend.objects.hazards.SlimeHazard;
import com.Ruinfall.frontend.objects.patterns.projectile.BubbleBullet;
import com.Ruinfall.frontend.objects.patterns.projectile.IBulletBehavior;
import com.Ruinfall.frontend.objects.patterns.projectile.IShootingPattern;
import com.Ruinfall.frontend.objects.patterns.projectile.LinearBullet;
import com.Ruinfall.frontend.objects.patterns.projectile.RadialBurst;
import com.Ruinfall.frontend.objects.patterns.projectile.SingleShot;
import com.Ruinfall.frontend.objects.patterns.projectile.SpreadShot;
import com.Ruinfall.frontend.objects.patterns.projectile.SpiralStream;
import com.Ruinfall.frontend.systems.BulletManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 3-stage segmented Worm Boss inheriting from Boss.
 * Uses Strategy-pattern shooting and bullet behaviors decoupled per stage.
 */
public class WormBoss extends Boss {
    public enum Stage {
        STAGE_1_BURROW_RUSH,
        STAGE_2_SIDE_SWEEPS,
        STAGE_3_BULLET_HELL_CORE
    }

    private enum SweepPhase {
        EXIT_TO_SIDE,
        SWEEP_ACROSS
    }

    private static final class HistorySample {
        final float x;
        final float y;
        final float headingDeg;

        HistorySample(float x, float y, float headingDeg) {
            this.x = x;
            this.y = y;
            this.headingDeg = headingDeg;
        }
    }

    private final Rectangle mapBounds;
    private final List<WormSegment> segments = new ArrayList<>();
    private final List<HistorySample> history = new ArrayList<>();
    private final List<SlimeHazard> slimeHazards = new ArrayList<>();

    private final float segmentSpacing;
    private float currentHeadingRad;
    private float moveSpeed = 10.0f;
    private float sweepSpeed = 18.0f;
    private float insideTurnRateRad = 0.85f;
    private float outsideTurnRateRad = 7.5f;

    private Stage currentStage = Stage.STAGE_1_BURROW_RUSH;
    private boolean useSegmentedHitboxes = true;
    private final Rectangle coreColumnHitbox = new Rectangle();

    // Stage 2 side-sweep state
    private SweepPhase sweepPhase = SweepPhase.EXIT_TO_SIDE;
    private int sweepDirection = 1; // +1 = sweeping right, -1 = sweeping left
    private float sweepSideMargin = 3.5f;
    private float slimeDropInterval = 0.35f;
    private float slimeDropTimer = 0f;
    private int nextSlimeSegmentIndex = 0;

    // Stage 3 core column state
    private boolean coreAligned = false;
    private float stage3CycleDuration = 3.0f;
    private float stage3CycleTimer = 0f;
    private int stage3PatternIndex = 0;

    // Injected Strategy-pattern shooting & bullet behaviors per stage
    private IShootingPattern stage1Pattern;
    private IBulletBehavior stage1Behavior;

    private IShootingPattern stage2Pattern;
    private IBulletBehavior stage2Behavior;

    private final List<IShootingPattern> stage3Patterns = new ArrayList<>();
    private IBulletBehavior stage3Behavior;

    // Cached references for hazard cleanup on death/destroy
    private BulletManager lastBulletManager;
    private PlayerController lastPlayer;

    public WormBoss(String name, int hp) {
        this(-12f, 7f, name, hp);
    }

    public WormBoss(float x, float y, String name, int hp) {
        this(x, y, name, hp, new Rectangle(-14f, 0f, 28f, 14f), 8, 1.0f);
    }

    public WormBoss(
        float x,
        float y,
        String name,
        int hp,
        Rectangle mapBounds,
        int segmentCount,
        float segmentSpacing
    ) {
        super(x, y, name, hp);
        this.mapBounds = mapBounds != null ? new Rectangle(mapBounds) : new Rectangle(-14f, 0f, 28f, 14f);
        this.segmentSpacing = Math.max(0.25f, segmentSpacing);
        this.width = 1.2f;
        this.height = 1.2f;
        this.color = new Color(0.75f, 0.22f, 0.32f, 1.0f);
        this.currentHeadingRad = 0f;

        initSegments(Math.max(1, segmentCount));
        initHistoryBuffer();
        initDefaultStrategies();
    }

    private void initSegments(int count) {
        segments.clear();
        float segSize = 0.95f;
        for (int i = 0; i < count; i++) {
            float cx = getHeadCenterX() - (i + 1) * segmentSpacing;
            float cy = getHeadCenterY();
            Color segColor = (i % 2 == 0)
                ? new Color(0.55f, 0.18f, 0.28f, 1.0f)
                : new Color(0.42f, 0.14f, 0.22f, 1.0f);
            WormSegment seg = new WormSegment(
                this,
                i,
                cx - segSize * 0.5f,
                cy - segSize * 0.5f,
                segSize,
                segSize,
                segColor
            );
            segments.add(seg);
        }
    }

    private void initHistoryBuffer() {
        history.clear();
        float totalLen = (segments.size() + 2) * segmentSpacing;
        int steps = Math.max(10, (int) Math.ceil(totalLen / 0.1f));
        float stepDist = totalLen / steps;
        float hx = getHeadCenterX();
        float hy = getHeadCenterY();
        float deg = currentHeadingRad * MathUtils.radiansToDegrees;
        // Oldest sample first, head sample last
        for (int i = steps; i >= 0; i--) {
            float sx = hx - MathUtils.cos(currentHeadingRad) * (i * stepDist);
            float sy = hy - MathUtils.sin(currentHeadingRad) * (i * stepDist);
            history.add(new HistorySample(sx, sy, deg));
        }
        updateSegmentsFromHistory();
    }

    private void initDefaultStrategies() {
        this.stage1Pattern = new SingleShot(1.1f, 9.0f, 1);
        this.stage1Behavior = new LinearBullet(mapBounds, 2.5f);

        this.stage2Pattern = new SpreadShot(1.25f, 4.5f, 1, 3, 45f);
        this.stage2Behavior = new BubbleBullet(1.0f, 2.2f, 8.5f, false, mapBounds);

        this.stage3Patterns.clear();
        this.stage3Patterns.add(new SpiralStream(0.12f, 8.5f, 1, 4, 16f));
        this.stage3Patterns.add(new RadialBurst(0.55f, 7.5f, 1, 14, 0f, 13f, 0.42f));
        this.stage3Patterns.add(new SpreadShot(0.28f, 9.5f, 1, 7, 90f, 12f, 0.42f));
        this.stage3Behavior = new LinearBullet(mapBounds, 2.5f);
    }

    public void setStage1Strategy(IShootingPattern pattern, IBulletBehavior behavior) {
        if (pattern != null) this.stage1Pattern = pattern;
        if (behavior != null) this.stage1Behavior = behavior;
    }

    public void setStage2Strategy(IShootingPattern pattern, IBulletBehavior behavior) {
        if (pattern != null) this.stage2Pattern = pattern;
        if (behavior != null) this.stage2Behavior = behavior;
    }

    public void setStage3Strategies(List<IShootingPattern> patterns, IBulletBehavior behavior) {
        if (patterns != null && !patterns.isEmpty()) {
            this.stage3Patterns.clear();
            this.stage3Patterns.addAll(patterns);
            this.stage3PatternIndex = 0;
        }
        if (behavior != null) this.stage3Behavior = behavior;
    }

    /**
     * Main per-frame boss update with player target and BulletManager injection.
     */
    public void updateBoss(float dt, PlayerController player, BulletManager bulletManager) {
        if (!active || !isAlive()) {
            return;
        }
        this.lastBulletManager = bulletManager;
        this.lastPlayer = player;
        super.update(dt);

        evaluateStageTransition();

        float targetX = player != null ? player.getPosition().x : (mapBounds.x + mapBounds.width * 0.5f);
        float targetY = player != null ? (player.getPosition().y + 0.7f) : (mapBounds.y + mapBounds.height * 0.5f);

        switch (currentStage) {
            case STAGE_1_BURROW_RUSH:
                updateStage1(dt, targetX, targetY, bulletManager);
                break;
            case STAGE_2_SIDE_SWEEPS:
                updateStage2(dt, targetX, targetY, bulletManager);
                break;
            case STAGE_3_BULLET_HELL_CORE:
                updateStage3(dt, targetX, targetY, bulletManager);
                break;
        }

        updateSlimeHazards(dt, player);
    }

    private void updateStage1(float dt, float targetX, float targetY, BulletManager bulletManager) {
        steerTerrainDependent(targetX, targetY, moveSpeed, dt);
        recordHistoryAndSyncSegments();

        if (isInsideMapBounds() && stage1Pattern != null && bulletManager != null) {
            stage1Pattern.update(
                dt,
                getHeadCenterX(),
                getHeadCenterY(),
                targetX,
                targetY,
                bulletManager,
                stage1Behavior,
                false
            );
        }
    }

    private void updateStage2(float dt, float targetX, float targetY, BulletManager bulletManager) {
        float leftStagingX = mapBounds.x - sweepSideMargin;
        float rightStagingX = mapBounds.x + mapBounds.width + sweepSideMargin;
        float hx = getHeadCenterX();
        float hy = getHeadCenterY();

        if (sweepPhase == SweepPhase.EXIT_TO_SIDE) {
            float destX = (sweepDirection > 0) ? leftStagingX : rightStagingX;
            float destY = MathUtils.clamp(targetY, mapBounds.y + 1.2f, mapBounds.y + mapBounds.height - 1.2f);
            steerWithTurnRate(destX, destY, sweepSpeed, outsideTurnRateRad, dt);

            if (Math.hypot(destX - getHeadCenterX(), destY - getHeadCenterY()) < 1.0f) {
                sweepPhase = SweepPhase.SWEEP_ACROSS;
                currentHeadingRad = (sweepDirection > 0) ? 0f : MathUtils.PI;
            }
        } else {
            // Horizontal rush across the map
            float exitTargetX = (sweepDirection > 0) ? rightStagingX : leftStagingX;
            steerWithTurnRate(exitTargetX, hy, sweepSpeed, 1.5f, dt);

            if ((sweepDirection > 0 && getHeadCenterX() >= rightStagingX)
                || (sweepDirection < 0 && getHeadCenterX() <= leftStagingX)) {
                sweepDirection = -sweepDirection;
                sweepPhase = SweepPhase.EXIT_TO_SIDE;
            }
        }

        recordHistoryAndSyncSegments();

        // 1. Fire BubbleBullet clusters during passes
        if (stage2Pattern != null && bulletManager != null && isInsideMapBounds()) {
            stage2Pattern.update(
                dt,
                getHeadCenterX(),
                getHeadCenterY(),
                targetX,
                targetY,
                bulletManager,
                stage2Behavior,
                false
            );
        }

        // 2. Trailing segments periodically drop persistent SlimeHazard zones
        slimeDropTimer += dt;
        if (slimeDropTimer >= slimeDropInterval && !segments.isEmpty()) {
            slimeDropTimer = 0f;
            WormSegment seg = segments.get(nextSlimeSegmentIndex % segments.size());
            nextSlimeSegmentIndex = (nextSlimeSegmentIndex + 1) % segments.size();
            float sx = seg.getCenterX();
            float sy = seg.getCenterY();
            if (mapBounds.contains(sx, sy)) {
                slimeHazards.add(new SlimeHazard(sx - 0.65f, sy - 0.35f, 1.3f, 0.7f, 1, 3));
            }
        }
    }

    private void updateStage3(float dt, float targetX, float targetY, BulletManager bulletManager) {
        float centerX = mapBounds.x + mapBounds.width * 0.5f;
        float columnHeight = segments.size() * segmentSpacing;
        float topY = mapBounds.y + mapBounds.height * 0.5f + columnHeight * 0.5f;

        if (!coreAligned) {
            float dx = centerX - getHeadCenterX();
            float dy = topY - getHeadCenterY();
            float dist = (float) Math.hypot(dx, dy);
            float step = sweepSpeed * dt;
            if (dist <= step || dist < 0.15f) {
                setHeadCenter(centerX, topY);
                currentHeadingRad = MathUtils.PI * 0.5f;
                coreAligned = true;
            } else {
                steerWithTurnRate(centerX, topY, sweepSpeed, outsideTurnRateRad, dt);
            }
        }

        // Align segments into a static vertical column and swap to single central vertical hitbox
        alignVerticalColumn(centerX, getHeadCenterY());

        if (!stage3Patterns.isEmpty() && bulletManager != null) {
            stage3CycleTimer += dt;
            if (stage3CycleTimer >= stage3CycleDuration) {
                stage3CycleTimer = 0f;
                stage3PatternIndex = (stage3PatternIndex + 1) % stage3Patterns.size();
                stage3Patterns.get(stage3PatternIndex).reset();
            }

            IShootingPattern activePattern = stage3Patterns.get(stage3PatternIndex);
            float coreOriginX = coreColumnHitbox.x + coreColumnHitbox.width * 0.5f;
            float coreOriginY = coreColumnHitbox.y + coreColumnHitbox.height * 0.5f;
            activePattern.update(
                dt,
                coreOriginX,
                coreOriginY,
                targetX,
                targetY,
                bulletManager,
                stage3Behavior,
                false
            );
        }
    }

    private void alignVerticalColumn(float centerX, float headCenterY) {
        useSegmentedHitboxes = false;
        for (int i = 0; i < segments.size(); i++) {
            WormSegment seg = segments.get(i);
            float sy = headCenterY - (i + 1) * segmentSpacing;
            seg.setCenter(centerX, sy);
            seg.setRotationDeg(270f);
        }

        float bottomY = headCenterY - segments.size() * segmentSpacing - height * 0.5f;
        float topEdge = headCenterY + height * 0.5f;
        float colWidth = Math.max(width, 1.3f);
        coreColumnHitbox.set(centerX - colWidth * 0.5f, bottomY, colWidth, topEdge - bottomY);
    }

    /**
     * Terrain-dependent steering:
     * - Inside map bounds: low angular velocity (high turn-radius restriction), maintaining momentum toward boundary.
     * - Outside map bounds: high angular velocity, allowing sharp U-turns toward the player.
     */
    public void steerTerrainDependent(float targetX, float targetY, float speed, float dt) {
        float turnRate = isInsideMapBounds() ? insideTurnRateRad : outsideTurnRateRad;
        steerWithTurnRate(targetX, targetY, speed, turnRate, dt);
    }

    private void steerWithTurnRate(float targetX, float targetY, float speed, float maxTurnRateRad, float dt) {
        float hx = getHeadCenterX();
        float hy = getHeadCenterY();
        float desiredAngleRad = MathUtils.atan2(targetY - hy, targetX - hx);
        float diff = normalizeAngleRad(desiredAngleRad - currentHeadingRad);
        float maxStep = maxTurnRateRad * dt;
        float deltaAngle = MathUtils.clamp(diff, -maxStep, maxStep);
        currentHeadingRad = normalizeAngleRad(currentHeadingRad + deltaAngle);

        float nextX = hx + MathUtils.cos(currentHeadingRad) * speed * dt;
        float nextY = hy + MathUtils.sin(currentHeadingRad) * speed * dt;
        setHeadCenter(nextX, nextY);
    }

    private void recordHistoryAndSyncSegments() {
        float hx = getHeadCenterX();
        float hy = getHeadCenterY();
        float deg = currentHeadingRad * MathUtils.radiansToDegrees;

        if (history.isEmpty()) {
            history.add(new HistorySample(hx, hy, deg));
        } else {
            HistorySample last = history.get(history.size() - 1);
            if (Math.hypot(hx - last.x, hy - last.y) >= 0.02f) {
                history.add(new HistorySample(hx, hy, deg));
            }
        }

        updateSegmentsFromHistory();
        trimHistoryBuffer();
    }

    private void updateSegmentsFromHistory() {
        if (history.size() < 2) {
            return;
        }
        for (int s = 0; s < segments.size(); s++) {
            float targetDist = (s + 1) * segmentSpacing;
            float accumulated = 0f;
            boolean placed = false;

            for (int i = history.size() - 1; i > 0; i--) {
                HistorySample a = history.get(i);
                HistorySample b = history.get(i - 1);
                float segLen = (float) Math.hypot(b.x - a.x, b.y - a.y);
                if (segLen <= 0.00001f) {
                    continue;
                }
                if (accumulated + segLen >= targetDist) {
                    float t = (targetDist - accumulated) / segLen;
                    float ix = MathUtils.lerp(a.x, b.x, t);
                    float iy = MathUtils.lerp(a.y, b.y, t);
                    segments.get(s).setCenter(ix, iy);
                    segments.get(s).setRotationDeg(a.headingDeg);
                    placed = true;
                    break;
                }
                accumulated += segLen;
            }

            if (!placed) {
                HistorySample oldest = history.get(0);
                segments.get(s).setCenter(oldest.x, oldest.y);
                segments.get(s).setRotationDeg(oldest.headingDeg);
            }
        }
    }

    private void trimHistoryBuffer() {
        float maxDist = (segments.size() + 3) * segmentSpacing;
        float accumulated = 0f;
        int keepFrom = 0;
        for (int i = history.size() - 1; i > 0; i--) {
            HistorySample a = history.get(i);
            HistorySample b = history.get(i - 1);
            accumulated += (float) Math.hypot(b.x - a.x, b.y - a.y);
            if (accumulated >= maxDist) {
                keepFrom = i - 1;
                break;
            }
        }
        if (keepFrom > 0) {
            history.subList(0, keepFrom).clear();
        }
    }

    private void updateSlimeHazards(float dt, PlayerController player) {
        boolean playerOverlappingAny = false;
        for (int i = slimeHazards.size() - 1; i >= 0; i--) {
            SlimeHazard hazard = slimeHazards.get(i);
            hazard.update(dt);
            if (!hazard.isActive()) {
                slimeHazards.remove(i);
                continue;
            }
            if (player != null && hazard.overlaps(player)) {
                hazard.applyTo(player);
                playerOverlappingAny = true;
            }
        }
        if (player != null && !playerOverlappingAny) {
            SlimeHazard.clearFrom(player);
        }
    }

    public Stage evaluateStageTransition() {
        float ratio = maxHP > 0 ? (hp / (float) maxHP) : 0f;
        Stage targetStage;
        if (ratio > 0.66f) {
            targetStage = Stage.STAGE_1_BURROW_RUSH;
        } else if (ratio > 0.33f) {
            targetStage = Stage.STAGE_2_SIDE_SWEEPS;
        } else {
            targetStage = Stage.STAGE_3_BULLET_HELL_CORE;
        }

        if (targetStage != currentStage) {
            onStageTransition(currentStage, targetStage);
            currentStage = targetStage;
        }
        return currentStage;
    }

    private void onStageTransition(Stage from, Stage to) {
        cleanupHazards(lastBulletManager, lastPlayer);

        if (stage1Pattern != null) stage1Pattern.reset();
        if (stage2Pattern != null) stage2Pattern.reset();
        for (IShootingPattern p : stage3Patterns) {
            p.reset();
        }

        if (to == Stage.STAGE_2_SIDE_SWEEPS) {
            sweepPhase = SweepPhase.EXIT_TO_SIDE;
            slimeDropTimer = 0f;
            useSegmentedHitboxes = true;
        } else if (to == Stage.STAGE_3_BULLET_HELL_CORE) {
            coreAligned = false;
            stage3CycleTimer = 0f;
            stage3PatternIndex = 0;
            useSegmentedHitboxes = false;
        }
    }

    /**
     * Cleans up all spawned hazards (slime zones, lingering bullets/bubbles) and clears player debuffs.
     */
    public void cleanupHazards(BulletManager bulletManager, PlayerController player) {
        for (SlimeHazard hazard : slimeHazards) {
            hazard.destroy();
        }
        slimeHazards.clear();

        if (bulletManager != null) {
            bulletManager.clearEnemyBullets();
        }
        if (player != null) {
            SlimeHazard.clearFrom(player);
        }
    }

    @Override
    public boolean takeDamage(int damage) {
        boolean defeated = super.takeDamage(damage);
        if (defeated || !isAlive()) {
            cleanupHazards(lastBulletManager, lastPlayer);
        } else {
            evaluateStageTransition();
        }
        return defeated;
    }

    @Override
    public void destroy() {
        super.destroy();
        cleanupHazards(lastBulletManager, lastPlayer);
        for (WormSegment seg : segments) {
            seg.destroy();
        }
    }

    @Override
    public Rectangle getCoreHitbox() {
        if (!useSegmentedHitboxes) {
            return new Rectangle(coreColumnHitbox);
        }
        return super.getCoreHitbox();
    }

    /**
     * Returns all active hitboxes for collision checks:
     * - Stages 1 & 2: head hitbox + all segment hitboxes.
     * - Stage 3: single central vertical rectangle hitbox.
     */
    public List<Rectangle> getActiveHitboxes() {
        if (!active) {
            return Collections.emptyList();
        }
        if (!useSegmentedHitboxes) {
            return Collections.singletonList(new Rectangle(coreColumnHitbox));
        }
        List<Rectangle> boxes = new ArrayList<>(segments.size() + 1);
        boxes.add(super.getCoreHitbox());
        for (WormSegment seg : segments) {
            if (seg.isActive()) {
                boxes.add(seg.getCoreHitbox());
            }
        }
        return boxes;
    }

    public void onSegmentCollision(WormSegment segment, Player player) {
        if (player != null && active) {
            player.takeDamage(1);
        }
    }

    @Override
    public void render(ShapeRenderer shapeRenderer) {
        if (!active || shapeRenderer == null) {
            return;
        }
        for (SlimeHazard hazard : slimeHazards) {
            hazard.render(shapeRenderer);
        }
        for (int i = segments.size() - 1; i >= 0; i--) {
            segments.get(i).render(shapeRenderer);
        }
        super.render(shapeRenderer);
    }

    @Override
    public void render(SpriteBatch batch) {
        if (!active || batch == null) {
            return;
        }
        for (SlimeHazard hazard : slimeHazards) {
            hazard.render(batch);
        }
        for (int i = segments.size() - 1; i >= 0; i--) {
            segments.get(i).render(batch);
        }
        super.render(batch);
    }

    public boolean isInsideMapBounds() {
        return mapBounds.contains(getHeadCenterX(), getHeadCenterY());
    }

    public float getHeadCenterX() {
        return x + width * 0.5f;
    }

    public float getHeadCenterY() {
        return y + height * 0.5f;
    }

    public void setHeadCenter(float cx, float cy) {
        this.x = cx - width * 0.5f;
        this.y = cy - height * 0.5f;
    }

    private static float normalizeAngleRad(float angleRad) {
        while (angleRad > MathUtils.PI) angleRad -= MathUtils.PI2;
        while (angleRad < -MathUtils.PI) angleRad += MathUtils.PI2;
        return angleRad;
    }

    public Stage getCurrentStage() {
        return currentStage;
    }

    public List<WormSegment> getSegments() {
        return Collections.unmodifiableList(segments);
    }

    public List<SlimeHazard> getSlimeHazards() {
        return Collections.unmodifiableList(slimeHazards);
    }

    public boolean isUsingSegmentedHitboxes() {
        return useSegmentedHitboxes;
    }

    public float getCurrentHeadingRad() {
        return currentHeadingRad;
    }

    public void setCurrentHeadingRad(float headingRad) {
        this.currentHeadingRad = normalizeAngleRad(headingRad);
    }

    public Rectangle getMapBounds() {
        return new Rectangle(mapBounds);
    }

    public int getStage3PatternIndex() {
        return stage3PatternIndex;
    }

    public void setStage3CycleDuration(float stage3CycleDuration) {
        this.stage3CycleDuration = Math.max(0.2f, stage3CycleDuration);
    }
}
