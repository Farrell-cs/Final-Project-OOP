package com.Ruinfall.frontend.systems;

import java.util.HashMap;
import java.util.Map;

/**
 * Singleton pattern for managing audio and game assets.
 */
public class AssetManager {
    private static AssetManager instance;
    private final Map<String, Object> soundMap = new HashMap<>();

    private AssetManager() {}

    public static AssetManager getInstance() {
        if (instance == null) {
            instance = new AssetManager();
        }
        return instance;
    }

    public void playSound(String soundKey) {
        // Log sound playback or trigger LibGDX Sound when loaded
        System.out.println("[AssetManager] Playing sound: " + soundKey);
    }
}
