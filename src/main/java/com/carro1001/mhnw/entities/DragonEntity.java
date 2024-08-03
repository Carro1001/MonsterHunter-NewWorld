package com.carro1001.mhnw.entities;

import com.carro1001.mhnw.entities.ai.DragonFlyingWaterAvoidingStrollGoal;
import com.carro1001.mhnw.entities.ai.DragonMeleeAttackGoal;
import com.carro1001.mhnw.entities.ai.DragonShootFireballGoal;
import com.carro1001.mhnw.entities.ai.DragonWaterAvoidingStrollGoal;
import com.carro1001.mhnw.entities.ai.util.SmartBodyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import javax.annotation.Nullable;

public abstract class DragonEntity extends LargeMonster {

    protected int fireballCooldownTime = 0;

    protected static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Integer> FIRE_BALL_CHARGE_STATE = SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.INT);

    public DragonEntity(EntityType<? extends LargeMonster > pEntityType, Level pLevel, String name) {
        super(pEntityType, pLevel);
        this.name = name;
        setMovement();
        if (!pLevel.isClientSide) {
            fireballCooldownTime = this.random.nextInt(500, 2000);
            this.setFireBallChargeState(DragonEntity.FireballState.COOL_DOWN);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            if (getFireballChargeState() == DragonEntity.FireballState.COOL_DOWN) {
                if (this.fireballCooldownTime >= 0) {
                    this.fireballCooldownTime--;
                } else {
                    setFireBallChargeState(DragonEntity.FireballState.READY);
                }
            }
        }
    }

    public void shootFireball() {
        if (level().isClientSide || this.fireballCooldownTime > 0) {
            return;
        }
        this.fireballCooldownTime = this.random.nextInt(500, 2000);
        Vec3 spawnPos = this.position().add(0, 10, 0);

        Vec3 velocity = this.getTarget().position().subtract(spawnPos).normalize().scale(6);

        LargeFireball largefireball = new LargeFireball(EntityType.FIREBALL, level());
        largefireball.setPos(spawnPos);
        largefireball.setDeltaMovement(velocity);
        //largefireball.explosionPower = 1;


        level().addFreshEntity(largefireball);
        this.setFireBallChargeState(DragonEntity.FireballState.COOL_DOWN);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true)); // This finds the closest player to target
        this.goalSelector.addGoal(2, new DragonMeleeAttackGoal(this, 1, false)); // This handles the chase and melee hit state
        this.goalSelector.addGoal(2, new DragonShootFireballGoal(this)); // This handles the fireball attack state
        this.goalSelector.addGoal(3, new DragonWaterAvoidingStrollGoal(this, 1, 0.05F)); // This handles on the ground random walk around
        this.goalSelector.addGoal(3, new DragonFlyingWaterAvoidingStrollGoal(this, 1)); // This handles random flying around
    }

    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor p_149132_, @NotNull DifficultyInstance p_149133_, @NotNull MobSpawnType p_149134_, @Nullable SpawnGroupData p_149135_, @Nullable CompoundTag p_149136_) {
        setState(State.WALKING);
        return super.finalizeSpawn(p_149132_, p_149133_, p_149134_, p_149135_, p_149136_);
    }

    //region Entity Data
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STATE, State.WALKING.ordinal());
        this.entityData.define(FIRE_BALL_CHARGE_STATE, FireballState.READY.ordinal());
    }

    public static AttributeSupplier.Builder prepareAttributes() {
        return Mob.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ATTACK_KNOCKBACK, 4.0)
                .add(Attributes.FOLLOW_RANGE, 64) // This is the target finding range
                .add(Attributes.MAX_HEALTH, 10)
                .add(Attributes.FOLLOW_RANGE, 128)
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.FLYING_SPEED, 25)
                .add(Attributes.ARMOR, 1.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 1.0D);
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new SmartBodyHelper(this);
    }

    protected FlyingPathNavigation createFlyingPathNavigation(Level pLevel) {
        FlyingPathNavigation flyingpathnavigation = new FlyingPathNavigation(this, pLevel) {
            public boolean isStableDestination(BlockPos p_27947_) {
                return !this.level.getBlockState(p_27947_.below()).isAir();
            }
        };
        flyingpathnavigation.setCanOpenDoors(false);
        flyingpathnavigation.setCanFloat(true);
        flyingpathnavigation.setCanPassDoors(true);
        return flyingpathnavigation;
    }

    private void setMovement() {
        this.navigation.stop();

        if (getState() == DragonEntity.State.WALKING) {
            this.moveControl = new MoveControl(this);
            this.navigation = this.createNavigation(level());
            this.setNoGravity(false);
        } else {
            this.moveControl = new FlyingMoveControl(this, 1, true);
            this.navigation = createFlyingPathNavigation(level());
            this.setNoGravity(true);
        }
    }

    @Override
    protected void checkFallDamage(double pY, boolean pOnGround, BlockState pState, BlockPos pPos) {
        if (getState() != DragonEntity.State.FLYING) {
            super.checkFallDamage(pY, pOnGround, pState, pPos);
        }
    }

    @Override
    protected float getFlyingSpeed() {
        return super.getFlyingSpeed();
    }

    @Override
    public boolean fireImmune() {
        return true;
    }


    protected PlayState poseBody(AnimationState<LargeMonster> animationState) {
        if(getDeathState() >= 1){
            return animationState.setAndContinue(getDeathAnimation());
        }
        if (this.getAggressionState() == DragonEntity.AggressionState.ROAR) {
            return animationState.setAndContinue(getState().getRoarAnimation(name));
        }
        if (this.getFireballChargeState() == DragonEntity.FireballState.CHARGING) {
            return animationState.setAndContinue(getState().getFireballAnimation(name));
        }

        if (animationState.isMoving()) {
            return animationState.setAndContinue(getState().getMovementAnimation(name));
        }

        return animationState.setAndContinue(getState().getIdleAnimation(name));
    }

    @Override
    protected RawAnimation getIdleAnimation(){
        return getState().getIdleAnimation(name);
    }

    @Override
    protected RawAnimation getMovementAnimation() {
        return getState().getMovementAnimation(name);
    }

    @Override
    protected RawAnimation getRoarAnimation() {
        return getState().getRoarAnimation(name);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("mon_state", getState().ordinal());
        pCompound.putInt("mon_fireball_state", getFireballChargeState().ordinal());
        pCompound.putInt("mon_fireball_cooldown", this.fireballCooldownTime);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);

        if (pCompound.contains("mon_state", Tag.TAG_INT)) {
            this.setState(DragonEntity.State.values()[pCompound.getInt("mon_state")]);
        }

        if (pCompound.contains("mon_fireball_state", Tag.TAG_INT)) {
            this.setFireBallChargeState(DragonEntity.FireballState.values()[pCompound.getInt("mon_fireball_state")]);
        }

        if (pCompound.contains("mon_fireball_cooldown", Tag.TAG_INT)) {
            this.fireballCooldownTime = pCompound.getInt("mon_fireball_cooldown");
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        if (STATE.equals(pKey)) {
            setMovement();
        }
        super.onSyncedDataUpdated(pKey);
    }

    public void setState(DragonEntity.State state) {
        this.entityData.set(STATE, state.ordinal());
    }
    public DragonEntity.State getState() {
        return DragonEntity.State.values()[this.entityData.get(STATE)];
    }

    public void setFireBallChargeState(DragonEntity.FireballState fireBallChargeState) {
        this.entityData.set(FIRE_BALL_CHARGE_STATE, fireBallChargeState.ordinal());
    }

    public DragonEntity.FireballState getFireballChargeState() {
        return DragonEntity.FireballState.values()[this.entityData.get(FIRE_BALL_CHARGE_STATE)];
    }

    public void setFireballCooldownTime(int fireballCooldownTime) {
        this.fireballCooldownTime = fireballCooldownTime;
    }

    public int getFireballCooldownTime() {
        return fireballCooldownTime;
    }

    public enum State {
        FLYING("hover", "fly", "aerial_roar", "air_fireball"),
        WALKING("idle_normal", "walk_normal", "roar","ground_fireball");


        private final String idleAnimation;
        private final String movementAnimation;
        private final String roarAnimation;
        private final String fireballAnimation;

        State(String idleAnimation, String movementAnimation, String roarAnimation, String fireballAnimation) {
            this.idleAnimation = idleAnimation;
            this.movementAnimation = movementAnimation;
            this.roarAnimation = roarAnimation;
            this.fireballAnimation = fireballAnimation;
        }
        RawAnimation getIdleAnimation(String name){
            return RawAnimation.begin().thenLoop("animation."+name+"."+idleAnimation);
        }

        public RawAnimation getMovementAnimation(String name) {
            return RawAnimation.begin().thenLoop("animation."+name+"."+movementAnimation);
        }

        public RawAnimation getRoarAnimation(String name) {
            return RawAnimation.begin().thenLoop("animation."+name+"."+roarAnimation);
        }

        public RawAnimation getFireballAnimation(String name) {
            return RawAnimation.begin().thenLoop("animation."+name+"."+fireballAnimation);
        }
    }

    public enum FireballState {
        CHARGING,
        READY,
        COOL_DOWN
    }
}
