package com.carro1001.mhnw.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
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
 * The small Izuchi: P4's "simple independent movement/targeting" species (handoff section 5, P4),
 * a genuinely hostile ground monster, unlike any of the P3 endemic life.
 *
 * <p>A single fitted hurtbox, not multipart: section 4.2 reserves that machinery for large
 * monsters, and this is explicitly the small one. Combat is ordinary vanilla {@link MeleeAttackGoal}
 * dealing damage through {@code Mob.doHurtTarget}, not {@link GreatIzuchiCombatGoal}'s attack
 * timeline: there is no attack clip to time a swing against (see below), and a creature this size
 * does not need one to be a working "simple independent" monster.
 *
 * <h2>Why there is no attack or death animation</h2>
 * The preserved master-branch asset for this species has exactly four clips: idle, sleep, walk,
 * run. No attack, no death. The handoff's own audit (section 6.1) found a candidate attack/death
 * set on the archived {@code brain} branch, but flagged those clips as referencing bone names
 * ({@code left_shoulder}, {@code left_ankle}, {@code mane}, {@code tailblade}) absent even from
 * their own paired geometry, meaning they need an actual retargeting pass, not just code, before
 * they play correctly, and P4 itself says to obtain an authored clip or explicit approval for a
 * temporary presentation if that retargeting is not attempted, not to fabricate a swing animation
 * to fill the gap. That decision has not been made, so this species has no attack or death
 * presentation yet: it fights using the walk/run clips already in motion, and dies using vanilla's
 * ordinary corpse flop, which is the correct default in the absence of an authored death clip
 * (compare {@link GreatIzuchi}, which explicitly cancels that same vanilla flop, but only because
 * it has an authored clip that would otherwise fight it).
 *
 * <h2>Sleep</h2>
 * The fourth clip, genuinely used: {@link IzuchiSleepGoal} makes it nap periodically when nothing
 * is targeting it, matching the roster's own description of a small ground monster with
 * independent behaviour rather than pack coordination (that comes later, with the great Izuchi's
 * kin, not this species).
 */
public class Izuchi extends Monster implements GeoEntity {

    // Provisional balance constants, not yet tuned. Deliberately weaker than Great Izuchi.
    public static final double MAX_HEALTH = 20.0D;
    public static final double MOVE_SPEED = 0.26D;
    public static final double ATTACK_DAMAGE = 3.0D;
    public static final double FOLLOW_RANGE = 20.0D;

    public static final float BODY_WIDTH = 0.9F;
    public static final float BODY_HEIGHT = 1.1F;

    private static final EntityDataAccessor<Boolean> DATA_SLEEPING =
            SynchedEntityData.defineId(Izuchi.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.izuchi.idle");
    private static final RawAnimation SLEEP = RawAnimation.begin().thenLoop("animation.izuchi.sleep");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.izuchi.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.izuchi.run");

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public Izuchi(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, MOVE_SPEED)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new IzuchiSleepGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SLEEPING, false);
    }

    public boolean isSleeping() {
        return this.entityData.get(DATA_SLEEPING);
    }

    void setSleeping(boolean sleeping) {
        this.entityData.set(DATA_SLEEPING, sleeping);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0D;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::mainAnim));
    }

    private PlayState mainAnim(AnimationState<Izuchi> state) {
        if (isSleeping()) {
            return state.setAndContinue(SLEEP);
        }
        if (state.isMoving()) {
            return state.setAndContinue(isAggressive() ? RUN : WALK);
        }
        return state.setAndContinue(IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animCache;
    }
}
