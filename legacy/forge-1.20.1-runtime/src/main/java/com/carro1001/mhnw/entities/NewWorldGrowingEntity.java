package com.carro1001.mhnw.entities;

import com.carro1001.mhnw.entities.interfaces.IGrows;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animation.AnimatableManager;

import java.util.Random;

public class NewWorldGrowingEntity extends NewWorldEntity implements IGrows {
    protected static final EntityDataAccessor<Boolean> SCALESSIGNED = SynchedEntityData.defineId(NewWorldGrowingEntity.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Float> MONSTER_SCALE = SynchedEntityData.defineId(NewWorldGrowingEntity.class, EntityDataSerializers.FLOAT);
    protected float minScale = 0.6f, maxScale = 1f;

    public NewWorldGrowingEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        fixupDimensions();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SCALESSIGNED, false);
        this.entityData.define(MONSTER_SCALE, 1F);

    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putFloat("Scale", this.getMonsterScale());

    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("Scale", Tag.TAG_FLOAT)) {
            this.setMonsterScale(pCompound.getFloat("Scale"), false);
        }
    }

    public void GenerateScale(){
        if(!getScaleAssignedDir()){
            setMonsterScale((float) new Random().nextDouble(minScale, maxScale), false);
            setScaleAssignedDir(true);
        }
    }

    public void setScaleAssignedDir(boolean pState) {
        this.entityData.set(SCALESSIGNED, pState);
    }

    public boolean getScaleAssignedDir() {
        return this.entityData.get(SCALESSIGNED);
    }

    @Override
    public float getMonsterScale() {
        return this.entityData.get(MONSTER_SCALE);
    }

    @Override
    public void setMonsterScale(float scale, boolean force) {
        if(!getScaleAssignedDir() || force){
            this.entityData.set(MONSTER_SCALE, scale);
            this.setScaleAssignedDir(true);
            this.reapplyPosition();
            this.refreshDimensions();
        }
    }

    public void refreshDimensions() {
        double d0 = this.getX();
        double d1 = this.getY();
        double d2 = this.getZ();
        super.refreshDimensions();
        this.setPos(d0, d1, d2);
    }

    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        if (MONSTER_SCALE.equals(pKey)) {
            this.refreshDimensions();
            this.setYRot(this.yHeadRot);
            this.yBodyRot = this.yHeadRot;
        }
        super.onSyncedDataUpdated(pKey);
    }

    public EntityDimensions getDimensions(Pose pPose) {
        return super.getDimensions(pPose).scale(getMonsterScale());
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        GenerateScale();
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

    }
}
