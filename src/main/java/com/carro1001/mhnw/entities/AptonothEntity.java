package com.carro1001.mhnw.entities;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.entities.ai.AptonothPanicOrAttack;
import com.carro1001.mhnw.utils.MHNWReferences;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.List;
import java.util.function.Predicate;

public class AptonothEntity extends NewWorldMonsterEntity {

    private static final RawAnimation RUNNING = RawAnimation.begin().thenPlay("animation.aptonoth.run");
    private static final RawAnimation EAT = RawAnimation.begin().thenPlay("animation.aptonoth.eat");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.aptonoth.attack");

    private static final EntityDataAccessor<Boolean> PANIC = SynchedEntityData.defineId(AptonothEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> PANICTIMER = SynchedEntityData.defineId(AptonothEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DATA_ID_FLAGS = SynchedEntityData.defineId(AptonothEntity.class, EntityDataSerializers.BYTE);

    private static final int PANIC_MAX_TICKS = (20*60)*3; // 20 ticks per second, 3 minutes
    private int eatingCounter;

    @javax.annotation.Nullable
    private AptonothEntity.AptonothAvoidEntityGoal aptonothAvoidPlayersGoal;

    public AptonothEntity(EntityType<? extends NewWorldMonsterEntity> p_27557_, Level p_27558_) {
        super(p_27557_, p_27558_);
        this.reassessPanicGoals();
        name = MHNWReferences.APTONOTH;
        deathTickTime = 20;
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
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.65D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.addBehaviourGoals();
    }

    protected void addBehaviourGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.25D, Ingredient.of(Items.GOLDEN_CARROT, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE), false));
    }

    @Override
    protected float getStandingEyeHeight(Pose pPose, EntityDimensions pSize) {
        float height = (2.2f)*getMonsterScale();
        return this.isBaby() ? height * 0.95F : 1.5F;
    }


    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide && this.isAlive()) {
            if (this.random.nextInt(900) == 0 && this.deathTime == 0) {
                this.heal(1.0F);
            }


            if (!this.isEating() && !this.isVehicle() && this.random.nextInt(300) == 0 && this.level().getBlockState(this.blockPosition().below()).is(Blocks.GRASS_BLOCK)) {
                this.setEating(true);
            }

            if (this.isEating() && ++this.eatingCounter > 80) {
                this.eatingCounter = 0;
                this.setEating(false);
            }

        }
    }

    @Override
    public void tick() {
        super.tick();
        if(isPanic()){
            PanicCooldown();
        }
    }

    public void setEating(boolean pEating) {
        this.setFlag(16, pEating);
    }

    // Data
    @Override
    protected void defineSynchedData () {
        super.defineSynchedData();
        this.entityData.define(PANIC, false);
        this.entityData.define(PANICTIMER, 0);
        this.entityData.define(DATA_ID_FLAGS, (byte) 0);

    }

    @Override
    public void addAdditionalSaveData (@NotNull CompoundTag pCompound){
        super.addAdditionalSaveData(pCompound);
        pCompound.putBoolean("EatingHaystack", this.isEating());
        pCompound.putBoolean("Panic", this.isPanic());
        pCompound.putInt("PanicCooldown", this.PanicTimer());

    }

    @Override
    public void readAdditionalSaveData (@NotNull CompoundTag pCompound){
        super.readAdditionalSaveData(pCompound);
        this.setEating(pCompound.getBoolean("EatingHaystack"));

        if (pCompound.getBoolean("Panic")) {
            this.panic();
            this.setPanicTimer(pCompound.getInt("PanicCooldown"));
        }
    }

    protected boolean getFlag ( int pFlagId){
        return (this.entityData.get(DATA_ID_FLAGS) & pFlagId) != 0;
    }

    protected void setFlag ( int pFlagId, boolean pValue){
        byte b0 = this.entityData.get(DATA_ID_FLAGS);
        if (pValue) {
            this.entityData.set(DATA_ID_FLAGS, (byte) (b0 | pFlagId));
        } else {
            this.entityData.set(DATA_ID_FLAGS, (byte) (b0 & ~pFlagId));
        }

    }

    public boolean isEating () {
        return this.getFlag(16);
    }

    public void panic () {
        if (!isPanic()) {
            this.entityData.set(PANIC, true);
            this.reassessPanicGoals();
        }
    }

    public void calm () {
        this.entityData.set(PANIC, false);
    }
    public boolean isPanic () {
        return this.entityData.get(PANIC);
    }

    public void setPanicTimer ( int timer){
        this.entityData.set(PANICTIMER, timer);
    }

    public int PanicTimer () {
        return this.entityData.get(PANICTIMER);
    }

    public void PanicCooldown () {
        int ticksRemain = PanicTimer() - 1;
        this.entityData.set(PANICTIMER, ticksRemain);
        if (ticksRemain <= 0) {
            calm();
            reassessPanicGoals();
        }
    }

    public static AttributeSupplier.Builder prepareAttributes () {
        return Mob.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ATTACK_KNOCKBACK, 3.0)
                .add(Attributes.MAX_HEALTH, 100)
                .add(Attributes.FOLLOW_RANGE, 15.0)
                .add(Attributes.MOVEMENT_SPEED, (double) 0.22F)
                .add(Attributes.ARMOR, 1.0D)
                .add(Attributes.JUMP_STRENGTH, 0.5)
                .add(Attributes.KNOCKBACK_RESISTANCE, (double) 0.6F)
                .add(Attributes.ARMOR_TOUGHNESS, 1.0D);
    }

    //Animation Controller
    @Override
    public void registerControllers (AnimatableManager.ControllerRegistrar controllers){
        controllers.add(new AnimationController<>(this, "main_controller", 5, this::poseBody).setAnimationSpeed(0.65),
                new AnimationController<>(this, "run_controller", 5, this::runBody).setAnimationSpeed(1.75),
                new AnimationController<>(this, "attack_die_controller", 5, this::fightBody));
    }

    protected PlayState fightBody (AnimationState < AptonothEntity > state) {
        if (getDeathState() >= 1 && onGround()) {
            MHNW.debugLog("fightBody: dead");
            return state.setAndContinue(getDeathAnimation());
        }
        if (isAggressive() || this.isAttacking()) {
            MHNW.debugLog("fightBody: attack");
            return state.setAndContinue(ATTACK);
        }
        return PlayState.STOP;
    }

    protected PlayState runBody (AnimationState < AptonothEntity > state) {
        if (getDeathState() >= 1 || isAggressive() || this.isAttacking()) return PlayState.STOP;
        if (isPanic()) {
            MHNW.debugLog("runBody: RUNNING");
            return state.setAndContinue(RUNNING);
        }
        boolean isRunning = state.isMoving();
        MHNW.debugLog(isRunning ? "WALK" : "IDLE");
        return state.setAndContinue(isRunning ? getMovementAnimation() : getIdleAnimation());
    }

    protected PlayState poseBody (AnimationState <NewWorldMonsterEntity> state) {
        if (getDeathState() >= 1 || isAggressive() || this.isAttacking()) return PlayState.STOP;
        if (this.isEating()) {
            MHNW.debugLog("isEating");
            return state.setAndContinue(EAT);
        }
        return PlayState.STOP;
    }

    //

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
