package com.Ruinfall.frontend.objects.patterns;

import com.Ruinfall.frontend.objects.Interactables.Stalactite;

public interface FallBehavior {
    void onFall(Stalactite stalactite, float delta);
}
