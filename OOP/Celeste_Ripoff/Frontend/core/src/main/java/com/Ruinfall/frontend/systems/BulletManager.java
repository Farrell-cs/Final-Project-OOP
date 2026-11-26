package com.Ruinfall.frontend.systems;

import com.Ruinfall.frontend.objects.bullets.Bullet;
import com.Ruinfall.frontend.objects.bullets.BulletType;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Object Pool + Manager for bullets.
 */
public class BulletManager {
    private final List<Bullet> activeBullets = new ArrayList<>();
    private final Queue<Bullet> bulletPool = new LinkedList<>();

    public Bullet spawnPlayerBullet(float x, float y, float vx, float vy, int damage) {
        return spawnPlayerBullet(x, y, vx, vy, damage, null);
    }

    public Bullet spawnPlayerBullet(float x, float y, float vx, float vy, int damage, String spriteKey) {
        Bullet bullet = obtainBullet();
        bullet.init(x, y, vx, vy, BulletType.AMULET, damage, true);
        activeBullets.add(bullet);
        return bullet;
    }

    public Bullet spawnEnemyBullet(float x, float y, float vx, float vy, int damage) {
        return spawnEnemyBullet(x, y, vx, vy, damage, null);
    }

    public Bullet spawnEnemyBullet(float x, float y, float vx, float vy, int damage, String spriteKey) {
        Bullet bullet = obtainBullet();
        bullet.init(x, y, vx, vy, BulletType.DANMAKU, damage, false);
        activeBullets.add(bullet);
        return bullet;
    }

    private Bullet obtainBullet() {
        if (!bulletPool.isEmpty()) {
            return bulletPool.poll();
        }
        return new Bullet(0, 0, BulletType.DANMAKU, 1);
    }

    public void recycleBullet(Bullet bullet) {
        bullet.destroy();
        bulletPool.offer(bullet);
    }

    public void update(float delta) {
        for (int i = activeBullets.size() - 1; i >= 0; i--) {
            Bullet b = activeBullets.get(i);
            b.update(delta);
            if (b.isDestroyed()) {
                activeBullets.remove(i);
                b.notifyDestroyed(this);
                recycleBullet(b);
            }
        }
    }

    /**
     * Immediately removes all active enemy bullets without triggering timeout/death bursts.
     */
    public void clearEnemyBullets() {
        for (int i = activeBullets.size() - 1; i >= 0; i--) {
            Bullet b = activeBullets.get(i);
            if (!b.getIsPlayerBullet()) {
                activeBullets.remove(i);
                recycleBullet(b);
            }
        }
    }

    /**
     * Immediately removes all active bullets without triggering timeout/death bursts.
     */
    public void clearAll() {
        for (int i = activeBullets.size() - 1; i >= 0; i--) {
            Bullet b = activeBullets.remove(i);
            recycleBullet(b);
        }
    }

    public List<Bullet> getActiveBullets() {
        return activeBullets;
    }
}
