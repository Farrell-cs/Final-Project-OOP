package com.Ruinfall.frontend.visuals;

import com.Ruinfall.frontend.objects.Interactables.Barrels;
import com.Ruinfall.frontend.objects.Interactables.CrystalStalactite;
import com.Ruinfall.frontend.objects.Interactables.Stalactite;
import com.Ruinfall.frontend.systems.GameEventBus;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

/**
 * Central VFX & Game Feel coordinator (Singleton + GameEventBus Observer).
 * Owns ParticleSystem, CameraShake, HitstopManager, and screen flash frames.
 */
public class EffectManager {
    public static final class TunnelBreachPayload {
        public final float x, y, dirX, dirY;
        public TunnelBreachPayload(float x, float y, float dirX, float dirY) {
            this.x = x;
            this.y = y;
            this.dirX = dirX;
            this.dirY = dirY;
        }
    }

    public static final class StageChangePayload {
        public final float x, y;
        public final int stageNumber;
        public StageChangePayload(float x, float y, int stageNumber) {
            this.x = x;
            this.y = y;
            this.stageNumber = stageNumber;
        }
    }

    public static final class BulletVfxPayload {
        public final float x, y, vx, vy;
        public final boolean isBubble;
        public final boolean isPlayerBullet;
        public BulletVfxPayload(float x, float y, float vx, float vy, boolean isBubble, boolean isPlayerBullet) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.isBubble = isBubble;
            this.isPlayerBullet = isPlayerBullet;
        }
    }

    public static final class BubblePopPayload {
        public final float x, y;
        public final boolean diagonal;
        public BubblePopPayload(float x, float y, boolean diagonal) {
            this.x = x;
            this.y = y;
            this.diagonal = diagonal;
        }
    }

    public static final class DashVfxPayload {
        public final float x, y, dirX, dirY;
        public DashVfxPayload(float x, float y, float dirX, float dirY) {
            this.x = x;
            this.y = y;
            this.dirX = dirX;
            this.dirY = dirY;
        }
    }

    public static final class PointPayload {
        public final float x, y;
        public PointPayload(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    private static EffectManager instance;

    private final ParticleSystem particles = new ParticleSystem();
    private final CameraShake cameraShake = new CameraShake();
    private final HitstopManager hitstop = new HitstopManager();

    private float flashTimer;
    private float flashDuration = 0.10f;
    private final Color flashColor = new Color(1f, 0.95f, 0.8f, 0.45f);

    private EffectManager() {
        subscribeToEvents();
    }

    public static EffectManager getInstance() {
        if (instance == null) {
            instance = new EffectManager();
        }
        return instance;
    }

    public void subscribeToEvents() {
        GameEventBus bus = GameEventBus.getInstance();

        bus.subscribe(GameEventBus.GameEvent.PLAYER_DASH_START, (event, payload) -> {
            if (payload instanceof DashVfxPayload p) {
                float angleDeg = MathUtils.atan2(-p.dirY, -p.dirX) * MathUtils.radiansToDegrees;
                particles.emitDirectional(p.x, p.y, angleDeg, 55f, 10, 4f, 11f, 4f, 0f, 0.22f, 0.28f, new Color(0.75f, 0.92f, 1f, 0.9f));
            }
        });

        bus.subscribe(GameEventBus.GameEvent.PLAYER_DASH_END, (event, payload) -> {
            if (payload instanceof DashVfxPayload p) {
                float angleDeg = MathUtils.atan2(p.dirY, p.dirX) * MathUtils.radiansToDegrees;
                particles.emitDirectional(p.x, p.y, angleDeg, 70f, 6, 2f, 6f, 5f, 0f, 0.16f, 0.22f, new Color(0.85f, 0.95f, 1f, 0.7f));
            }
        });

        bus.subscribe(GameEventBus.GameEvent.GRAPPLE_LATCHED, (event, payload) -> {
            if (payload instanceof PointPayload p) {
                particles.emitBurst(p.x, p.y, 12, 3f, 9f, 5f, 4f, 0.12f, 0.22f, 0.18f, 0.30f, new Color(1f, 0.92f, 0.35f, 1f));
                cameraShake.addTrauma(0.15f);
            }
        });

        bus.subscribe(GameEventBus.GameEvent.ROCK_SHATTERED, (event, payload) -> {
            if (payload instanceof Stalactite s) {
                float cx = s.getX() + s.getWidth() * 0.5f;
                float cy = s.getY() + s.getHeight() * 0.5f;
                particles.emitBurst(cx, cy, 14, 3f, 10f, 2f, 28f, 0.14f, 0.28f, 0.30f, 0.55f, new Color(0.55f, 0.62f, 0.70f, 1f));
                particles.emitBurst(cx, cy, 8, 1.5f, 4.5f, 4f, 0f, 0.25f, 0.45f, 0.25f, 0.45f, new Color(0.80f, 0.82f, 0.85f, 0.65f));
                cameraShake.addTrauma(0.22f);
            }
        });

        bus.subscribe(GameEventBus.GameEvent.CRYSTAL_SHATTERED, (event, payload) -> {
            if (payload instanceof CrystalStalactite s) {
                float cx = s.getX() + s.getWidth() * 0.5f;
                float cy = s.getY() + s.getHeight() * 0.5f;
                particles.emitBurst(cx, cy, 14, 3f, 10f, 2f, 22f, 0.12f, 0.24f, 0.28f, 0.50f, Color.CYAN);
                cameraShake.addTrauma(0.20f);
            }
        });

        bus.subscribe(GameEventBus.GameEvent.BARREL_HIT, (event, payload) -> {
            if (payload instanceof Barrels.BarrelHitPayload p) {
                // Wood splinter burst
                particles.emitBurst(p.x, p.y, 7, 2.5f, 7.5f, 2.5f, 22f, 0.12f, 0.22f, 0.20f, 0.36f, new Color(0.55f, 0.34f, 0.16f, 1f));
                // Metal hoop spark burst
                particles.emitBurst(p.x, p.y, 6, 4.0f, 10.5f, 4.0f, 10f, 0.08f, 0.16f, 0.14f, 0.26f, new Color(1.0f, 0.85f, 0.35f, 1f));
                cameraShake.addTrauma(0.08f);
            }
        });

        bus.subscribe(GameEventBus.GameEvent.BARREL_EXPLODED, (event, payload) -> {
            if (payload instanceof Barrels.BarrelExplosionPayload p) {
                float cx = p.x + 0.7f;
                float cy = p.y + 0.8f;
                particles.emitShockwave(cx, cy, 0.4f, 4.5f, 0.22f, 0.28f, new Color(1f, 0.75f, 0.25f, 0.95f));
                particles.emitBurst(cx, cy, 20, 4f, 14f, 3f, 5f, 0.18f, 0.36f, 0.28f, 0.52f, new Color(1f, 0.48f, 0.12f, 1f));
                particles.emitBurst(cx, cy, 12, 3f, 11f, 1.5f, 26f, 0.16f, 0.30f, 0.35f, 0.65f, new Color(0.45f, 0.28f, 0.14f, 1f));
                cameraShake.addTrauma(0.55f);
                hitstop.trigger(0.045f);
                triggerFlash(new Color(1f, 0.88f, 0.55f, 0.45f), 0.10f);
            }
        });

        bus.subscribe(GameEventBus.GameEvent.BOSS_TUNNEL_BREACH, (event, payload) -> {
            if (payload instanceof TunnelBreachPayload p) {
                float backAngle = MathUtils.atan2(-p.dirY, -p.dirX) * MathUtils.radiansToDegrees;
                particles.emitDirectional(p.x, p.y, backAngle, 75f, 14, 3f, 10f, 2f, 22f, 0.22f, 0.45f, new Color(0.52f, 0.42f, 0.32f, 0.95f));
                particles.emitBurst(p.x, p.y, 8, 1.5f, 5f, 4f, 0f, 0.30f, 0.50f, 0.25f, 0.45f, new Color(0.70f, 0.65f, 0.60f, 0.65f));
                cameraShake.addTrauma(0.25f);
            }
        });

        bus.subscribe(GameEventBus.GameEvent.BOSS_STAGE_CHANGED, (event, payload) -> {
            if (payload instanceof StageChangePayload p) {
                Color ringColor = (p.stageNumber >= 3) ? Color.MAGENTA : Color.GOLD;
                particles.emitShockwave(p.x, p.y, 0.6f, 7.5f, 0.28f, 0.42f, ringColor);
                particles.emitBurst(p.x, p.y, 32, 5f, 16f, 2.5f, 4f, 0.20f, 0.40f, 0.35f, 0.70f, ringColor);
                cameraShake.addTrauma(0.85f);
                hitstop.trigger(0.055f);
                triggerFlash(new Color(ringColor.r, ringColor.g, ringColor.b, 0.35f), 0.14f);
            }
        });

        bus.subscribe(GameEventBus.GameEvent.BULLET_SPAWNED, (event, payload) -> {
            if (payload instanceof BulletVfxPayload p) {
                Color c = p.isBubble ? new Color(0.55f, 0.90f, 1f, 0.8f) : new Color(1f, 0.78f, 0.30f, 0.9f);
                particles.emitBurst(p.x, p.y, 3, 1.5f, 4.0f, 6f, 0f, 0.12f, 0.20f, 0.10f, 0.18f, c);
            }
        });

        bus.subscribe(GameEventBus.GameEvent.BUBBLE_POPPED, (event, payload) -> {
            if (payload instanceof BubblePopPayload p) {
                particles.emitShockwave(p.x, p.y, 0.2f, 1.6f, 0.12f, 0.20f, new Color(0.45f, 0.88f, 1f, 0.85f));
                float baseDeg = p.diagonal ? 45f : 0f;
                for (int i = 0; i < 4; i++) {
                    particles.emitDirectional(p.x, p.y, baseDeg + i * 90f, 18f, 3, 4f, 8f, 4f, 0f, 0.14f, 0.22f, Color.CYAN);
                }
            }
        });

        bus.subscribe(GameEventBus.GameEvent.BULLET_HIT, (event, payload) -> {
            if (payload instanceof BulletVfxPayload p) {
                Color c = p.isPlayerBullet ? Color.CYAN : Color.ORANGE;
                particles.emitBurst(p.x, p.y, 6, 3f, 8f, 5f, 4f, 0.10f, 0.18f, 0.14f, 0.24f, c);
            }
        });
    }

    public void onStalactiteShaking(float x, float y) {
        if (MathUtils.randomBoolean(0.35f)) {
            particles.emit(x + MathUtils.random(-0.25f, 0.25f), y, MathUtils.random(-0.5f, 0.5f), -1.5f, 0f, 18f, 0.12f, 0.3f, 0.35f, new Color(0.65f, 0.70f, 0.75f, 0.85f));
        }
    }

    public void onStalactiteRegenerating(float x, float y) {
        if (MathUtils.randomBoolean(0.18f)) {
            particles.emit(x + MathUtils.random(-0.3f, 0.3f), y + MathUtils.random(-0.2f, 0.2f), MathUtils.random(-0.6f, 0.6f), MathUtils.random(0.4f, 1.4f), 2f, 0f, 0.11f, 0.1f, 0.30f, new Color(0.65f, 0.95f, 1f, 0.9f));
        }
    }

    public void onSlimeDebuffTick(float footX, float footY) {
        if (MathUtils.randomBoolean(0.28f)) {
            particles.emit(footX + MathUtils.random(-0.35f, 0.35f), footY + 0.05f, MathUtils.random(-0.8f, 0.8f), MathUtils.random(0.8f, 2.2f), 2f, 12f, 0.14f, 0.2f, 0.32f, new Color(0.35f, 0.95f, 0.25f, 0.85f));
        }
    }

    public void onHighVelocityTrail(float x, float y, float vx, float vy) {
        if (MathUtils.randomBoolean(0.4f)) {
            particles.emit(x, y, -vx * 0.12f, -vy * 0.12f, 5f, 0f, 0.14f, 0.1f, 0.20f, new Color(0.88f, 0.95f, 1f, 0.55f));
        }
    }

    public void triggerFlash(Color color, float duration) {
        this.flashColor.set(color);
        this.flashDuration = Math.max(0.02f, duration);
        this.flashTimer = this.flashDuration;
    }

    public void update(float rawDt) {
        particles.update(rawDt);
        cameraShake.update(rawDt);
        if (flashTimer > 0f) {
            flashTimer = Math.max(0f, flashTimer - rawDt);
        }
    }

    public void render(SpriteBatch batch, Texture pixelTexture, float camX, float camY, float viewW, float viewH) {
        particles.render(batch, pixelTexture);
        if (flashTimer > 0f && batch != null && pixelTexture != null) {
            Color prev = batch.getColor();
            float alpha = (flashTimer / flashDuration) * flashColor.a;
            batch.setColor(flashColor.r, flashColor.g, flashColor.b, alpha);
            batch.draw(pixelTexture, camX - viewW * 0.6f, camY - viewH * 0.6f, viewW * 1.2f, viewH * 1.2f);
            batch.setColor(prev);
        }
    }

    public ParticleSystem getParticles() { return particles; }
    public CameraShake getCameraShake() { return cameraShake; }
    public HitstopManager getHitstop() { return hitstop; }

    public void clearAll() {
        particles.clear();
        cameraShake.reset();
        hitstop.reset();
        flashTimer = 0f;
    }
}
