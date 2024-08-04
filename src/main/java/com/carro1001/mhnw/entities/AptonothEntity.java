package com.carro1001.mhnw.entities;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.entities.ai.AptonothPanicOrAttack;
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
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
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
    private static final RawAnimation RUNNING = RawAnimation.begin().thenPlay("animation.aptonoth.run");
    private static final RawAnimation WALK = RawAnimation.begin().thenPlay("animation.aptonoth.walk");
    private static final RawAnimation IDLE = RawAnimation.begin().thenPlay("animation.aptonoth.idle");
    private static final RawAnimation EAT = RawAnimation.begin().thenPlay("animation.aptonoth.eat");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.aptonoth.death");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.aptonoth.attack");

    //0 is normal, 1 is dying to trigger animation and 2 is dead animation is done
    private static final EntityDataAccessor<Integer> DEATH_STATE = SynchedEntityData.defineId(AptonothEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WALKING = SynchedEntityData.defineId(AptonothEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ATTACKING = SynchedEntityData.defineId(AptonothEntity.class, EntityDataSerializers.BOOLEAN);
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
        fixupDimensions();

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
        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this)).setAlertOthers(AptonothEntity.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.goalSelector.addGoal(1, new AptonothPanicOrAttack(this, 1.4D));
        this.goalSelector.addGoal(1, new RunAroundLikeCrazyGoal(this, 1.2D));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D, AbstractHorse.class));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.65D){
            @Override
            public void start() {
                super.start();
                setWalking(1);
            }

            @Override
            public void stop() {
                super.stop();
                setWalking(0);
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
        controllers.add(new AnimationController<>(this, "main_controller", 5, this::poseBody).setAnimationSpeed(0.65),
                new AnimationController<>(this, "run_controller", 5, this::runBody).setAnimationSpeed(1.75),
                new AnimationController<>(this, "attack_die_controller", 5, this::fightBody));
    }
    protected PlayState fightBody(AnimationState<AptonothEntity> state) {
        if(getDeathState() >= 1 && onGround()){
            MHNW.debugLog("fightBody: dead");
            setDeathState(2);
            return state.setAndContinue(DEATH);
        }
        if(getDeathState() >= 1) return PlayState.STOP;
        if (isAggressive() || this.Attacking()){
            MHNW.debugLog("fightBody: attack");
            return state.setAndContinue(ATTACK);
        }
        return PlayState.STOP;
    }
    protected PlayState runBody(AnimationState<AptonothEntity> state) {
        if(getDeathState() >= 1) return PlayState.STOP;
        if ((this.IsRunning() || isPanic())){
            MHNW.debugLog("runBody: RUNNING");
            return state.setAndContinue(RUNNING);
        }
        boolean isRunning = state.isMoving() || IsWalking();
        MHNW.debugLog(isRunning ? "WALK" : "IDLE");
        return state.setAndContinue(isRunning ? WALK : IDLE);
    }

    protected PlayState poseBody(AnimationState<AptonothEntity> state) {
        if(getDeathState() >= 1) return PlayState.STOP;
        if (this.isEating() && !(this.Attacking() || isAggressive())){
            MHNW.debugLog("isEating");
            return state.setAndContinue(EAT);
        }
        return PlayState.STOP;
    }

    @Override
    public void die(DamageSource pDamageSource) {
        this.setHealth(1);
        int state = getDeathState();
        if(state == 0){
            MHNW.debugLog("aptonoth@"+this.position()+": i guess i will die");
            setDeathState(1);
            setNoAi(true);
        }
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
        this.entityData.define(DEATH_STATE, 0);
        this.entityData.define(WALKING, 0);
        this.entityData.define(ATTACKING, false);
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
        pCompound.putInt("Walking", this.getWalkingState());
        pCompound.putInt("DeathState", this.getDeathState());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if(pCompound.getBoolean("Panic")){
            this.panic();
            this.setPanicTimer(pCompound.getInt("PanicCooldown"));
        }
        this.setMonsterScale(pCompound.getFloat("Scale"));
        setWalking(pCompound.getInt("Walking"));
        setDeathState(pCompound.getInt("DeathState"));
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

    public void setWalking(int state){
        this.entityData.set(WALKING, state);
    }

    public boolean IsWalking(){
        return getWalkingState() == 1;
    }

    public boolean IsRunning(){
        return getWalkingState() == 2;
    }

    public int getWalkingState(){
        return this.entityData.get(WALKING);
    }

    public void setAttacking(boolean attacking){
        this.entityData.set(ATTACKING, attacking);
    }

    public boolean Attacking(){
        return this.entityData.get(ATTACKING);
    }

    public void setDeathState(int state){
        this.entityData.set(DEATH_STATE, state);
    }

    public int getDeathState(){
        return this.entityData.get(DEATH_STATE);
    }

//region Scale

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

    protected float getStandingEyeHeight(Pose pPose, EntityDimensions pSize) {
        float height = (2.2f)*getMonsterScale();
        return this.isBaby() ? height * 0.95F : 1.5F;
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
//
//

    @Override
    public boolean canMate(Animal pOtherAnimal) {
        return pOtherAnimal instanceof AptonothEntity;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        GenerateScale();
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
    }

    public static AttributeSupplier.Builder prepareAttributes() {
        return Mob.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ATTACK_KNOCKBACK, 3.0)
                .add(Attributes.MAX_HEALTH, 100)
                .add(Attributes.FOLLOW_RANGE, 15.0)
                .add(Attributes.MOVEMENT_SPEED, (double)0.22F)
                .add(Attributes.ARMOR, 1.0D)
                .add(Attributes.JUMP_STRENGTH, 0.5)
                .add(Attributes.KNOCKBACK_RESISTANCE, (double)0.6F)
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
            boolean canUse = this.aptonoth.isPanic() && super.canUse();
            MHNW.debugLog("AptonothAvoidEntityGoal canUse:" + canUse);
            return canUse;
        }

        public boolean canContinueToUse() {
            return this.aptonoth.isPanic() && super.canContinueToUse();
        }
    }
}
