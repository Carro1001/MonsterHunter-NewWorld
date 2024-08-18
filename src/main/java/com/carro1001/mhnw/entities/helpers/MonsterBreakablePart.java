package com.carro1001.mhnw.entities.helpers;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.entities.NewWorldMonsterEntity;
import com.carro1001.mhnw.entities.interfaces.IMonsterBreakablePart;
import de.dertoaster.multihitboxlib.entity.MHLibPartEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class MonsterBreakablePart implements IMonsterBreakablePart {

    float hp;
    boolean isGone;
    MHLibPartEntity<NewWorldMonsterEntity> partEntity;
    PART partType;
    public MonsterBreakablePart(MHLibPartEntity<NewWorldMonsterEntity> part,float hp, boolean isGoneOnDeath, PART partType){
        setPart(part);
        setHP(hp);
        isGone = isGoneOnDeath;
        this.partType = partType;
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
    public boolean isDead() {
        return getHP()<=0;
    }

    @Override
    public void setGoneWhenDead(boolean goneWhenDead) {
        isGone = goneWhenDead;
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
                Vec3 pos = partEntity.getPosition(0);
                partEntity.level().setBlock(new BlockPos((int) pos.x, (int) pos.y, (int) pos.z), Blocks.SAND.defaultBlockState(),3);
            }
        }
    }

    @Override
    public String getPartName() {
        return partEntity.getConfigName();
    }

    @Override
    public PART getPartType() {
        return partType;
    }

    @Override
    public String toString(){
        return getPartType().toString() + " type part: " + partEntity.getConfigName() + " with health: " + getHP() +", status: " + (isGoneWhenDead()?"Can be cut":"Can be broken");
    }
}
