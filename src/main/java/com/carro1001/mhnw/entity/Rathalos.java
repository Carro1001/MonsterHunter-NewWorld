package com.carro1001.mhnw.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Rathalos, ground-only, the second large wyvern (handoff section 5, P4). Same shape as
 * {@link Rathian} for the same reasons, with one difference worth naming: Rathalos is genuinely
 * a flying wyvern, more so than Rathian, so a ground-only slice undersells it more than Rathian's
 * did. It is still the correct scope for this pass: flight for this family is explicitly deferred
 * within Rathian's own packet in the handoff's text, and nothing about that changes for Rathalos.
 *
 * <h2>This species' attack clips are worse off than Rathian's</h2>
 * Rathian's attacks were merely untrustworthy to solve offline; every bone they reference actually
 * exists in the geometry. Rathalos's four melee attack clips ({@code attack_claw_scratch},
 * {@code attack_bite}, {@code attack_airsweep}, {@code attack_air_fireball}) each reference 14 to
 * 17 bone names (wing membrane and talon bones, mostly) that do not exist anywhere in this
 * species' own geometry at all, exactly the specific example the handoff's own audit named
 * (section 6.1, giving {@code rightwinglimb1} and {@code leftFoot2} as instances). That is not a
 * measurement problem the runtime bone probe can solve the way Great Izuchi's claw was fixed: there
 * is no bone to measure. It needs an actual retargeting pass against the real skeleton in a model
 * editor, or a new authored clip, before any attack presentation is possible. See
 * {@code docs/DEFERRED.md}.
 *
 * <p>Until then: ordinary vanilla {@link MeleeAttackGoal}, same interim {@link Izuchi} still uses
 * for the same reason (Rathian has since moved off this to {@link RathianCombatGoal}, but only
 * because {@code attack_charge_bite_right} exists and animates real bones in Rathian's own model;
 * that option isn't available here until this species' own attack clips are retargeted or replaced).
 *
 * <h2>Hurtboxes</h2>
 * Offline-solved from {@code idle_normal}, same trustworthy channel category (plain constants and
 * single sine waves) that validated well for Great Izuchi's tail; not yet checked live. Part sizes
 * come from the archived hitbox profile that is actually, correctly, named and pathed for this
 * species (unlike Rathian's, which pointed at the wrong file), so these are the real authored
 * values, not a borrowed approximation.
 */
public class Rathalos extends Monster implements GeoEntity, Roarable {

    // Provisional balance constants, not yet tuned.
    public static final double MAX_HEALTH = 85.0D;
    public static final double MOVE_SPEED = 0.24D;
    public static final double ATTACK_DAMAGE = 6.0D;
    public static final double FOLLOW_RANGE = 28.0D;

    public static final float BODY_WIDTH = 2.2F;
    public static final float BODY_HEIGHT = 3.6F;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.rathalos.idle_normal");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.rathalos.walk_normal");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.rathalos.walk_aggro");
    private static final RawAnimation ROAR = RawAnimation.begin().thenPlay("animation.rathalos.roar");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.rathalos.death");

    /** {@code animation.rathalos.roar} is 5s; see {@code docs/ANIMATION_MANIFEST.json}. */
    private static final int ROAR_TICKS = 100;

    private static final EntityDataAccessor<Integer> DATA_ROAR_TICKS =
            SynchedEntityData.defineId(Rathalos.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);
    private final MonsterPart[] parts;

    /** Same guard and the same caveat as {@link Rathian#hurt}; see that copy's doc comment. */
    private DamageSource lastDamageSource;
    private int lastDamageTick = -1;

    public Rathalos(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        // Rathian's own real BoneProbe measurements (see that class's constructor comment), reused
        // here as-is on the maintainer's own call ("rathalos and rathian are almost identical, you
        // can use the numbers for one on the other"): these two share the same base skeleton layout
        // (BoneProbe.WYVERN_BONES lists the same bone names for both) and the same idle-pose
        // proportions closely enough that a real measurement for one is a far better estimate for
        // the other than another offline solve or hand-eyeballed guess would be. Still a proxy, not
        // a Rathalos-specific measurement -- if a live session ever shows a specific part
        // meaningfully off, that part's own measured value should win over this borrowed one.
        //
        // Switched from plain averaging to range-midpoint (see Rathian's constructor comment for
        // why), and added `throat` between neck and head to close a real, measured gap -- no
        // stinger chain here, so no tail_tip analog is needed.
        //
        // Widths re-sized to the exact distance to each neighbour (touching, not gapping or
        // overlapping), matching Rathian's identical follow-up fix (see that class's constructor
        // comment) -- reused here for the same reason as the original numbers: same skeleton, same
        // proportions, no Rathalos-specific capture yet.
        //
        // A flat +0.35 raise on the tail chain was tried and reverted in Rathian (see that class's
        // constructor comment for why: no single centre value reads right against a full block of
        // real idle sway, whichever moment a screenshot catches). Back to plain range-midpoint
        // centres here too, with `height` widened on the tail chain instead so the box spans the
        // actual swing.
        //
        // Refreshed again to match Rathian's latest numbers, now sourced from the model's own
        // *Hitbox locator bones, range-midpointed across two captures after the first capture's
        // narrow slice of their real sway made things worse (see that class's constructor comment)
        // -- still a borrowed proxy, not a Rathalos-specific capture.
        this.parts = new MonsterPart[] {
                //              name          width height  left  up      forward
                new MonsterPart(this, "torso",  2.4F, 2.3F, 0.00D, 2.34D,  2.15D),
                new MonsterPart(this, "neck",   2.4F, 2.3F, 0.00D, 1.80D,  4.51D),
                new MonsterPart(this, "throat", 1.4F, 1.4F, 0.00D, 1.71D,  5.91D),
                new MonsterPart(this, "head",   2.0F, 2.0F, 0.00D, 1.61D,  7.30D),
                new MonsterPart(this, "tail_base", 2.3F, 1.9F, 0.00D, 2.12D, -1.56D),
                new MonsterPart(this, "tail_mid",  2.3F, 1.7F, 0.00D, 1.64D, -3.83D),
                new MonsterPart(this, "tail_end",  2.2F, 2.0F, 0.00D, 1.18D, -5.99D),
        };
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, MOVE_SPEED)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE)
                .add(Attributes.STEP_HEIGHT, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Sits above the melee goal deliberately (see RoarGoal's own doc): the opening roar must
        // freeze the fight, not play underneath an attack goal that keeps moving/swinging.
        this.goalSelector.addGoal(1, new RoarGoal<>(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        // Same reasoning as Great Izuchi's/Rathian's identical goal: pillagers make the attack
        // observable from outside the fight, and a large monster does not care who you are.
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractIllager.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ROAR_TICKS, 0);
    }

    // ---------------------------------------------------------------- roar (Roarable)

    @Override
    public int getRoarTicks() {
        return this.entityData.get(DATA_ROAR_TICKS);
    }

    @Override
    public void setRoarTicks(int ticks) {
        this.entityData.set(DATA_ROAR_TICKS, ticks);
    }

    @Override
    public int roarDurationTicks() {
        return ROAR_TICKS;
    }

    public boolean isRoaring() {
        return getRoarTicks() > 0;
    }

    /** Server-only AI state, not synced (see {@link Roarable#hasRoaredThisEngagement}). */
    private boolean roaredThisEngagement;

    @Override
    public boolean hasRoaredThisEngagement() {
        return this.roaredThisEngagement;
    }

    @Override
    public void setRoaredThisEngagement(boolean roared) {
        this.roaredThisEngagement = roared;
    }

    // ---------------------------------------------------------------- multipart

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return this.parts;
    }

    public MonsterPart[] monsterParts() {
        return this.parts;
    }

    public MonsterPart part(String name) {
        for (MonsterPart p : this.parts) {
            if (p.partName.equals(name)) {
                return p;
            }
        }
        throw new IllegalArgumentException("no such part: " + name);
    }

    public Vec3 localToWorld(double left, double up, double forward) {
        double rad = Math.toRadians(this.yBodyRot);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);
        return new Vec3(
                getX() + left * cos - forward * sin,
                getY() + up,
                getZ() + left * sin + forward * cos);
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        for (int i = 0; i < this.parts.length; i++) {
            this.parts[i].setId(id + i + 1);
        }
    }

    private void positionParts() {
        for (MonsterPart part : this.parts) {
            part.setOldPosAndRot();
            Vec3 centre = localToWorld(part.localLeft, part.localUp, part.localForward);
            part.setPos(centre.x, centre.y - part.halfHeight(), centre.z);
        }
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        positionParts();
    }

    /** Position parts exactly ONCE per tick, after super.tick() fully returns; see Rathian's
     * identical tick() comment for why an aiStep()-level call as well was a real bug, not just
     * wasted work. */
    @Override
    public void tick() {
        super.tick();
        positionParts();
    }

    /** Same contract and the same fairness correction as {@link Rathian#hurt}. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide) {
            if (source == this.lastDamageSource && this.tickCount == this.lastDamageTick) {
                return false;
            }
            if (source != this.lastDamageSource) {
                this.invulnerableTime = 0;
            }
            this.lastDamageSource = source;
            this.lastDamageTick = this.tickCount;
        }
        return super.hurt(source, amount);
    }

    // ---------------------------------------------------------------- presentation

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 16384.0D;
    }

    /** Same fix, same reason, as {@link Rathian#getBoundingBoxForCulling}. */
    @Override
    public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(9.0D, 5.0D, 9.0D);
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::mainAnim));
    }

    private PlayState mainAnim(AnimationState<Rathalos> state) {
        if (isDeadOrDying()) {
            return state.setAndContinue(DEATH);
        }
        if (isRoaring()) {
            return state.setAndContinue(ROAR);
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
