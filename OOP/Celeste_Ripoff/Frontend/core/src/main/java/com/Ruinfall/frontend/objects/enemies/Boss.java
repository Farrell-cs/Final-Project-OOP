package com.Ruinfall.frontend.objects.enemies;

import com.badlogic.gdx.graphics.Color;
import com.gylchrist.frontend.objects.Collidable;
import com.gylchrist.frontend.objects.Player;
import com.gylchrist.frontend.objects.patterns.entityStrategy.FixedMovement;
import com.gylchrist.frontend.objects.patterns.shootingStrategy.SpreadShot;

public class Boss extends Enemy {
    // TODO: Deklarasikan field private `shootingPattern` bertipe ShootingPattern
    //       dan langsung inisialisasi (new) dengan `SpreadShot` yang baru saja kita buat.
    //       Pastikan speed-nya bernilai 200f, angle spread-nya 30f, dan ada 3 peluru (num) yang ditembak.

    public Boss(String name, int hp) {
        super(380, 400, 64, 64, Color.BLUE, name, hp, 5000L);
        // TODO (2 poin): Suntikkan strategi bawaan Boss via setter
        // 1. Tetapkan Shooting Pattern sebagai Spread Shot dengan speed 200f, 3 peluru, dan 30f spreadAngleDegerees).
        // 2. Tetapkan Movement Pattern dengan Fixed Movement.
        setShootingPattern(new SpreadShot(200f, 3, 30f));
        setMovementPattern(new FixedMovement());
        this.name = name;
        this.maxHP = hp;
    }

    public Boss(float x, float y, String name, int hp) {
        super(x, y, 64, 64, Color.BLUE, name, hp, 5000L);
        // TODO (2 poin): Lakukan penyuntikan strategi yang sama pada konstruktor koordinat kustom
        setShootingPattern(new SpreadShot(200f, 3, 30f));
        setMovementPattern(new FixedMovement());

        this.x = x;
        this.y = y;
        this.name = name;
        this.maxHP = hp;
    }

    @Override
    public void onCollision(Collidable other) {
        if (other instanceof Player) {
            System.out.println("Player touches boss");
        }
    }

}
//Dari ini terbentuk hierarchical inheretance sebab ada 2 subclass yang menginherit sifat dari 1 class
