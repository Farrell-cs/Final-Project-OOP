package com.Ruinfall.frontend.objects.patterns.entityStrategy;

import com.Ruinfall.frontend.objects.GameObject;

public abstract interface EntityMovementPattern {
    void move(GameObject entity, float delta);
}
