package com.Ruinfall.frontend.objects.bullets;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.Ruinfall.frontend.objects.Collidable;
import com.Ruinfall.frontend.objects.GameObject;
import com.Ruinfall.frontend.objects.enemies.Enemy;
import com.Ruinfall.frontend.objects.patterns.bulletStrategy.BulletMovementPattern;

public class Bullet extends GameObject {
    private BulletType bulletType;
    private int damage;
    private float vx = 20, vy = 20;
    private float angle = 90f;
    private boolean isPlayerBullet;
    private boolean grazed;
    private BulletMovementPattern movementPattern;
    // TODO: Deklarasikan atribut-atribut private berikut:
    // 1. float vx dan vy (komponen kecepatan vektor 2D)
    // 2. float angle dengan nilai awal 90f (sudut rotasi visual peluru dalam derajat)
    // 3. boolean isPlayerBullet (penanda apakah peluru ini milik player atau musuh)
    // 4. boolean grazed (penanda apakah peluru ini sudah pernah di-graze oleh player)

    // Pool Re-initialization untuk Object Pool Pattern
    public void init(float x, float y, float vx, float vy, BulletType bulletType, int damage, boolean isPlayerBullet) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.movementPattern = null;

        this.angle = (float)Math.toDegrees(Math.atan2(vy, vx)) + 90;
        this.speed = (float)Math.hypot(vx, vy);

        this.bulletType = bulletType;
        this.damage = damage;
        this.isPlayerBullet = isPlayerBullet;

        this.active = true;
        this.stateTime = 0;

        this.grazed = false;

        // TODO:
        // 1. Perbarui nilai posisi (x, y) dan komponen kecepatan vektor (vx, vy) dari parameter yang diterima
        // 2. Hitung sudut rotasi `angle` menggunakan rumus Math.atan2 sebagaimana dijelaskan di atas, lalu ganti ke degree.
        // 3. Perbarui nilai `speed` dengan menghitung panjang resultan kedua vektor kecepatan menggunakan rumus: Math.hypot sebagaimana dijelaskan di paragraf atas.
        // 4. Simpan atribut identitas peluru (bulletType, damage, dan isPlayerBullet) sesuai nilai parameter
        // 5. Aktifkan kembali peluru ini (active) dan mulai ulang waktu state animasinya (stateTime) dari 0
        // 6. Pastikan status graze (grazed) direset menjadi tidak aktif agar peluru daur ulang ini dapat di-graze kembali
    }

    // Konstruktor dengan kecepatan default (speed = 400f)
    public Bullet(float x, float y, BulletType bulletType, int damage) {
        super(x, y, 16, 16, 400f, Color.YELLOW);
        // TODO:
        this.bulletType = bulletType;
        this.damage = damage;
        vx = 0;
        vy = 400f;
        angle = 90f;
        isPlayerBullet = true;
        grazed = false;
        // 1. Inisialisasi atribut bulletType dan damage dari parameter
        // 2. Beri kecepatan awal vx sebagai nol dan vy sebagai 400f (bergerak lurus ke atas)
        // 3. Set sudut rotasi visual angle agar 90 derajat ke atas
        // 4. Tandai peluru ini sebagai dirinya adalah milik player (isPlayerBullet), lalu tandai status graze dalam keadaan tidak aktif
    }

    // Konstruktor overload dengan kecepatan custom
    public Bullet(float x, float y, float speed, BulletType bulletType, int damage) {
        super(x, y, 16, 16, speed, Color.YELLOW);
        this.bulletType = bulletType;
        this.damage = damage;
        vx = 0;
        vy = speed;
        if (speed < 0)
            angle = 270f;
        else
            angle = 90f;
        isPlayerBullet = true;
        grazed = false;
        // TODO:
        // 1. Inisialisasi atribut bulletType dan damage dari parameter
        // 2. Beri kecepatan awal vx sebagai nol dan vy sesuai dengan speed
        // 3. Set sudut rotasi visual angle: jika speed bernilai negatif gunakan 270f (menghadap bawah), jika tidak gunakan 90f
        // 4. Tandai peluru ini sebagai dirinya adalah milik player (isPlayerBullet), lalu tandai status graze dalam keadaan tidak aktif
    }


    @Override
    public void update(float delta) {
        super.update(delta);

        if (movementPattern != null) {
            movementPattern.move(this, delta);
        } else {
            setX(getX() + vx * delta);
            setY(getY() + vy * delta);
        }
    }

    public int getDamage() {
        return damage;
    }

    public boolean getIsPlayerBullet() {
        return isPlayerBullet;
    }

    public float getVx() {
        return vx;
    }

    public float getVy() {
        return vy;
    }

    public float getAngle() {
        return angle;
    }

    public void setAngle(float angle) {
        this.angle =  angle;
    }

    public void setGrazed(boolean grazed) {
        this.grazed =  grazed;
    }

    public boolean hasBeenGrazed() {
        return grazed;
    }

    public BulletMovementPattern getMovementPattern() { return movementPattern; }
    public void setMovementPattern(BulletMovementPattern movementPattern) { this.movementPattern = movementPattern; }

    // TODO:
    // 1. Buat getter untuk isPlayerBullet, vx, dan vy
    // 2. Buat getter dan setter untuk angle
    // 3. Buat getter hasBeenGrazed() yang mengembalikan nilai atribut grazed
    // 4. Buat setter setGrazed(boolean grazed)

@Override
    public void render(SpriteBatch batch) {
        if (batch != null && active) {
            if (sprite != null) {
                // TODO: Gambar sprite peluru menggunakan batch.draw() dengan parameter:
                // sprite, posisi (x, y), titik origin putar di tengah (width / 2f, height / 2f),
                // ukuran (width, height), scale (1f, 1f), dan sudut rotasi (angle - 90f)

                batch.draw(sprite, x, y, width / 2f, height / 2f, width, height, 1f, 1f, angle - 90f);
            } else {
                super.render(batch);
            }
        }
    }

    @Override
    public void onCollision(Collidable other) {
        if (other instanceof Enemy enemy) {
            // 1. Tampilkan pesan bahwa Bullet mengenai Enemy dalam format:
            //    Bullet hit [EnemyName] for [damage] DMG!
            System.out.println("Bullet hit " + enemy.getName() + " for " + damage + " DMG!");

            // 2. Panggil takeDamage() milik Enemy dengan damage milik Bullet ini.
            enemy.takeDamage(damage);

            // 3. Bikin si bullet hancur (destroy) setelah mengenai Enemy, apapun hasilnya
            //    (baik enemy kalah atau masih hidup), krn satu bullet cuma boleh kena satu target.
            destroy();
        }
    }
}
