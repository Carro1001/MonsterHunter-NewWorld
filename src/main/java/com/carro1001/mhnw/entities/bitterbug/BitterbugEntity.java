package com.carro1001.mhnw.entities.bitterbug;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;
import java.util.Random;

public class BitterbugEntity extends Mob {
    private static final EntityDataAccessor<Integer> TYPE = SynchedEntityData.defineId(BitterbugEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> TYPEASSIGNED = SynchedEntityData.defineId(BitterbugEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_POWERED = SynchedEntityData.defineId(BitterbugEntity.class, EntityDataSerializers.BOOLEAN);

    public BitterbugEntity(EntityType<? extends Mob> p_27557_, Level p_27558_) {
        super(p_27557_, p_27558_);

    }
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TYPEASSIGNED, false);
        this.entityData.define(TYPE, 0);

        this.entityData.define(DATA_IS_POWERED, false);
    }
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        if (this.entityData.get(DATA_IS_POWERED)) {
            pCompound.putBoolean("powered", true);
        }
        pCompound.putBoolean("type_assign", getTypeAssignedDir());
        pCompound.putInt("type", getTypeDir());

    }
    public boolean getTypeAssignedDir() {
        return this.entityData.get(TYPEASSIGNED);
    }
    public int getTypeDir() {
        return this.entityData.get(TYPE);
    }
    public void setTypeAssignedDir(boolean pState) {
        this.entityData.set(TYPEASSIGNED, pState);
    }
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);

        this.entityData.set(DATA_IS_POWERED, pCompound.getBoolean("powered"));

        this.setTypeDir(pCompound.getInt("type"));

    }
    public void setTypeDir(int pState) {
        if(!getTypeAssignedDir()){
            this.entityData.set(TYPE, pState);
            this.setTypeAssignedDir(true);
        }
    }
    public void setType() {
        if (!getTypeAssignedDir()){
            Random random = new Random();
            setTypeDir(random.nextInt(2));

        }

    }
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor p_149132_, DifficultyInstance p_149133_, MobSpawnType p_149134_, @Nullable SpawnGroupData p_149135_, @Nullable CompoundTag p_149136_) {
        this.setType();
        return super.finalizeSpawn(p_149132_, p_149133_, p_149134_, p_149135_, p_149136_);
    }
    public static AttributeSupplier.Builder prepareAttributes() {
        return Mob.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.MAX_HEALTH, 10)
                .add(Attributes.FOLLOW_RANGE, 15.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ARMOR, 1.0D)
                .add(Attributes.ARMOR_TOUGHNESS,1.0D);
    }
}
