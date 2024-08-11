package com.carro1001.mhnw.entities.helpers;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.entities.NewWorldMonsterEntity;
import com.carro1001.mhnw.entities.interfaces.IMonsterBreakablePart;
import de.dertoaster.multihitboxlib.entity.MHLibPartEntity;
import net.minecraft.world.level.block.Blocks;

public class MonsterBreakablePart implements IMonsterBreakablePart {

    float hp;
    boolean isGone;
    MHLibPartEntity<NewWorldMonsterEntity> partEntity;
    public MonsterBreakablePart(MHLibPartEntity<NewWorldMonsterEntity> part,float hp, boolean isGoneOnDeath){
        setPart(part);
        setHP(hp);
        isGone = isGoneOnDeath;
        MHNW.LOGGER.debug("Breakable part added: {}", this);
    }

    @Override
    public float getHP() {
        return hp;
    }

    @Override
    public void setHP(float value) {
        hp = value;
    }

    @Override
    public void addHP(float value) {
        hp = Math.max(value + getHP(),0);
    }

    @Override
    public boolean isGoneWhenDead() {
        return isGone;
    }

    @Override
    public MHLibPartEntity<NewWorldMonsterEntity> getPart() {
        return partEntity;
    }

    @Override
    public void setPart(MHLibPartEntity<NewWorldMonsterEntity> entity) {
        partEntity = entity;
    }

    @Override
    public void hurt(float amount) {
        addHP(-amount);
        if(getHP()<= 0){
            partEntity.setInvulnerable(true);
            if(isGoneWhenDead()){
                partEntity.discard();
                partEntity.level().setBlock(partEntity.getOnPos(), Blocks.SAND.defaultBlockState(),3);
            }
        }
    }

    @Override
    public String getPartName() {
        return partEntity.getConfigName();
    }

    @Override
    public String toString(){
        return "Part: " + partEntity.getConfigName() + " with health: " + getHP();
    }
}
