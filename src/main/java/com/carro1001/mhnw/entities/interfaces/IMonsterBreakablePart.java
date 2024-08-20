package com.carro1001.mhnw.entities.interfaces;

public interface IMonsterBreakablePart {

    float getHP();
    void setHP(float value);
    void addHP(float value);
    boolean isDead();

    void setGoneWhenDead(boolean  goneWhenDead);
    boolean isGoneWhenDead();

    String getPartName();

    PART getPartType();

    enum  PART{
        HEAD,
        ARM,
        BODY,
        WING,
        TAIL,
        OTHER
    }

}
