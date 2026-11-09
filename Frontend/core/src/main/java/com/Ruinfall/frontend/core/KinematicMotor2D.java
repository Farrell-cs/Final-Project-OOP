package com.Ruinfall.frontend.core;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;
import java.util.List;

public class KinematicMotor2D {
    private static final float SKIN_WIDTH = 0.015f;
    private static final float GROUND_PROBE_DIST = 0.04f;

    private final Vector2 position = new Vector2();
    private final Vector2 currentColliderSize = new Vector2(0.8f, 1.375f);
    private final Vector2 standingSize = new Vector2(0.8f, 1.375f);
    private final Vector2 duckingSize = new Vector2(0.8f, 0.75f);

    private final List<ISolid> solids = new ArrayList<>();
    private final Vector2 rawLiftSpeed = new Vector2();
    private final Vector2 maxLiftBoost = new Vector2(31.25f, 16.25f);

    private boolean isDucking;
    private boolean isGrounded;
    private float cornerCorrectionDistance = 0.50f;
    private float dashFloorPopDistance = 0.50f;
    private boolean allowDashFloorPop;
    private int preferredCeilingNudgeDir;
    private float liftBoostTimer;
    private float liftBoostGraceDuration = 0.15f;

    public KinematicMotor2D() {
        setDucking(false);
    }

    public void configure(
        Vector2 normalSize,
        Vector2 duckSize,
        float cornerDist,
        float popDist,
        float maxLiftX,
        float maxLiftY,
        float liftGrace
    ) {
        standingSize.set(normalSize);
        duckingSize.set(duckSize);
        cornerCorrectionDistance = cornerDist;
        dashFloorPopDistance = popDist;
        maxLiftBoost.set(maxLiftX, maxLiftY);
        liftBoostGraceDuration = liftGrace;
        currentColliderSize.set(isDucking ? duckingSize : standingSize);
    }

    public void setSolids(List<? extends ISolid> newSolids) {
        solids.clear();
        solids.addAll(newSolids);
    }

    public void addSolid(ISolid solid) {
        solids.add(solid);
    }

    public Vector2 getPosition() {
        return position;
    }

    public void teleport(float x, float y) {
        position.set(x, y);
        rawLiftSpeed.setZero();
        liftBoostTimer = 0.0f;
    }

    public Vector2 getCurrentColliderSize() {
        return currentColliderSize;
    }

    public boolean isDucking() {
        return isDucking;
    }

    public boolean isGrounded() {
        return isGrounded;
    }

    public void setAllowDashFloorPop(boolean allow) {
        allowDashFloorPop = allow;
    }

    public void setPreferredCeilingNudgeDir(int dir) {
        preferredCeilingNudgeDir = dir;
    }

    public void registerLiftSpeed(Vector2 solidVelocity) {
        rawLiftSpeed.set(solidVelocity);
        liftBoostTimer = liftBoostGraceDuration;
    }

    public Vector2 getClampedLiftBoost() {
        if (liftBoostTimer <= 0.0f) {
            return new Vector2();
        }
        float bx = MathUtils.clamp(rawLiftSpeed.x, -maxLiftBoost.x, maxLiftBoost.x);
        float by = MathUtils.clamp(Math.max(0.0f, rawLiftSpeed.y), 0.0f, maxLiftBoost.y);
        return new Vector2(bx, by);
    }

    public void tickTimers(float dt) {
        if (liftBoostTimer > 0.0f) {
            liftBoostTimer -= dt;
            if (liftBoostTimer <= 0.0f) {
                rawLiftSpeed.setZero();
            }
        }
    }

    public boolean setDucking(boolean shouldDuck) {
        if (shouldDuck == isDucking) {
            return true;
        }
        if (!shouldDuck && !canUnduck()) {
            return false;
        }
        isDucking = shouldDuck;
        currentColliderSize.set(isDucking ? duckingSize : standingSize);
        return true;
    }

    public boolean canUnduck() {
        if (!isDucking) {
            return true;
        }
        Rectangle testBox = getBoxAt(position.x, position.y, standingSize.x - SKIN_WIDTH * 2.0f, standingSize.y - SKIN_WIDTH * 2.0f);
        return !overlapsAnySolid(testBox);
    }

    public void move(Vector2 velocity, float dt, Runnable onCollideX, Runnable onCollideY) {
        moveX(velocity.x * dt, onCollideX);
        moveY(velocity.y * dt, onCollideY);
        updateGroundState(velocity.y);
    }

    public void moveX(float deltaX, Runnable onCollideX) {
        if (Math.abs(deltaX) < 0.00001f) {
            return;
        }

        int sign = deltaX > 0.0f ? 1 : -1;
        float distance = Math.abs(deltaX);

        SolidHit hit = castSolid(position.x, position.y, sign, 0, distance);
        if (hit.hit) {
            float safeMove = Math.max(0.0f, hit.distance - SKIN_WIDTH);
            position.x += sign * safeMove;
            float remaining = distance - safeMove;

            captureLiftSpeed(hit.solid);

            if (allowDashFloorPop && remaining > 0.0001f && tryPopUpOntoLedge(sign, remaining)) {
                return;
            }

            if (onCollideX != null) {
                onCollideX.run();
            }
        } else {
            position.x += sign * distance;
        }
    }

    public void moveY(float deltaY, Runnable onCollideY) {
        if (Math.abs(deltaY) < 0.00001f) {
            return;
        }

        int sign = deltaY > 0.0f ? 1 : -1;
        float distance = Math.abs(deltaY);

        SolidHit hit = castSolid(position.x, position.y, 0, sign, distance);
        if (hit.hit) {
            float safeMove = Math.max(0.0f, hit.distance - SKIN_WIDTH);
            position.y += sign * safeMove;
            float remaining = distance - safeMove;

            captureLiftSpeed(hit.solid);

            if (sign > 0 && remaining > 0.0001f && tryNudgePastCeilingCorner(remaining)) {
                return;
            }

            if (onCollideY != null) {
                onCollideY.run();
            }
        } else {
            position.y += sign * distance;
        }
    }

    public void updateGroundState(float currentVy) {
        if (currentVy > 0.05f) {
            isGrounded = false;
            return;
        }
        SolidHit hit = castSolid(position.x, position.y, 0, -1, GROUND_PROBE_DIST);
        isGrounded = hit.hit;
        if (isGrounded) {
            captureLiftSpeed(hit.solid);
        }
    }

    public boolean checkWall(int dirX, float probeDistance) {
        if (dirX == 0) {
            return false;
        }
        SolidHit hit = castSolid(position.x, position.y, (int) Math.signum(dirX), 0, probeDistance);
        if (hit.hit) {
            captureLiftSpeed(hit.solid);
            return true;
        }
        return false;
    }

    public boolean checkOpenSpaceAtOffset(float offsetX, float offsetY) {
        Rectangle testBox = getBoxAt(
            position.x + offsetX,
            position.y + offsetY,
            currentColliderSize.x - SKIN_WIDTH * 2.0f,
            currentColliderSize.y - SKIN_WIDTH * 2.0f
        );
        return !overlapsAnySolid(testBox);
    }

    public Rectangle getBounds() {
        return new Rectangle(
            position.x - currentColliderSize.x * 0.5f,
            position.y,
            currentColliderSize.x,
            currentColliderSize.y
        );
    }

    private boolean tryNudgePastCeilingCorner(float remainingY) {
        float step = 0.05f;
        int[] dirs = preferredCeilingNudgeDir > 0
            ? new int[]{1}
            : preferredCeilingNudgeDir < 0
                ? new int[]{-1}
                : new int[]{-1, 1};

        for (float offset = step; offset <= cornerCorrectionDistance + 0.001f; offset += step) {
            for (int dir : dirs) {
                SolidHit sideHit = castSolid(position.x, position.y, dir, 0, offset);
                if (sideHit.hit) {
                    continue;
                }
                float testX = position.x + dir * offset;
                SolidHit upHit = castSolid(testX, position.y, 0, 1, Math.max(0.08f, remainingY));
                if (!upHit.hit) {
                    position.x = testX;
                    position.y += remainingY;
                    return true;
                }
            }
        }
        return false;
    }

    private boolean tryPopUpOntoLedge(int dirX, float remainingX) {
        float step = 0.05f;
        for (float offset = step; offset <= dashFloorPopDistance + 0.001f; offset += step) {
            SolidHit upHit = castSolid(position.x, position.y, 0, 1, offset);
            if (upHit.hit) {
                break;
            }
            float testY = position.y + offset;
            SolidHit forwardHit = castSolid(position.x, testY, dirX, 0, Math.max(0.08f, remainingX));
            if (!forwardHit.hit) {
                position.y = testY;
                position.x += dirX * remainingX;
                return true;
            }
        }
        return false;
    }

    private SolidHit castSolid(float originX, float originY, int dirX, int dirY, float distance) {
        Rectangle box = getBoxAt(originX, originY, currentColliderSize.x - SKIN_WIDTH * 2.0f, currentColliderSize.y - SKIN_WIDTH * 2.0f);
        SolidHit closest = new SolidHit();
        closest.distance = Float.MAX_VALUE;

        for (ISolid solid : solids) {
            Rectangle s = solid.getBounds();

            if (dirX != 0) {
                boolean yOverlaps = box.y < s.y + s.height && box.y + box.height > s.y;
                if (!yOverlaps) continue;

                if (dirX > 0) {
                    if (box.x + box.width <= s.x + 0.001f) {
                        float dist = s.x - (box.x + box.width);
                        if (dist <= distance + SKIN_WIDTH && dist < closest.distance) {
                            closest.hit = true;
                            closest.distance = Math.max(0.0f, dist);
                            closest.solid = solid;
                        }
                    }
                } else {
                    if (box.x >= s.x + s.width - 0.001f) {
                        float dist = box.x - (s.x + s.width);
                        if (dist <= distance + SKIN_WIDTH && dist < closest.distance) {
                            closest.hit = true;
                            closest.distance = Math.max(0.0f, dist);
                            closest.solid = solid;
                        }
                    }
                }
            } else if (dirY != 0) {
                boolean xOverlaps = box.x < s.x + s.width && box.x + box.width > s.x;
                if (!xOverlaps) continue;

                if (dirY > 0) {
                    if (box.y + box.height <= s.y + 0.001f) {
                        float dist = s.y - (box.y + box.height);
                        if (dist <= distance + SKIN_WIDTH && dist < closest.distance) {
                            closest.hit = true;
                            closest.distance = Math.max(0.0f, dist);
                            closest.solid = solid;
                        }
                    }
                } else {
                    if (box.y >= s.y + s.height - 0.001f) {
                        float dist = box.y - (s.y + s.height);
                        if (dist <= distance + SKIN_WIDTH && dist < closest.distance) {
                            closest.hit = true;
                            closest.distance = Math.max(0.0f, dist);
                            closest.solid = solid;
                        }
                    }
                }
            }
        }

        return closest;
    }

    private boolean overlapsAnySolid(Rectangle box) {
        for (ISolid solid : solids) {
            if (box.overlaps(solid.getBounds())) {
                return true;
            }
        }
        return false;
    }

    private void captureLiftSpeed(ISolid solid) {
        if (solid != null) {
            registerLiftSpeed(solid.getLiftSpeed());
        }
    }

    private Rectangle getBoxAt(float x, float y, float width, float height) {
        return new Rectangle(x - width * 0.5f, y, width, height);
    }

    private static class SolidHit {
        boolean hit;
        float distance;
        ISolid solid;
    }
}
