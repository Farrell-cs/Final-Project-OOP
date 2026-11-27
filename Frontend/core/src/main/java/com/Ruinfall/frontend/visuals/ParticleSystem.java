package com.Ruinfall.frontend.visuals;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

/**
 * Pooled, zero-allocation-per-frame procedural LibGDX particle engine.
 * Renders crisp squares, 45-degree diamonds, and velocity-aligned streaks
 * using {@link ProceduralTextures#getWhitePixel()} (zero external image assets).
 */
public class ParticleSystem {

    public enum Shape {
        SQUARE,
        DIAMOND,
        STREAK
    }

    private static final class Particle {
        float x, y;
        float vx, vy;
        float ax, ay;
        float drag;
        float life, maxLife;
        float startSize, endSize;
        float streakLengthMult = 1.0f;
        float rotation, rotSpeed;
        float startR, startG, startB, startA;
        float endR, endG, endB, endA;
        float wobbleAmp, wobbleFreq, wobblePhase;
        boolean additive;
        Shape shape = Shape.SQUARE;
    }

    public static final int MAX_PARTICLES = 1024;
    private final Particle[] pool = new Particle[MAX_PARTICLES];
    private int activeCount = 0;
    private int overwriteCursor = 0;
    private int activeRingCount = 0;

    private static final class SimpleRing {
        float life;
    }
    private final java.util.List<SimpleRing> simpleRings = new java.util.ArrayList<>();

    public ParticleSystem() {
        for (int i = 0; i < MAX_PARTICLES; i++) {
            pool[i] = new Particle();
        }
    }

    public void clear() {
        activeCount = 0;
        overwriteCursor = 0;
        simpleRings.clear();
        activeRingCount = 0;
    }

    public int getActiveCount() {
        return activeCount;
    }

    public int getActiveParticleCount() {
        return activeCount;
    }

    public int getPooledParticleCount() {
        return MAX_PARTICLES - activeCount;
    }

    public int getActiveRingCount() {
        return activeRingCount;
    }

    public void emitBurst(float x, float y, int count, float minSpeed, float maxSpeed,
                          float drag, float gravity, float minLife, float maxLife,
                          float minSize, float maxSize, Color color) {
        Color c = (color != null) ? color : Color.WHITE;
        Color endC = new Color(c.r, c.g, c.b, 0f);
        for (int i = 0; i < count; i++) {
            float angle = MathUtils.random(0f, MathUtils.PI2);
            float spd = MathUtils.random(minSpeed, maxSpeed);
            float life = MathUtils.random(minLife, maxLife);
            float size = MathUtils.random(minSize, maxSize);
            spawn(x, y,
                MathUtils.cos(angle) * spd, MathUtils.sin(angle) * spd,
                0f, -gravity, drag, life,
                size, size * 0.2f,
                c, endC, false, Shape.SQUARE);
        }
    }

    public void emitDirectional(float x, float y, float centerAngleDeg, float spreadDeg, int count,
                                float minSpeed, float maxSpeed, float drag, float gravity,
                                float minLife, float maxLife, Color color) {
        Color c = (color != null) ? color : Color.WHITE;
        Color endC = new Color(c.r, c.g, c.b, 0f);
        for (int i = 0; i < count; i++) {
            float angleRad = (centerAngleDeg + MathUtils.random(-spreadDeg * 0.5f, spreadDeg * 0.5f)) * MathUtils.degreesToRadians;
            float spd = MathUtils.random(minSpeed, maxSpeed);
            float life = MathUtils.random(minLife, maxLife);
            spawn(x, y,
                MathUtils.cos(angleRad) * spd, MathUtils.sin(angleRad) * spd,
                0f, -gravity, drag, life,
                0.14f, 0.03f,
                c, endC, false, Shape.STREAK);
        }
    }

    public void emitRing(float x, float y, float startRadius, float maxRadius, float life, float thickness, Color color) {
        SimpleRing r = new SimpleRing();
        r.life = Math.max(0.05f, life);
        simpleRings.add(r);
        activeRingCount = simpleRings.size();
        int sparks = 14;
        Color c = (color != null) ? color : Color.WHITE;
        Color endC = new Color(c.r, c.g, c.b, 0f);
        float speed = (maxRadius - startRadius) / Math.max(0.08f, life);
        for (int i = 0; i < sparks; i++) {
            float a = (i / (float) sparks) * MathUtils.PI2;
            spawn(x + MathUtils.cos(a) * startRadius, y + MathUtils.sin(a) * startRadius,
                MathUtils.cos(a) * speed, MathUtils.sin(a) * speed,
                0f, 0f, 2.5f, life,
                Math.max(0.10f, thickness), 0.02f,
                c, endC, true, Shape.DIAMOND);
        }
    }

    private Particle alloc() {
        if (activeCount < MAX_PARTICLES) {
            return pool[activeCount++];
        }
        // Ring overwrite cursor so saturated bursts overwrite older slots evenly
        Particle recycled = pool[overwriteCursor];
        overwriteCursor = (overwriteCursor + 1) % MAX_PARTICLES;
        return recycled;
    }

    public void spawn(
            float x, float y,
            float vx, float vy,
            float ax, float ay,
            float drag,
            float life,
            float startSize, float endSize,
            Color startColor, Color endColor,
            boolean additive,
            Shape shape
    ) {
        spawnFull(x, y, vx, vy, ax, ay, drag, life, startSize, endSize,
                1.0f, 0f, 0f, startColor, endColor, 0f, 0f, additive, shape);
    }

    public void spawnFull(
            float x, float y,
            float vx, float vy,
            float ax, float ay,
            float drag,
            float life,
            float startSize, float endSize,
            float streakLengthMult,
            float rotation, float rotSpeed,
            Color startColor, Color endColor,
            float wobbleAmp, float wobbleFreq,
            boolean additive,
            Shape shape
    ) {
        if (life <= 0f) return;
        Particle p = alloc();
        p.x = x;
        p.y = y;
        p.vx = vx;
        p.vy = vy;
        p.ax = ax;
        p.ay = ay;
        p.drag = drag;
        p.life = life;
        p.maxLife = life;
        p.startSize = startSize;
        p.endSize = endSize;
        p.streakLengthMult = streakLengthMult;
        p.rotation = rotation;
        p.rotSpeed = rotSpeed;
        p.startR = startColor.r;
        p.startG = startColor.g;
        p.startB = startColor.b;
        p.startA = startColor.a;
        p.endR = endColor.r;
        p.endG = endColor.g;
        p.endB = endColor.b;
        p.endA = endColor.a;
        p.wobbleAmp = wobbleAmp;
        p.wobbleFreq = wobbleFreq;
        p.wobblePhase = MathUtils.random(0f, MathUtils.PI2);
        p.additive = additive;
        p.shape = shape;
    }

    public void emitDust(float x, float y, float vx, float vy, float size, float life, Color color) {
        spawnFull(
                x, y, vx, vy,
                0f, 1.8f, 4.5f,
                life, size, size * 0.25f,
                1.0f, MathUtils.random(0f, 360f), MathUtils.random(-90f, 90f),
                color, new Color(color.r, color.g, color.b, 0f),
                0f, 0f, false, Shape.SQUARE
        );
    }

    public void emitSpark(float x, float y, float vx, float vy, float size, float life, Color startCol, Color endCol, boolean streak) {
        spawnFull(
                x, y, vx, vy,
                0f, -6.0f, 2.8f,
                life, size, size * 0.1f,
                streak ? 3.2f : 1.0f,
                45f, 0f,
                startCol, endCol,
                0f, 0f, true,
                streak ? Shape.STREAK : Shape.DIAMOND
        );
    }

    public void emitDebris(float x, float y, float vx, float vy, float size, float life, Color color) {
        spawnFull(
                x, y, vx, vy,
                0f, -34.0f, 0.8f,
                life, size, size * 0.6f,
                1.0f, MathUtils.random(0f, 360f), MathUtils.random(-320f, 320f),
                color, new Color(color.r, color.g, color.b, 0f),
                0f, 0f, false, Shape.SQUARE
        );
    }

    public void emitSpore(float x, float y, float vx, float vy, float size, float life, Color color) {
        spawnFull(
                x, y, vx, vy,
                0f, 0.12f, 0.2f,
                life, size * 0.7f, size,
                1.0f, 45f, MathUtils.random(-35f, 35f),
                color, new Color(color.r, color.g, color.b, 0f),
                MathUtils.random(0.25f, 0.65f), MathUtils.random(1.8f, 3.4f),
                true, Shape.DIAMOND
        );
    }

    public void emitEmber(float x, float y, float vx, float vy, float size, float life, Color startCol, Color endCol) {
        spawnFull(
                x, y, vx, vy,
                0f, 1.4f, 0.4f,
                life, size, size * 0.2f,
                2.2f, 45f, 0f,
                startCol, endCol,
                MathUtils.random(0.5f, 1.3f), MathUtils.random(3.5f, 6.5f),
                true, Shape.DIAMOND
        );
    }

    public void update(float dt) {
        if (dt <= 0f) return;
        for (int i = simpleRings.size() - 1; i >= 0; i--) {
            simpleRings.get(i).life -= dt;
            if (simpleRings.get(i).life <= 0f) {
                simpleRings.remove(i);
            }
        }
        activeRingCount = simpleRings.size();
        int i = 0;
        while (i < activeCount) {
            Particle p = pool[i];
            p.life -= dt;
            if (p.life <= 0f) {
                activeCount--;
                if (i < activeCount) {
                    Particle dead = pool[i];
                    pool[i] = pool[activeCount];
                    pool[activeCount] = dead;
                }
                continue;
            }

            p.vx += p.ax * dt;
            p.vy += p.ay * dt;
            if (p.drag > 0f) {
                float damp = (float) Math.exp(-p.drag * dt);
                p.vx *= damp;
                p.vy *= damp;
            }

            float wobbleVx = 0f;
            if (p.wobbleAmp > 0f) {
                float elapsed = p.maxLife - p.life;
                wobbleVx = MathUtils.cos(elapsed * p.wobbleFreq + p.wobblePhase) * p.wobbleAmp;
            }

            p.x += (p.vx + wobbleVx) * dt;
            p.y += p.vy * dt;
            p.rotation += p.rotSpeed * dt;
            i++;
        }
    }

    public void render(SpriteBatch batch, OrthographicCamera camera) {
        if (activeCount == 0 || batch == null || camera == null) return;

        Texture whitePixel = ProceduralTextures.getWhitePixel();
        float halfW = camera.viewportWidth * 0.5f + 1.5f;
        float halfH = camera.viewportHeight * 0.5f + 1.5f;
        float minX = camera.position.x - halfW;
        float maxX = camera.position.x + halfW;
        float minY = camera.position.y - halfH;
        float maxY = camera.position.y + halfH;

        Color prev = batch.getColor();

        // Pass 1: Standard alpha-blended particles (dust, stone chips, water droplets)
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        drawPass(batch, whitePixel, false, minX, maxX, minY, maxY);

        // Pass 2: Additive-blended glowing particles (spores, sparks, embers, soul orbs)
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        drawPass(batch, whitePixel, true, minX, maxX, minY, maxY);

        // Restore default blend state
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(prev);
    }

    private void drawPass(SpriteBatch batch, Texture tex, boolean additivePass,
                          float minX, float maxX, float minY, float maxY) {
        for (int i = 0; i < activeCount; i++) {
            Particle p = pool[i];
            if (p.additive != additivePass) continue;
            if (p.x < minX || p.x > maxX || p.y < minY || p.y > maxY) continue;

            float t = 1.0f - MathUtils.clamp(p.life / p.maxLife, 0f, 1f);
            float size = MathUtils.lerp(p.startSize, p.endSize, t);
            if (size <= 0.002f) continue;

            float r = MathUtils.lerp(p.startR, p.endR, t);
            float g = MathUtils.lerp(p.startG, p.endG, t);
            float b = MathUtils.lerp(p.startB, p.endB, t);
            float a = MathUtils.lerp(p.startA, p.endA, t);
            if (a <= 0.005f) continue;

            batch.setColor(r, g, b, a);

            float w = size;
            float h = size;
            float angle = p.rotation;

            if (p.shape == Shape.DIAMOND) {
                angle = p.rotation + 45f;
            } else if (p.shape == Shape.STREAK) {
                float speed = (float) Math.hypot(p.vx, p.vy);
                w = Math.max(size, Math.min(size * p.streakLengthMult, size + speed * 0.025f));
                h = size * 0.55f;
                angle = MathUtils.atan2(p.vy, p.vx) * MathUtils.radiansToDegrees;
            }

            float originX = w * 0.5f;
            float originY = h * 0.5f;
            batch.draw(
                    tex,
                    p.x - originX,
                    p.y - originY,
                    originX,
                    originY,
                    w,
                    h,
                    1f, 1f,
                    angle,
                    0, 0,
                    1, 1,
                    false, false
            );
        }
    }
}
