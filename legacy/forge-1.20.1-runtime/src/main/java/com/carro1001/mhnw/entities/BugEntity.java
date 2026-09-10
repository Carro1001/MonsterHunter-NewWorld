package com.carro1001.mhnw.entities;

import com.carro1001.mhnw.setup.ModConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Random;

public class BugEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> TYPE = SynchedEntityData.defineId(BugEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> TYPEASSIGNED = SynchedEntityData.defineId(BugEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_POWERED = SynchedEntityData.defineId(BugEntity.class, EntityDataSerializers.BOOLEAN);

    public BugEntity(EntityType<? extends PathfinderMob> p_27557_, Level p_27558_) {
        super(p_27557_, p_27558_);

    }
    protected void registerGoals() {
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this,0.3));
        this.goalSelector.addGoal(8, new FloatGoal(this));
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TYPEASSIGNED, false);
        this.entityData.define(TYPE, 0);

        this.entityData.define(DATA_IS_POWERED, false);
    }
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
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
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
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

        while (!getTypeAssignedDir()){
            Random random = new Random();
            float chance = random.nextFloat();
            switch(random.nextInt(2)){
                case 1:
                    if(chance < ModConfig.GODBUG_CHANCE.get())setTypeDir(1);
                    break;
                case 0:
                    if(chance < 1-ModConfig.GODBUG_CHANCE.get())setTypeDir(0);
                    break;
            }
        }


    }
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor p_149132_, @NotNull DifficultyInstance p_149133_, @NotNull MobSpawnType p_149134_, @Nullable SpawnGroupData p_149135_, @Nullable CompoundTag p_149136_) {
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
