package com.carro1001.mhnw.entities.helpers;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.entities.NewWorldGrowingEntity;
import com.carro1001.mhnw.entities.NewWorldMonsterEntity;
import com.carro1001.mhnw.entities.interfaces.IMonsterBreakablePart;
import de.dertoaster.multihitboxlib.entity.MHLibPartEntity;
import de.dertoaster.multihitboxlib.entity.hitbox.SubPartConfig;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

public class MonsterBreakablePartEntity<T extends Entity> extends MHLibPartEntity<T> implements IMonsterBreakablePart {

    float hp;
    boolean isGone;
    PART partType;
    NewWorldMonsterEntity entityParent;
    public MonsterBreakablePartEntity(T parent, SubPartConfig properties, EntityDimensions baseSize, Vec3 basePosition, Vec3 pivot, float hp, boolean isGoneOnDeath, PART partType){
        super(parent, properties, baseSize, basePosition, pivot);
        setHP(hp);
        isGone = isGoneOnDeath;
        this.partType = partType;
        if(parent instanceof NewWorldMonsterEntity newWorldMonsterEntity)
            entityParent = newWorldMonsterEntity;

        MHNW.LOGGER.debug("Breakable part added: {}", this);
    }

    @Override
    public void setScaling(Vec3 scale) {
        super.setScaling(scale);
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
            if(isDead()){
                setInvulnerable(true);
                if(isGoneWhenDead()){
                    if(getPartType() == PART.TAIL && getParent() instanceof NewWorldMonsterEntity newWorldMonsterEntity){
                        NewWorldGrowingEntity summon = entityParent.getTailEntity().create(level());
                        Vec3 pos = getPosition(0);
                        summon.setPos(pos);
                        newWorldMonsterEntity.setTailCut(true);
                        summon.lookAt(EntityAnchorArgument.Anchor.EYES,entityParent.position());
                        summon.setMonsterScale(entityParent.getMonsterScale(),true);
                        summon.move(MoverType.PISTON,entityParent.getLookAngle().multiply(new Vec3(-1,2,-1)));
                        level().addFreshEntity(summon);
                    }
                    setEnabled(false);
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
    public void deserializeNBT(CompoundTag nbt) {
        super.deserializeNBT(nbt);
        if(nbt.contains("hp")){
            setHP(nbt.getFloat("hp"));
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag ret =  super.serializeNBT();
        ret.putFloat("hp", getHP());
        return ret;
    }

    @Override
    public String toString(){
        return getPartType().toString() + " type part: " + getConfigName() + " with health: " + getHP() +", status: " + (isGoneWhenDead()?"Can be cut":"Can be broken");
    }
}
