package com.carro1001.mhnw.entities;

import com.carro1001.mhnw.entities.ai.MonsterAggressionStateGoal;
import com.carro1001.mhnw.entities.interfaces.IGrows;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Random;

public class Monster extends PathfinderMob implements GeoEntity, IGrows {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Boolean> WALKING = SynchedEntityData.defineId(Monster.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Boolean> SCALESSIGNED = SynchedEntityData.defineId(Monster.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Float> MONSTER_SCALE = SynchedEntityData.defineId(Monster.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Integer> AGGRESSION_STATE = SynchedEntityData.defineId(Monster.class, EntityDataSerializers.INT);

    protected String name;

    public Monster(EntityType<? extends PathfinderMob> p_27557_, Level p_27558_) {
        super(p_27557_, p_27558_);
        this.noCulling = true;
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true)); // This finds the closest player to target
        this.goalSelector.addGoal(1, new MonsterAggressionStateGoal(this)); // This handles the aggression state between passive, roar, and aggressive
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.7D){
            @Override
            public void start() {
                super.start();
                setWalking(true);
            }

            @Override
            public void stop() {
                super.stop();
                setWalking(false);
            }
        });
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.addBehaviourGoals();
    }

    protected void addBehaviourGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main_controller", 16, this::poseBody));
    }

    protected PlayState poseBody(AnimationState<Monster> animationState) {
        if (this.getAggressionState() == AggressionState.ROAR) {
            return animationState.setAndContinue(getRoarAnimation(name));
        }

        if (animationState.isMoving() || IsWalking()) {
            return animationState.setAndContinue(getMovementAnimation(name));
        }

        return animationState.setAndContinue(getIdleAnimation(name));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public static AttributeSupplier.Builder prepareAttributes() {
        return Mob.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.MAX_HEALTH, 10)
                .add(Attributes.FOLLOW_RANGE, 15.0)
                .add(Attributes.MOVEMENT_SPEED, 0.45)
                .add(Attributes.ARMOR, 1.0D)
                .add(Attributes.ARMOR_TOUGHNESS,1.0D);
    }
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(WALKING, false);
        this.entityData.define(SCALESSIGNED, false);
        this.entityData.define(MONSTER_SCALE, 1F);
        this.entityData.define(AGGRESSION_STATE, AggressionState.PASSIVE.ordinal());

    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putFloat("Scale", this.getMonsterScale());
        pCompound.putInt("mon_aggro_state", getAggressionState().ordinal());

    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("Scale", Tag.TAG_FLOAT)) {
            this.setMonsterScale(pCompound.getFloat("Scale"));
        }

        if (pCompound.contains("mon_aggro_state", Tag.TAG_INT)) {
            this.setAggressionState(DragonEntity.AggressionState.values()[pCompound.getInt("mon_aggro_state")]);
        }
    }

    public void GenerateScale(){
        if(!getScaleAssignedDir()){
            setMonsterScale((float) new Random().nextDouble(0.5,1));
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
    public void setMonsterScale(float scale) {
        if(!getScaleAssignedDir()){
            this.entityData.set(MONSTER_SCALE, scale);
            this.setScaleAssignedDir(true);
        }
    }

    public void setAggressionState(DragonEntity.AggressionState aggressionState) {
        this.entityData.set(AGGRESSION_STATE, aggressionState.ordinal());
    }

    public DragonEntity.AggressionState getAggressionState() {
        return DragonEntity.AggressionState.values()[this.entityData.get(AGGRESSION_STATE)];
    }

    protected RawAnimation getIdleAnimation(String name){
        return RawAnimation.begin().thenLoop("animation."+name+".idle");
    }

    protected RawAnimation getMovementAnimation(String name) {
        return RawAnimation.begin().thenLoop("animation."+name+".walk");
    }

    protected RawAnimation getRoarAnimation(String name) {
        return RawAnimation.begin().thenLoop("animation."+name+".roar");
    }
    @Override
    public float getScale() {
        return getMonsterScale();
    }

    public void setWalking(boolean walking){
        this.entityData.set(WALKING, walking);
    }

    public boolean IsWalking(){
        return this.entityData.get(WALKING);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        GenerateScale();
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        double d0 = this.getBoundingBox().getSize();
        if (Double.isNaN(d0)) {
            d0 = 1.0D;
        }

        d0 *= 64.0D * 4;
        return pDistance < d0 * d0;
    }

    public enum AggressionState {
        PASSIVE,
        ROAR,
        AGGRESSIVE
    }

}
