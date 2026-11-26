package com.Ruinfall.frontend.objects.enemies;

import com.badlogic.gdx.graphics.Color;
import com.Ruinfall.frontend.objects.EntityShooter;
import com.Ruinfall.frontend.objects.Player;
import com.Ruinfall.frontend.objects.patterns.ShootingPattern;
import com.Ruinfall.frontend.objects.patterns.entityStrategy.EntityMovementPattern;
import com.Ruinfall.frontend.systems.AssetManager;

public class Enemy extends EntityShooter {
    protected String name;
    protected int hp;
    protected int maxHP;
    protected  long ScoreValue;

    public Enemy(String name, int hp){
        super(200, 380, 24, 24, 0, Color.PINK);
        this.name = name;
        this.hp = hp;
        this.maxHP = hp;
        this.ScoreValue = 100;
    }

    public Enemy(float x, float y, float width, float height, Color color, String name, int hp, long scoreValue) {
        super(x, y, width, height, 0, color);
        this.name = name;
        this.hp = hp;
        this.maxHP = hp;
        this.ScoreValue = 100;
    }

    public Enemy(float x, float y, float width, float height, Color color, String name, int hp, long scoreValue, ShootingPattern shootingPattern, EntityMovementPattern movementPattern) {
        super(x, y, width, height, 0, color, shootingPattern, movementPattern);
        this.name = name;
        this.hp = hp;
        this.maxHP = hp;
        this.ScoreValue = scoreValue;
    }


    public boolean takeDamage(int damage) {
        boolean wasAlive = isAlive();
        this.hp -= damage;
        if (this.hp < 0) {
            this.hp = 0;
        }
        System.out.println(name + " took " + damage + " damage! HP: " + this.hp + "/" + this.maxHP);
        if (wasAlive && this.hp == 0) {
            System.out.println(name + " was defeated!");
            AssetManager.getInstance().playSound("sfx_dead");
            destroy();
            return true;
        }
        return false;
    }

    public void attack(Player player, int damage) {
        // 1. Tampilkan informasi bahwa Enemy menyerang Player dalam format: [EnemyName] unleashes bullet barrage on [PlayerName]!
        System.out.println(name + " unleashes bullet barrage on " + player.getName() + "!");

        // 2. Panggil takeDamage() milik Player menggunakan damage yang diberikan.
        player.takeDamage(damage);
    }

    public boolean isAlive() {
        // 1. Kembalikan true jika hp > 0 dan false jika sebaliknya
        if(hp > 0)
            return true;
        else
            return false;
    }

    public void setHp(int hp) {
        this.hp = Math.max(0, hp);
    }

    public void setScoreValue(int scoreValue) {
        this.ScoreValue = scoreValue;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getHp() {
        return hp;
    }

    public String getName() {
        return name;
    }

    public int getMaxHP() {
        return maxHP;
    }

    public long getScoreValue() {
        return ScoreValue;
    }

}
