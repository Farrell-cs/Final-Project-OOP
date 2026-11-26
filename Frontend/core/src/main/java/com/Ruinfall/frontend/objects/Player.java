package com.Ruinfall.frontend.objects;

public interface Player extends Collidable {
    String getName();
    void takeDamage(int damage);
}
