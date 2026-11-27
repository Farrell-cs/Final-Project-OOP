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
 * Hybrid VFX & Game Feel coordinator (Singleton + GameEventBus Observer).
 * Bridges WormBoss, BubbleBullet, RegeneratingStalactite, and Barrel events
 * directly into stage-1-tutorial's unified ParticleSystem, VfxManager, and CameraFollow
 * while retaining full standalone/headless compatibility for unit tests.
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

    // Fallback instances for headless tests; replaced/bridged at runtime via bindRuntime()
    private ParticleSystem particles = new ParticleSystem();
    private final CameraShake cameraShake = new CameraShake();
    private final HitstopManager hitstop = new HitstopManager();
    private VfxManager runtimeVfx;
    private CameraFollow runtimeCamera;

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

    /**
     * Binds the active stage-1-tutorial runtime engine (ParticleSystem, VfxManager, CameraFollow)
     * so all GameEventBus VFX route through a single unified pipeline.
     */
    public void bindRuntime(ParticleSystem runtimeParticles, VfxManager vfx, CameraFollow camera) {
        if (runtimeParticles != null) {
            this.particles = runtimeParticles;
        }
        this.runtimeVfx = vfx;
        this.runtimeCamera = camera;
    }

    private void triggerTrauma(float amount) {
        cameraShake.addTrauma(amount);
        if (runtimeCamera != null) {
            runtimeCamera.addTrauma(amount);
        }
    }

    private void triggerHitstop(float duration) {
        hitstop.trigger(duration);
        if (runtimeVfx != null) {
            runtimeVfx.triggerHitStop(duration);
        }
    }

    private void triggerFlash(Color color, float alpha, float duration) {
        flashColor.set(color);
        flashDuration = duration;
        flashTimer = duration;
        if (runtimeVfx != null) {
            runtimeVfx.triggerScreenFlash(color, alpha, duration);
        }
    }

    private void triggerRing(float x, float y, float startR, float maxR, float life, float thick, Color color) {
        particles.emitRing(x, y, startR, maxR, life, thick, color);
        if (runtimeVfx != null) {
            runtimeVfx.triggerImpactRing(x, y, color, maxR, life);
        }
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
                triggerTrauma(0.15f);
            }
        });

        bus.subscribe(GameEventBus.GameEvent.ROCK_SHATTERED, (event, payload) -> {
            if (payload instanceof Stalactite s) {
                float cx = s.getX() + s.getWidth() * 0.5f;
                float cy = s.getY() + s.getHeight() * 0.5f;
                particles.emitBurst(cx, cy, 14, 3f, 10f, 2f, 28f, 0.14f, 0.28f, 0.30f, 0.55f, new Color(0.55f, 0.62f, 0.70f, 1f));
                triggerTrauma(0.30f);
            }
        });

        bus.subscribe(GameEventBus.GameEvent.CRYSTAL_SHATTERED, (event, payload) -> {
            if (payload instanceof CrystalStalactite s) {
                float cx = s.getX() + s.getWidth() * 0.5f;
                float cy = s.getY() + s.getHeight() * 0.5f;
                particles.emitBurst(cx, cy, 16, 4f, 11f, 2f, 18f, 0.14f, 0.28f, 0.25f, 0.50f, new Color(0.45f, 0.96f, 1f, 1f));
                triggerTrauma(0.32f);
            }
        });

        bus.subscribe(GameEventBus.GameEvent.BARREL_HIT, (event, payload) -> {
            if (payload instanceof Barrels.BarrelHitPayload p) {
                particles.emitBurst(p.x, p.y, 8, 2f, 7f, 3f, 18f, 0.15f, 0.30f, 0.12f, 0.22f, new Color(0.75f, 0.52f, 0.28f, 1f));
                triggerTrauma(0.18f);
            } else if (payload instanceof PointPayload p) {
                particles.emitBurst(p.x, p.y, 8, 2f, 7f, 3f, 18f, 0.15f, 0.30f, 0.12f, 0.22f, new Color(0.75f, 0.52f, 0.28f, 1f));
                triggerTrauma(0.18f);
            }
        });

        bus.subscribe(GameEventBus.GameEvent.BARREL_EXPLODED, (event, payload) -> {
            float ex = 0f, ey = 0f;
            if (payload instanceof Barrels.BarrelExplosionPayload p) {
                ex = p.x;
                ey = p.y;
            }
            particles.emitBurst(ex, ey, 22, 4f, 14f, 2.5f, 22f, 0.20f, 0.55f, 0.14f, 0.28f, new Color(1f, 0.62f, 0.18f, 1f));
            triggerRing(ex, ey, 0.3f, 3.6f, 0.30f, 0.16f, new Color(1f, 0.78f, 0.30f, 0.95f));
            triggerTrauma(0.55f);
            triggerHitstop(0.06f);
            triggerFlash(new Color(1f, 0.85f, 0.5f, 1f), 0.30f, 0.10f);
        });

        bus.subscribe(GameEventBus.GameEvent.BOSS_TUNNEL_BREACH, (event, payload) -> {
            if (payload instanceof TunnelBreachPayload p) {
                float angleDeg = MathUtils.atan2(p.dirY, p.dirX) * MathUtils.radiansToDegrees;
                particles.emitDirectional(p.x, p.y, angleDeg, 75f, 18, 5f, 15f, 2f, 20f, 0.22f, 0.50f, new Color(0.60f, 0.48f, 0.38f, 1f));
                triggerRing(p.x, p.y, 0.4f, 3.2f, 0.28f, 0.14f, new Color(0.85f, 0.72f, 0.48f, 0.9f));
                triggerTrauma(0.45f);
            }
        });

        bus.subscribe(GameEventBus.GameEvent.BOSS_STAGE_CHANGED, (event, payload) -> {
            if (payload instanceof StageChangePayload p) {
                particles.emitBurst(p.x, p.y, 28, 5f, 16f, 2f, 10f, 0.30f, 0.70f, 0.14f, 0.30f, new Color(0.95f, 0.35f, 0.85f, 1f));
                triggerRing(p.x, p.y, 0.5f, 5.5f, 0.42f, 0.18f, new Color(0.95f, 0.45f, 0.90f, 0.95f));
                triggerTrauma(0.75f);
                triggerHitstop(0.10f);
                triggerFlash(new Color(1f, 0.75f, 0.95f, 1f), 0.42f, 0.18f);
            }
        });

        bus.subscribe(GameEventBus.GameEvent.BUBBLE_POPPED, (event, payload) -> {
            if (payload instanceof BubblePopPayload p) {
                particles.emitBurst(p.x, p.y, 12, 3f, 9f, 3f, 8f, 0.15f, 0.32f, 0.10f, 0.18f, new Color(0.45f, 0.92f, 1f, 0.95f));
                triggerRing(p.x, p.y, 0.2f, 1.8f, 0.20f, 0.10f, new Color(0.55f, 0.95f, 1f, 0.85f));
            }
        });

        bus.subscribe(GameEventBus.GameEvent.BULLET_HIT, (event, payload) -> {
            if (payload instanceof PointPayload p) {
                particles.emitBurst(p.x, p.y, 10, 2.5f, 8f, 3f, 12f, 0.12f, 0.25f, 0.08f, 0.16f, new Color(1f, 0.42f, 0.42f, 1f));
                triggerTrauma(0.22f);
            }
        });
    }

    public void onStalactiteShaking(float x, float y) {
        particles.emitBurst(x, y, 2, 0.5f, 2.2f, 1f, 18f, 0.20f, 0.35f, 0.07f, 0.12f, new Color(0.65f, 0.70f, 0.72f, 0.8f));
    }

    public void onStalactiteRegenerating(float x, float y) {
        onStalactiteRegenerating(x, y, 0.5f);
    }

    public void onStalactiteRegenerating(float x, float y, float progress) {
        if (MathUtils.random() < 0.25f) {
            particles.emitBurst(x, y, 1, 0.4f, 1.8f, 2f, -4f, 0.18f, 0.35f, 0.06f, 0.12f, new Color(0.45f, 0.95f, 0.82f, 0.85f));
        }
    }

    public void update(float dt) {
        cameraShake.update(dt);
        if (flashTimer > 0f) {
            flashTimer = Math.max(0f, flashTimer - dt);
        }
        // Only tick standalone particles if not bound to Main's ParticleSystem (which Main updates)
        if (runtimeVfx == null) {
            particles.update(dt);
        }
    }

    public void render(SpriteBatch batch, Texture pixelTexture) {
        if (runtimeVfx == null) {
            particles.render(batch, null);
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
