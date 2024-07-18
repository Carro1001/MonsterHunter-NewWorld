package com.carro1001.mhnw.entities;

import com.carro1001.mhnw.entities.interfaces.IGrows;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
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

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import static com.carro1001.mhnw.registration.ModEntities.APTONOTH;

public class AptonothEntity extends AbstractHorse implements GeoEntity, IGrows {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation WALK = RawAnimation.begin().thenPlay("animation.aptonoth.walk");
    private static final RawAnimation IDLE = RawAnimation.begin().thenPlay("animation.aptonoth.idle");
    private static final RawAnimation EAT = RawAnimation.begin().thenPlay("animation.aptonoth.eat");

    private static final EntityDataAccessor<Boolean> WALKING = SynchedEntityData.defineId(AptonothEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SCALESSIGNED = SynchedEntityData.defineId(AptonothEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> MONSTER_SCALE = SynchedEntityData.defineId(AptonothEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> PANIC = SynchedEntityData.defineId(AptonothEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> PANICTIMER = SynchedEntityData.defineId(AptonothEntity.class, EntityDataSerializers.INT);
    private static final int PANIC_MAX_TICKS = (20*60)*3; // 20 ticks per second, 3 minutes

    @javax.annotation.Nullable
    private AptonothEntity.AptonothAvoidEntityGoal aptonothAvoidPlayersGoal;

    public AptonothEntity(EntityType<? extends AbstractHorse> p_27557_, Level p_27558_) {
        super(p_27557_, p_27558_);
        this.noCulling = true;
        this.reassessPanicGoals();

    }

    @Override
    public void tick() {
        super.tick();
        if(isPanic()){
            PanicCooldown();
        }
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        boolean flag = super.hurt(pSource, pAmount);
        if (pSource.getEntity() instanceof LivingEntity && !level().isClientSide) {
            List<AptonothEntity> list = level().getEntitiesOfClass(AptonothEntity.class, getBoundingBox().inflate(16.0));
            list.forEach(AptonothEntity::panic);
            panic();
        }
        return flag;
    }

    protected void reassessPanicGoals() {
        if (this.aptonothAvoidPlayersGoal == null) {
            this.aptonothAvoidPlayersGoal = new AptonothEntity.AptonothAvoidEntityGoal(this, 45.0F, 1, 1.5);
        }

        this.goalSelector.removeGoal(this.aptonothAvoidPlayersGoal);
        if (this.isPanic()) {
            this.goalSelector.addGoal(1, this.aptonothAvoidPlayersGoal);
            setPanicTimer(PANIC_MAX_TICKS);
        }
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.2D));
        this.goalSelector.addGoal(1, new RunAroundLikeCrazyGoal(this, 1.2D));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D, AbstractHorse.class));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D){
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
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        if (this.canPerformRearing()) {
            this.goalSelector.addGoal(9, new RandomStandGoal(this));
        }

        this.addBehaviourGoals();
    }

//Animation Controller
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main_controller", 5, this::poseBody).setAnimationSpeed(1.3));
    }

    protected PlayState poseBody(AnimationState<AptonothEntity> state) {
        if (this.isEating()){
            return state.setAndContinue(EAT);
        }
        return state.setAndContinue(state.isMoving() || IsWalking() ? WALK : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
//
// Data
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(WALKING, false);
        this.entityData.define(SCALESSIGNED, false);
        this.entityData.define(MONSTER_SCALE, 1F);
        this.entityData.define(PANIC, false);
        this.entityData.define(PANICTIMER, 0);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putFloat("Scale", this.getMonsterScale());
        pCompound.putBoolean("Panic", this.isPanic());
        pCompound.putInt("PanicCooldown", this.PanicTimer());
        pCompound.putInt("Walking", this.PanicTimer());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if(pCompound.getBoolean("Panic")){
            this.panic();
            this.setPanicTimer(pCompound.getInt("PanicCooldown"));
        }
        this.setMonsterScale(pCompound.getFloat("Scale"));
    }

    public void panic(){
        if(!isPanic()){
            this.entityData.set(PANIC, true);
            this.reassessPanicGoals();
        }
    }
    public void calm(){
        this.entityData.set(PANIC, false);
    }
    public boolean isPanic(){
        return this.entityData.get(PANIC);
    }

    public void setPanicTimer(int timer){
        this.entityData.set(PANICTIMER, timer);
    }

    public int PanicTimer(){
        return this.entityData.get(PANICTIMER);
    }

    public void PanicCooldown(){
        int ticksRemain = PanicTimer()-1;
        this.entityData.set(PANICTIMER,ticksRemain);
        if(ticksRemain <= 0){
            calm();
            reassessPanicGoals();
        }
    }

    public void setWalking(boolean walking){
        this.entityData.set(WALKING, walking);
    }

    public boolean IsWalking(){
        return this.entityData.get(WALKING);
    }
//region Scale
    @Override
    public float getScale() {
        return getMonsterScale();
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
//
//

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        GenerateScale();
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
    }

    public static AttributeSupplier.Builder prepareAttributes() {
        return Mob.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.MAX_HEALTH, 10)
                .add(Attributes.FOLLOW_RANGE, 15.0)
                .add(Attributes.MOVEMENT_SPEED, (double)0.22F)
                .add(Attributes.ARMOR, 1.0D)
                .add(Attributes.JUMP_STRENGTH, 0.5)
                .add(Attributes.ARMOR_TOUGHNESS,1.0D);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(@NotNull ServerLevel p_146743_, @NotNull AgeableMob p_146744_) {
        return APTONOTH.get().create(p_146743_);
    }

    static class AptonothAvoidEntityGoal extends AvoidEntityGoal {

        private final AptonothEntity aptonoth;
        public AptonothAvoidEntityGoal(AptonothEntity aptonoth, float pMaxDist, double pWalkSpeedModifier, double pSprintSpeedModifier) {
            super(aptonoth, Player.class, pMaxDist, pWalkSpeedModifier, pSprintSpeedModifier, ((Predicate)EntitySelector.NO_CREATIVE_OR_SPECTATOR)::test);
            this.aptonoth = aptonoth;
        }

        public boolean canUse() {
            return this.aptonoth.isPanic() && super.canUse();
        }

        public boolean canContinueToUse() {
            return this.aptonoth.isPanic() && super.canContinueToUse();
        }
    }
}
