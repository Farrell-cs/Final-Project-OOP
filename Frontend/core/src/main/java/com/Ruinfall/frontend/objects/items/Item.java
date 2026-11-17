package com.Ruinfall.frontend.objects.items;

import com.badlogic.gdx.graphics.Color;
import com.Ruinfall.frontend.objects.Collidable;
import com.Ruinfall.frontend.objects.GameObject;
import com.Ruinfall.frontend.PlayerController;


public class Item extends GameObject {
    private String itemType;
    private long scoreValue;
    private ItemType itemTypeEnum;

    public Item(float x, float y, String itemType){
        super(x, y, 16, 16, 100f, Color.WHITE);
        this.itemType = itemType;
    }

    public Item(float x, float y, float width, float height, float speed, String itemType){
        super(x, y, width, height, 100f, Color.WHITE);
        this.itemType = itemType;
    }

    public Item(float x, float y, float width, float height, float speed, String itemType, long scoreValue){
        super(x, y, width, height, 100f, Color.WHITE);
        this.itemType = itemType;
        this.scoreValue = scoreValue;
    }

    public Item(float x, float y, ItemType itemTypeEnum){
        super(x, y, 16, 16, 100f, Color.WHITE);
        this.scoreValue = itemTypeEnum.getScoreValue();
        this.itemType = itemTypeEnum.name();

    }

    public Item(float x, float y, float width, float height, float speed, ItemType itemTypeEnum, long scoreValue){
        super(x, y, width, height, speed, Color.WHITE);
        this.scoreValue = itemTypeEnum.getScoreValue();
        this.itemType = itemTypeEnum.name();
    }


    @Override
    public void update(float delta) {
        this.y -= speed * delta;
    }//Item dalam game tidak mempunyai pergerakan yang aktif dan hanya secara pasif bergerak linear

    public String getItemType(){
        return itemType;
    }

    public ItemType getItemTypeEnum() { return itemTypeEnum; }

    public long getScoreValue(){
        return scoreValue;
    }

    @Override
    public void onCollision(Collidable other) {
        if(other instanceof PlayerController) {

        }
    }

    public void collectItem(Item item) {
        if (item.isDestroyed()) return; // Mencegah item diambil dua kali di frame yang sama
        destroy();
    }
}
