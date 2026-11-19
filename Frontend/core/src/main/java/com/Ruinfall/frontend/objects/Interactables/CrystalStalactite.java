package com.Ruinfall.frontend.objects.Interactables;

import com.badlogic.gdx.graphics.Color;
import com.Ruinfall.frontend.PlayerController;
import com.Ruinfall.frontend.objects.Collidable;
import com.Ruinfall.frontend.objects.GameObject;
import com.Ruinfall.frontend.objects.bullets.Bullet;
import com.Ruinfall.frontend.objects.items.Item;
import com.Ruinfall.frontend.objects.items.ItemType;
import com.Ruinfall.frontend.systems.GameEventBus;
import com.Ruinfall.frontend.systems.ItemFactory;

/**
 * CrystalStalactite: Variant of Stalactite that drops when disturbed
 * and reflects or refracts laser/danmaku projectiles.
 */
public class CrystalStalactite extends Stalactite {

    public CrystalStalactite() {
        this(0, 0);
    }

    public CrystalStalactite(float x, float y) {
        super(x, y, 16f, 32f, Color.CYAN);
        this.shakeDuration = 0.2f; // Detaches faster when disturbed
        this.fallGravity = 350f;
    }

    @Override
    public void onEntityHit(GameObject entity) {
        if (!active) return;

        System.out.println("[CrystalStalactite] Hit entity: " + entity.getClass().getSimpleName());

        if (entity instanceof PlayerController player) {
            player.takeDamage(1);
            player.getVelocity().set(player.getVelocity().x * 0.5f, 0);
        }

        Item drop = ItemFactory.createItem(x, y, ItemType.SHIELD);
        GameEventBus.getInstance().publish(GameEventBus.GameEvent.CRYSTAL_SHATTERED, drop);
        destroy();
    }

    @Override
    public void onGroundHit() {
        if (!active) return;
        System.out.println("[CrystalStalactite] Shattered on ground impact!");

        ItemType dropType = Math.random() < 0.5 ? ItemType.SHIELD : ItemType.MANTIS_CLAW;
        Item drop = ItemFactory.createItem(x, y, dropType);
        GameEventBus.getInstance().publish(GameEventBus.GameEvent.CRYSTAL_SHATTERED, drop);
        destroy();
    }

    @Override
    public void onCollision(Collidable other) {
        if (!active) return;

        if (other instanceof Bullet bullet) {
            reflectBullet(bullet);
            return;
        }

        super.onCollision(other);
    }

    public void reflectBullet(Bullet bullet) {
        System.out.println("[CrystalStalactite] Reflected/Refracted projectile at (" + bullet.getX() + ", " + bullet.getY() + ")");
        // Reverse direction or refract the bullet
        bullet.setAngle(bullet.getAngle() + 180f);
        bullet.destroy(); // bullet consumed/reflected
    }
}
