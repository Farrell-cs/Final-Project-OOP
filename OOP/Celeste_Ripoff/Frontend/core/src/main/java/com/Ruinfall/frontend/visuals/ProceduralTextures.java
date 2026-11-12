package com.Ruinfall.frontend.visuals;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

public class ProceduralTextures {
    private static Texture blockTexture;
    private static Texture whitePixelTexture;

    public static Texture getBlockTexture() {
        if (blockTexture != null) {
            return blockTexture;
        }

        Pixmap pixmap = new Pixmap(16, 16, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();

        pixmap.setColor(new Color(0.85f, 0.85f, 0.88f, 1.0f));
        pixmap.drawRectangle(0, 0, 16, 16);

        blockTexture = new Texture(pixmap);
        blockTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();

        return blockTexture;
    }

    public static Texture getWhitePixel() {
        if (whitePixelTexture != null) {
            return whitePixelTexture;
        }

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();

        whitePixelTexture = new Texture(pixmap);
        pixmap.dispose();

        return whitePixelTexture;
    }

    public static void dispose() {
        if (blockTexture != null) {
            blockTexture.dispose();
            blockTexture = null;
        }
        if (whitePixelTexture != null) {
            whitePixelTexture.dispose();
            whitePixelTexture = null;
        }
    }
}
