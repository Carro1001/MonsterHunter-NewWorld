package com.carro1001.mhnw.entities;

import com.carro1001.mhnw.entities.ai.NewRathalosAggressionStateGoal;
import com.carro1001.mhnw.entities.ai.NewRathalosFlyingWaterAvoidingStrollGoal;
import com.carro1001.mhnw.entities.ai.NewRathalosShootFireballGoal;
import com.carro1001.mhnw.entities.ai.NewRathalosWaterAvoidingStrollGoal;
import com.carro1001.mhnw.entities.interfaces.IGrows;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class NewRathalosEntity extends PathfinderMob implements GeoEntity, IGrows {

    private static final EntityDataAccessor<Float> MONSTER_SCALE = SynchedEntityData.defineId(NewRathalosEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(NewRathalosEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AGGRESSION_STATE = SynchedEntityData.defineId(NewRathalosEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FIRE_BALL_CHARGE_STATE = SynchedEntityData.defineId(NewRathalosEntity.class, EntityDataSerializers.INT);

    private int fireballCooldownTime = 0;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public NewRathalosEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        setMovement();
    }

    @Override
    protected void checkFallDamage(double pY, boolean pOnGround, BlockState pState, BlockPos pPos) {
        if (getState() != State.FLYING) {
            super.checkFallDamage(pY, pOnGround, pState, pPos);
        }
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true)); // This finds the closest player
        this.goalSelector.addGoal(1, new NewRathalosAggressionStateGoal(this)); // This handles the aggression state
        this.goalSelector.addGoal(2, new NewRathalosMeleeAttackGoal(this, 1, false)); // This handles the chase and hit state
        this.goalSelector.addGoal(2, new NewRathalosShootFireballGoal(this)); // This handles the fireball attack state
        this.goalSelector.addGoal(3, new NewRathalosWaterAvoidingStrollGoal(this, 1, 0.05F)); // This handles on the ground random walk around
        this.goalSelector.addGoal(3, new NewRathalosFlyingWaterAvoidingStrollGoal(this, 1)); // This handles random flying around
    }

    public static AttributeSupplier.Builder prepareAttributes() {
        return Mob.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ATTACK_KNOCKBACK, 4.0)
                .add(Attributes.FOLLOW_RANGE, 64) // This is the target finding range
                .add(Attributes.MAX_HEALTH, 10)
                .add(Attributes.FOLLOW_RANGE, 128)
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.FLYING_SPEED, 10)
                .add(Attributes.ARMOR, 1.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 1.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(MONSTER_SCALE, 1F);
        this.entityData.define(STATE, State.FLYING.ordinal());
        this.entityData.define(AGGRESSION_STATE, AggressionState.PASSIVE.ordinal());
        this.entityData.define(FIRE_BALL_CHARGE_STATE, FireballState.READY.ordinal());
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();

        if (random.nextInt(500) == 0 && !level().isClientSide) {
//            this.setState(State.values()[(getState().ordinal() + 1) % State.values().length]);
        }

        if (!level().isClientSide) {
            if (getFireballChargeState() == FireballState.COOL_DOWN) {
                if (this.fireballCooldownTime >= 0) {
                    this.fireballCooldownTime--;
                } else {
                    setFireBallChargeState(FireballState.READY);
                }
            }
        }
    }

    public void shootFireball() {
        if (level().isClientSide) {
            return;
        }
        this.fireballCooldownTime = 500;
        Vec3 spawnPos = this.position().add(0, 10, 0);

        Vec3 velocity = this.getTarget().position().subtract(spawnPos).normalize().scale(6);

        LargeFireball largefireball = new LargeFireball(EntityType.FIREBALL, level());
        largefireball.setPos(spawnPos);
        largefireball.setDeltaMovement(velocity);
        largefireball.explosionPower = 10;


        level().addFreshEntity(largefireball);
        this.setFireBallChargeState(FireballState.COOL_DOWN);
    }

    @Override
    protected float getFlyingSpeed() {
        return super.getFlyingSpeed();
    }

    protected PathNavigation createFlyingPathNavigation(Level pLevel) {
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

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);

        pCompound.putFloat("mon_scale", getMonsterScale());
        pCompound.putInt("mon_state", getState().ordinal());
        pCompound.putInt("mon_aggro_state", getAggressionState().ordinal());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);

        if (pCompound.contains("mon_scale", Tag.TAG_FLOAT)) {
            this.setState(State.values()[pCompound.getInt("mon_scale")]);
        }

        if (pCompound.contains("mon_state", Tag.TAG_INT)) {
            this.setState(State.values()[pCompound.getInt("mon_state")]);
        }

        if (pCompound.contains("mon_aggro_state", Tag.TAG_INT)) {
            this.setAggressionState(AggressionState.values()[pCompound.getInt("mon_aggro_state")]);
        }
    }

    // Mimic Slime scaling
    @Override
    public void refreshDimensions() {
        double d0 = this.getX();
        double d1 = this.getY();
        double d2 = this.getZ();
        super.refreshDimensions();
        this.setPos(d0, d1, d2);
    }

    // Mimic Slime scaling
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        if (MONSTER_SCALE.equals(pKey)) {
            this.refreshDimensions();
            this.setYRot(this.yHeadRot);
            this.yBodyRot = this.yHeadRot;
            if (this.isInWater() && this.random.nextInt(20) == 0) {
                this.doWaterSplashEffect();
            }
        }

        if (STATE.equals(pKey)) {
            setMovement();
        }

        super.onSyncedDataUpdated(pKey);
    }

    private void setMovement() {
        if (getState() == State.WALKING) {
            this.moveControl = new MoveControl(this);
            this.navigation = this.createNavigation(level());
            this.setNoGravity(false);
        } else {
            this.moveControl = new FlyingMoveControl(this, 1, true);
            this.navigation = createFlyingPathNavigation(level());
            this.setNoGravity(true);
        }
    }

    public EntityDimensions getDimensions(Pose pPose) {
        return super.getDimensions(pPose).scale(0.255F * this.getMonsterScale());
    }


    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<GeoAnimatable>(this, "main_controller", 16, this::mainAnimationStateHandler));
    }

    protected <E extends GeoAnimatable> PlayState mainAnimationStateHandler(AnimationState<E> animationState) {
        if (this.getAggressionState() == AggressionState.ROAR) {
            return animationState.setAndContinue(getState().roarAnimation);
        }
        if (this.getFireballChargeState() == FireballState.CHARGING) {
            return animationState.setAndContinue(getState().fireballAnimation);
        }

        if (animationState.isMoving()) {
            return animationState.setAndContinue(getState().movementAnimation);
        }

        return animationState.setAndContinue(getState().idleAnimation);
    }


    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public float getMonsterScale() {
        return this.entityData.get(MONSTER_SCALE);
    }

    @Override
    public void setMonsterScale(float scale) {
        this.entityData.set(MONSTER_SCALE, scale);
    }

    public void setState(State state) {
        this.entityData.set(STATE, state.ordinal());
    }

    public State getState() {
        return State.values()[this.entityData.get(STATE)];
    }

    public void setAggressionState(AggressionState aggressionState) {
        this.entityData.set(AGGRESSION_STATE, aggressionState.ordinal());
    }

    public AggressionState getAggressionState() {
        return AggressionState.values()[this.entityData.get(AGGRESSION_STATE)];
    }

    public void setFireBallChargeState(FireballState aggressionState) {
        this.entityData.set(FIRE_BALL_CHARGE_STATE, aggressionState.ordinal());
    }

    public FireballState getFireballChargeState() {
        return FireballState.values()[this.entityData.get(FIRE_BALL_CHARGE_STATE)];
    }


    public enum State {
        FLYING(RawAnimation.begin().thenLoop("animation.rathalos.hover"), RawAnimation.begin().thenLoop("animation.rathalos.fly"), RawAnimation.begin().thenPlayAndHold("animation.rathalos.aerial_roar"), RawAnimation.begin().thenPlayAndHold("animation.rathalos.air_fireball")),
        WALKING(RawAnimation.begin().thenLoop("animation.rathalos.idle_normal"), RawAnimation.begin().thenLoop("animation.rathalos.walk_normal"), RawAnimation.begin().thenPlayAndHold("animation.rathalos.roar"), RawAnimation.begin().thenPlayAndHold("animation.rathalos.ground_fireball"));


        private final RawAnimation idleAnimation;
        private final RawAnimation movementAnimation;
        private final RawAnimation roarAnimation;
        private final RawAnimation fireballAnimation;

        State(RawAnimation idleAnimation, RawAnimation movementAnimation, RawAnimation roarAnimation, RawAnimation fireballAnimation) {
            this.idleAnimation = idleAnimation;
            this.movementAnimation = movementAnimation;
            this.roarAnimation = roarAnimation;
            this.fireballAnimation = fireballAnimation;
        }
    }

    public enum AggressionState {
        PASSIVE,
        ROAR,
        AGGRESSIVE
    }

    public enum FireballState {
        CHARGING,
        READY,
        COOL_DOWN
    }
}
