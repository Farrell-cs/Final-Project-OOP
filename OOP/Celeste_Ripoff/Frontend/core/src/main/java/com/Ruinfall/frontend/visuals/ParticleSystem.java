package com.Ruinfall.frontend.visuals;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Pooled particle and shockwave ring system to avoid GC allocations during combat.
 */
public class ParticleSystem {
    public static final class Particle {
        float x, y;
        float vx, vy;
        float drag;
        float gravity;
        float startSize;
        float endScale;
        float life;
        float maxLife;
        final Color color = new Color();

        void init(
            float x, float y, float vx, float vy,
            float drag, float gravity,
            float startSize, float endScale, float maxLife,
            Color color
        ) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.drag = Math.max(0f, drag);
            this.gravity = gravity;
            this.startSize = startSize;
            this.endScale = endScale;
            this.maxLife = Math.max(0.01f, maxLife);
            this.life = this.maxLife;
            this.color.set(color);
        }
    }

    public static final class ShockwaveRing {
        float x, y;
        float startRadius;
        float maxRadius;
        float thickness;
        float life;
        float maxLife;
        final Color color = new Color();

        void init(float x, float y, float startRadius, float maxRadius, float thickness, float maxLife, Color color) {
            this.x = x;
            this.y = y;
            this.startRadius = startRadius;
            this.maxRadius = maxRadius;
            this.thickness = thickness;
            this.maxLife = Math.max(0.01f, maxLife);
            this.life = this.maxLife;
            this.color.set(color);
        }
    }

    private final List<Particle> activeParticles = new ArrayList<>();
    private final Queue<Particle> particlePool = new ArrayDeque<>();

    private final List<ShockwaveRing> activeRings = new ArrayList<>();
    private final Queue<ShockwaveRing> ringPool = new ArrayDeque<>();

    public ParticleSystem() {
        prewarm(256, 16);
    }

    public void prewarm(int particleCount, int ringCount) {
        for (int i = 0; i < particleCount; i++) {
            particlePool.offer(new Particle());
        }
        for (int i = 0; i < ringCount; i++) {
            ringPool.offer(new ShockwaveRing());
        }
    }

    public void emit(
        float x, float y, float vx, float vy,
        float drag, float gravity,
        float size, float endScale, float life,
        Color color
    ) {
        Particle p = particlePool.isEmpty() ? new Particle() : particlePool.poll();
        p.init(x, y, vx, vy, drag, gravity, size, endScale, life, color);
        activeParticles.add(p);
    }

    public void emitBurst(
        float x, float y, int count,
        float minSpeed, float maxSpeed,
        float drag, float gravity,
        float minSize, float maxSize,
        float minLife, float maxLife,
        Color color
    ) {
        for (int i = 0; i < count; i++) {
            float angle = MathUtils.random(0f, MathUtils.PI2);
            float speed = MathUtils.random(minSpeed, maxSpeed);
            float size = MathUtils.random(minSize, maxSize);
            float life = MathUtils.random(minLife, maxLife);
            emit(
                x, y,
                MathUtils.cos(angle) * speed,
                MathUtils.sin(angle) * speed,
                drag, gravity, size, 0.2f, life, color
            );
        }
    }

    public void emitDirectional(
        float x, float y, float baseAngleDeg, float spreadDeg,
        int count, float minSpeed, float maxSpeed,
        float drag, float gravity,
        float size, float life, Color color
    ) {
        float half = spreadDeg * 0.5f;
        for (int i = 0; i < count; i++) {
            float angleRad = (baseAngleDeg + MathUtils.random(-half, half)) * MathUtils.degreesToRadians;
            float speed = MathUtils.random(minSpeed, maxSpeed);
            emit(
                x, y,
                MathUtils.cos(angleRad) * speed,
                MathUtils.sin(angleRad) * speed,
                drag, gravity, size, 0.15f, life, color
            );
        }
    }

    public void emitShockwave(float x, float y, float startRadius, float maxRadius, float thickness, float life, Color color) {
        ShockwaveRing r = ringPool.isEmpty() ? new ShockwaveRing() : ringPool.poll();
        r.init(x, y, startRadius, maxRadius, thickness, life, color);
        activeRings.add(r);
    }

    public void update(float dt) {
        for (int i = activeParticles.size() - 1; i >= 0; i--) {
            Particle p = activeParticles.get(i);
            p.life -= dt;
            if (p.life <= 0f) {
                activeParticles.remove(i);
                particlePool.offer(p);
                continue;
            }
            float damp = Math.max(0f, 1.0f - p.drag * dt);
            p.vx *= damp;
            p.vy = p.vy * damp - p.gravity * dt;
            p.x += p.vx * dt;
            p.y += p.vy * dt;
        }

        for (int i = activeRings.size() - 1; i >= 0; i--) {
            ShockwaveRing r = activeRings.get(i);
            r.life -= dt;
            if (r.life <= 0f) {
                activeRings.remove(i);
                ringPool.offer(r);
            }
        }
    }

    public void render(SpriteBatch batch, Texture pixelTexture) {
        if (batch == null || pixelTexture == null) {
            return;
        }
        Color prev = batch.getColor();

        for (Particle p : activeParticles) {
            float t = MathUtils.clamp(p.life / p.maxLife, 0f, 1f);
            float scale = MathUtils.lerp(p.endScale, 1.0f, t);
            float size = p.startSize * scale;
            batch.setColor(p.color.r, p.color.g, p.color.b, p.color.a * t);
            batch.draw(pixelTexture, p.x - size * 0.5f, p.y - size * 0.5f, size, size);
        }

        final int segments = 20;
        for (ShockwaveRing r : activeRings) {
            float progress = 1.0f - MathUtils.clamp(r.life / r.maxLife, 0f, 1f);
            float radius = MathUtils.lerp(r.startRadius, r.maxRadius, progress);
            float alpha = (1.0f - progress) * r.color.a;
            batch.setColor(r.color.r, r.color.g, r.color.b, alpha);
            float segLen = (MathUtils.PI2 * radius) / segments;
            for (int s = 0; s < segments; s++) {
                float a = (s / (float) segments) * MathUtils.PI2;
                float px = r.x + MathUtils.cos(a) * radius;
                float py = r.y + MathUtils.sin(a) * radius;
                batch.draw(pixelTexture, px - segLen * 0.5f, py - r.thickness * 0.5f, segLen, r.thickness);
            }
        }

        batch.setColor(prev);
    }

    public int getActiveParticleCount() {
        return activeParticles.size();
    }

    public int getPooledParticleCount() {
        return particlePool.size();
    }

    public int getActiveRingCount() {
        return activeRings.size();
    }

    public void clear() {
        while (!activeParticles.isEmpty()) {
            particlePool.offer(activeParticles.remove(activeParticles.size() - 1));
        }
        while (!activeRings.isEmpty()) {
            ringPool.offer(activeRings.remove(activeRings.size() - 1));
        }
    }
}
