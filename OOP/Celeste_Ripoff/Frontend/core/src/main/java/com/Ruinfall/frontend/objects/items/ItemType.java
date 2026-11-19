package com.Ruinfall.frontend.objects.items;

/**
 * ItemType: Enumerates collectible item categories along with their respective bonuses.
 */
public enum ItemType {
    BOMB(500L, 0, 1, 0),         // +500 score, +1 bomb
    LIFE(2000L, 0, 0, 20),       // +2000 score, +20 HP bonus
    SHIELD(300L, 0, 1, 0),       // +300 score, shield protection
    MANTIS_CLAW(1500L, 1, 0, 0), // +1500 score, mantis claw wall-climb/reach upgrade
    POWER(100L, 1, 0, 0),        // legacy compatibility
    POINT(1000L, 0, 0, 0);       // legacy compatibility

    private final long scoreValue;
    private final int powerBonus;
    private final int specialBonus;
    private final int hpBonus;

    ItemType(long scoreValue, int powerBonus, int specialBonus, int hpBonus) {
        this.scoreValue = scoreValue;
        this.powerBonus = powerBonus;
        this.specialBonus = specialBonus;
        this.hpBonus = hpBonus;
    }

    ItemType(long scoreValue, int powerBonus) {
        this(scoreValue, powerBonus, 0, 0);
    }

    public long getScoreValue() { return scoreValue; }
    public int getPowerBonus() { return powerBonus; }
    public int getSpecialBonus() { return specialBonus; }
    public int getHpBonus() { return hpBonus; }
}
