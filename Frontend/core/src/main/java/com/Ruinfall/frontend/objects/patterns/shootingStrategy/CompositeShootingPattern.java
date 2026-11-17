// CompositeShootingPattern.java
package com.Ruinfall.frontend.objects.patterns.shootingStrategy;

import com.gylchrist.frontend.objects.patterns.ShootingPattern;
import com.gylchrist.frontend.systems.BulletManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CompositeShootingPattern implements ShootingPattern {
    private List<ShootingPattern> patterns = new ArrayList<>();

    public CompositeShootingPattern(ShootingPattern... patterns) {
        if (patterns != null) this.patterns.addAll(Arrays.asList(patterns));
    }

    public void addPattern(ShootingPattern pattern) {
        if (pattern != null) this.patterns.add(pattern);
    }

    public void removePattern(ShootingPattern pattern) {
        this.patterns.remove(pattern);
    }

    @Override
    public void execute(float originX, float originY, BulletManager bulletManager, boolean isPlayer) {
        for (ShootingPattern pattern : patterns) {
            if (pattern != null) pattern.execute(originX, originY, bulletManager, isPlayer);
        }
    }

    public List<ShootingPattern> getPatterns() { return patterns; }
}
