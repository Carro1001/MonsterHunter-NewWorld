package com.carro1001.mhnw.entities.interfaces;

import com.carro1001.mhnw.entities.NewWorldMonsterEntity;
import de.dertoaster.multihitboxlib.entity.MHLibPartEntity;

public interface IMonsterBreakablePart {

    float getHP();
    void setHP(float value);
    void addHP(float value);

    boolean isGoneWhenDead();

    MHLibPartEntity<NewWorldMonsterEntity> getPart();
    void setPart(MHLibPartEntity<NewWorldMonsterEntity> entity);

    void hurt(float amount);
    String getPartName();

}
