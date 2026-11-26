package com.Ruinfall.frontend.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import java.util.HashMap;
import java.util.Map;

/**
 * Singleton pattern for managing audio, textures, and sprite-sheet animations.
 */
public class AssetManager {
    private static AssetManager instance;
    private final Map<String, Object> soundMap = new HashMap<>();
    private final Map<String, Animation<TextureRegion>> playerAnimations = new HashMap<>();
    private Texture playerSheet;
    private boolean spritesLoaded = false;

    private AssetManager() {}

    public static AssetManager getInstance() {
        if (instance == null) {
            instance = new AssetManager();
        }
        return instance;
    }

    /**
     * Loads the 656x16 (41 frames of 16x16) player pixel-art sprite sheet from
     * Frontend/assets/sprites/player_idle.png with crisp Nearest filtering.
     */
    public void loadPlayerSprites() {
        if (spritesLoaded) return;
        spritesLoaded = true;

        FileHandle file = Gdx.files.internal("sprites/player_idle.png");
        if (!file.exists()) {
            System.out.println("[AssetManager] sprites/player_idle.png not found; using procedural fallback.");
            return;
        }

        playerSheet = new Texture(file);
        playerSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        int frameSize = playerSheet.getHeight(); // 16px
        int totalFrames = playerSheet.getWidth() / frameSize; // 41 frames
        TextureRegion[][] split = TextureRegion.split(playerSheet, frameSize, frameSize);
        if (split.length == 0 || split[0].length == 0) return;

        TextureRegion[] frames = split[0];
        playerAnimations.put("climb", buildAnim(frames, 0, Math.min(5, totalFrames - 1), 0.10f, Animation.PlayMode.LOOP));
        playerAnimations.put("idle",  buildAnim(frames, 6, Math.min(11, totalFrames - 1), 0.14f, Animation.PlayMode.LOOP));
        playerAnimations.put("swing", buildAnim(frames, 16, Math.min(20, totalFrames - 1), 0.045f, Animation.PlayMode.NORMAL));
        playerAnimations.put("run",   buildAnim(frames, 21, Math.min(28, totalFrames - 1), 0.075f, Animation.PlayMode.LOOP));
        playerAnimations.put("jump",  buildAnim(frames, 29, Math.min(32, totalFrames - 1), 0.09f, Animation.PlayMode.NORMAL));
        playerAnimations.put("fall",  buildAnim(frames, 33, Math.min(36, totalFrames - 1), 0.09f, Animation.PlayMode.LOOP));
        playerAnimations.put("dash",  buildAnim(frames, 37, Math.min(40, totalFrames - 1), 0.05f, Animation.PlayMode.LOOP));
    }

    private Animation<TextureRegion> buildAnim(TextureRegion[] all, int start, int end, float frameDuration, Animation.PlayMode mode) {
        Array<TextureRegion> slice = new Array<>();
        int safeStart = Math.max(0, Math.min(start, all.length - 1));
        int safeEnd = Math.max(safeStart, Math.min(end, all.length - 1));
        for (int i = safeStart; i <= safeEnd; i++) {
            slice.add(all[i]);
        }
        Animation<TextureRegion> anim = new Animation<>(frameDuration, slice);
        anim.setPlayMode(mode);
        return anim;
    }

    public Animation<TextureRegion> getPlayerAnimation(String key) {
        if (!spritesLoaded) {
            loadPlayerSprites();
        }
        return playerAnimations.get(key);
    }

    public boolean hasPlayerSprites() {
        if (!spritesLoaded) {
            loadPlayerSprites();
        }
        return !playerAnimations.isEmpty();
    }

    public void playSound(String soundKey) {
        System.out.println("[AssetManager] Playing sound: " + soundKey);
    }

    public void dispose() {
        if (playerSheet != null) {
            playerSheet.dispose();
            playerSheet = null;
        }
        playerAnimations.clear();
        spritesLoaded = false;
    }
}
