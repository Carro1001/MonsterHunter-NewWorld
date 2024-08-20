package com.carro1001.mhnw.entities.helpers;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.entities.NewWorldMonsterEntity;
import com.carro1001.mhnw.entities.interfaces.IMonsterBreakablePart;
import de.dertoaster.multihitboxlib.entity.MHLibPartEntity;
import de.dertoaster.multihitboxlib.entity.hitbox.SubPartConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class MonsterBreakablePartEntity<T extends Entity> extends MHLibPartEntity<T> implements IMonsterBreakablePart {

    float hp;
    boolean isGone;
    PART partType;
    public MonsterBreakablePartEntity(T parent, SubPartConfig properties, EntityDimensions baseSize, Vec3 basePosition, Vec3 pivot, float hp, boolean isGoneOnDeath, PART partType){
        super(parent, properties, baseSize, basePosition, pivot);
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
    public boolean hurt(DamageSource pSource, float pAmount) {
        if(!isGoneWhenDead() && getHP()<= 0){
            pAmount = pAmount/getConfig().damageModifier();
        }
        boolean hurted = super.hurt(pSource, pAmount);
        if(hurted){
            addHP(-pAmount);
            if(getHP()<= 0){
                setInvulnerable(true);
                if(isGoneWhenDead()){
                    if(getPartType() == PART.TAIL && getParent() instanceof NewWorldMonsterEntity newWorldMonsterEntity){
                        newWorldMonsterEntity.setTailCut(true);
                    }
                    Vec3 pos = getPosition(0);
                    level().setBlock(new BlockPos((int) pos.x, (int) pos.y, (int) pos.z), Blocks.SAND.defaultBlockState(),3);
                    discard();
                }
            }
        }
        return hurted;
    }

    @Override
    public String getPartName() {
        return getConfigName();
    }

    @Override
    public PART getPartType() {
        return partType;
    }

    @Override
    public String toString(){
        return getPartType().toString() + " type part: " + getConfigName() + " with health: " + getHP() +", status: " + (isGoneWhenDead()?"Can be cut":"Can be broken");
    }
}
