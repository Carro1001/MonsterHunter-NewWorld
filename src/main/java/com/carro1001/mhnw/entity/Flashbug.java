package com.carro1001.mhnw.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The second endemic-life species: flying, harmless, and the second real caller of
 * {@link EndemicAreaEffectGoal} ({@link FlashbugFlashGoal}), the point at which extracting that
 * shared base out of {@link ToadFuseGoal} actually earned its keep.
 *
 * <p>Flight reuses vanilla's own combination for a small ambient flyer: {@link FlyingMoveControl}
 * with hovering, and {@link FlyingPathNavigation}, the same pairing {@code Parrot} uses (minus the
 * taming/riding machinery this creature has no use for).
 *
 * <p>Two authored clips are named for this species in the preserved animation file:
 * {@code animation.flashfly.fly} (0.54 s, does not loop) and {@code animation.flashbug.fly} (2 s,
 * loops). The differing prefix looks like a leftover from renaming the species during authoring,
 * not two deliberately distinct clips; going purely by shape, the short non-looping one reads as a
 * one-shot burst and the long looping one as sustained flight, so that is how they are used here
 * (short clip for the flash telegraph, long clip for ordinary flying). This is an inference from
 * the asset, not a confirmed authorial intent.
 */
public class Flashbug extends PathfinderMob implements GeoEntity {

    public static final double MAX_HEALTH = 3.0D;
    public static final float BODY_WIDTH = 0.4F;
    public static final float BODY_HEIGHT = 0.3F;
    /** Keeps it within player melee/jump reach instead of drifting up out of reach. */
    private static final double MAX_HOVER_HEIGHT = 4.0D;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.flashbug.idle");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.flashbug.fly");
    private static final RawAnimation FLASH = RawAnimation.begin().thenPlay("animation.flashfly.fly");

    private static final EntityDataAccessor<Boolean> DATA_FLASHING =
            SynchedEntityData.defineId(Flashbug.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    /** Set by {@link #hurt}, read and cleared by {@link FlashbugFlashGoal}. Transient, not saved. */
    boolean provoked;

    public Flashbug(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.FLYING_SPEED, 0.4D)
                .add(Attributes.MOVEMENT_SPEED, 0.15D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FlashbugFlashGoal(this));
        this.goalSelector.addGoal(1, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 5.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        // No target selector: like Toad, it never fights and never flees, only flashes in place
        // (handoff: "flying endemic life with a bounded flash effect").
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FLASHING, false);
    }

    public boolean isFlashing() {
        return this.entityData.get(DATA_FLASHING);
    }

    void setFlashing(boolean flashing) {
        this.entityData.set(DATA_FLASHING, flashing);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide) {
            this.provoked = true;
        }
        return hurt;
    }

    /** A flyer never takes fall damage; it has no floor to fall onto in the first place. */
    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        clampHoverHeight();
    }

    /**
     * Nudges it back down whenever it drifts above {@link #MAX_HOVER_HEIGHT} blocks off the ground
     * below it, rather than constraining the wander goal itself: cheaper, and it also catches
     * spawning too high. No lower bound is enforced — flying close to the ground is fine and is
     * what keeps it near player reach.
     */
    private void clampHoverHeight() {
        double groundY = findGroundY();
        if (groundY != Double.MIN_VALUE && getY() - groundY > MAX_HOVER_HEIGHT) {
            net.minecraft.world.phys.Vec3 v = getDeltaMovement();
            setDeltaMovement(v.x, Math.min(v.y, -0.02D), v.z);
        }
    }

    /** Highest solid block's top surface below this entity, searched within 32 blocks. */
    private double findGroundY() {
        net.minecraft.core.BlockPos.MutableBlockPos pos = new net.minecraft.core.BlockPos.MutableBlockPos(
                net.minecraft.util.Mth.floor(getX()), net.minecraft.util.Mth.floor(getY()), net.minecraft.util.Mth.floor(getZ()));
        int minY = level().getMinBuildHeight();
        for (int i = 0; i < 32 && pos.getY() > minY; i++) {
            pos.move(0, -1, 0);
            if (!level().getBlockState(pos).isAir()) {
                return pos.getY() + 1.0D;
            }
        }
        return Double.MIN_VALUE;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 1024.0D;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, this::mainAnim));
    }

    private PlayState mainAnim(AnimationState<Flashbug> state) {
        if (isFlashing()) {
            return state.setAndContinue(FLASH);
        }
        return state.setAndContinue(state.isMoving() ? FLY : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animCache;
    }
}
