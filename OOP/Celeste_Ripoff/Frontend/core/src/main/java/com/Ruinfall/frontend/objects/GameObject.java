package com.Ruinfall.frontend.objects;
import com.badlogic.gdx.graphics.Color;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

public abstract class GameObject implements Collidable {
    protected float x, y, width, height, speed;
    protected Color color;
    protected boolean active = true;
    protected TextureRegion sprite;
    protected Animation<TextureRegion> animation;
    protected float stateTime = 0f;

    public GameObject() {
        this(0, 0, 16, 16, 0, Color.WHITE);
    }

    public GameObject(float x, float y, float width, float height, float speed,Color color)
    {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.speed = speed;
        this.color = color;
    }

    public void update(float delta)
    {
        stateTime += delta;
    }

    @Override
    public Rectangle getCoreHitbox() {
        // TODO: kembalikan Rectangle baru sesuai x, y, width, height object ini
        Rectangle rectangle = new Rectangle(x, y, width, height);
        return rectangle;
    }

    @Override
    public Rectangle getGrazeHitbox() {
        // TODO: kembalikan Rectangle dengan padding +10px di setiap sisi
        Rectangle rectangle = new Rectangle(x - 10, y - 10, width + 20, height + 20);
        return rectangle;
    }

    @Override
    public void onCollision(Collidable other) {
        // Base collision handler (boleh di-override oleh subclass yang butuh bereaksi)
    }

    public void render(ShapeRenderer shapeRenderer) {
        if (shapeRenderer != null && color != null && active) {
            shapeRenderer.setColor(color);
            shapeRenderer.rect(x, y, width, height);
        }
    }

    public void render(SpriteBatch batch) {
        if (batch != null && active) {
            if (animation != null) {
                TextureRegion currentFrame = animation.getKeyFrame(stateTime, true);
                batch.draw(currentFrame, x, y, width, height);
            } else if (sprite != null) {
                batch.draw(sprite, x, y, width, height);
            }
        }
    }


    public boolean isDestroyed() {
        if(!active)
            return true;
        return false;
    }

    public void destroy() {
        active = false;
    }

    public boolean isOffScreen(float screenWidth, float screenHeight) {
        if(x > screenWidth + 50 || y > screenHeight + 50 || x < -50 || y < -50) {
            return true;
        }
        // Gunakan margin toleransi 50px di setiap sisi, supaya objek yang baru
        // sedikit melewati tepi layar tidak langsung dianggap hilang.
        return false;
    }

    public void setX(float X) {
        this.x = X;
    }

    public void setY(float Y) {
        this.y = Y;
    }

    public void setWidth(float width) {
        if (width > 0) this.width = width;
    }

    public void setHeight(float height) {
        if (height > 0) this.height = height;
    }

    public void setSpeed(float speed) {
        if (speed >= 0) this.speed = speed;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public void setSprite(TextureRegion sprite) {this.sprite = sprite;}

    public void setAnimation(Animation animation) {
        this.animation = animation;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public float getSpeed() {
        return speed;
    }

    public Color getColor() {
        return color;
    }

    public TextureRegion getSprite() {return sprite;}

    public Animation getAnimation() {return animation;}

    public float getStateTime() {return stateTime;}

    public void setStateTime(float stateTime) { this.stateTime = stateTime; }

    public void setActive(boolean active) { this.active = active; }

    public boolean isActive() {return active;}
}
