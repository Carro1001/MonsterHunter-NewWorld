package com.carro1001.mhnw.entity;

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
 * Rathian, ground-only, P4's first large-wyvern slice (handoff section 5, P4). Flight is
 * deliberately not implemented yet: the handoff's own text for this species defers it within its
 * own packet ("bounded flight later in its packet"), and this class only covers the ground half.
 *
 * <h2>What is genuinely measured, and what is not</h2>
 * The seven hurtbox offsets below come from solving the geometry's {@code idle_normal} pose
 * offline, the same forward-kinematics technique used for Great Izuchi, and for the same reason it
 * is trustworthy here: the idle pose's rotation channels are plain constants and single sine waves,
 * the class of channel that solved Great Izuchi's tail to within 0.05 block of the runtime-measured
 * truth. This has not itself been checked against a live client, unlike Great Izuchi's numbers,
 * which were; treat these as a good first estimate pending that check, not as calibrated.
 *
 * <p>The attack clips ({@code attack_charge_bite_left/right}, {@code attack_tailwhip}, the fireball
 * clips, and so on) are deliberately NOT wired to any custom attack volume. Investigating why
 * offline solving diverged for Great Izuchi's claw led to a real, fixed bug in the FK tooling
 * itself (per-keyframe MoLang was being evaluated at the sampled query time instead of each
 * keyframe's own declared time), but fixing that bug did not close the gap against the
 * runtime-measured claw path, so something else about actively-animated, heavily keyframed chains
 * still makes offline solving untrustworthy for motion, not just for this bug. Rathian's attack
 * clips use exactly that pattern (four real keyframes per bone, each carrying its own compound
 * MoLang formula) on the Chest/Neck/Head chain, unlike the idle pose's simple constants, so the
 * same caution applies until one of those clips can be measured live with the bone probe the way
 * Great Izuchi's attacks were. See {@code docs/DEFERRED.md}.
 *
 * <p>Until then, this fights with ordinary vanilla {@link MeleeAttackGoal}, damage through
 * {@code Mob.doHurtTarget}, no custom timeline: the same honest interim {@link Izuchi} uses for the
 * same reason (no attack clip it can currently trust), rather than leaving a seven-part multipart
 * monster with no combat behaviour at all.
 */
public class Rathian extends Monster implements GeoEntity {

    // Provisional balance constants, not yet tuned.
    public static final double MAX_HEALTH = 90.0D;
    public static final double MOVE_SPEED = 0.24D;
    public static final double ATTACK_DAMAGE = 6.0D;
    public static final double FOLLOW_RANGE = 28.0D;

    public static final float BODY_WIDTH = 2.2F;
    public static final float BODY_HEIGHT = 3.6F;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.rathian.idle_normal");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.rathian.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.rathian.run");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.rathian.death");

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);
    private final MonsterPart[] parts;

    /**
     * De-duplicates one damage source that enumerates several parts within a single tick, the same
     * guard {@link GreatIzuchi} carries; see its own copy of this field for why vanilla's own
     * invulnerability window is not enough on its own. Duplicated rather than shared through a base
     * class for now: a genuine extraction candidate once enough of {@code MultipartMonster}'s
     * boilerplate is common to two species to be worth the risk of restructuring already-shipped,
     * player-visible code without the ability to test the result interactively. See
     * {@code docs/DEFERRED.md}.
     */
    private DamageSource lastDamageSource;
    private int lastDamageTick = -1;

    public Rathian(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        // These offsets are real BoneProbe measurements (see docs/TEST_PLAN.md), not offline-solved
        // or eyeballed: the maintainer ran with debugCombat on and stood near a live Rathian, and
        // the *Hitbox-named bones' logged left/up/forward values were averaged across several idle
        // samples and plugged in directly. This replaced two earlier, both-wrong attempts: an
        // offline FK solve (confirmed wrong by screenshots showing the boxes floating above/in
        // front of the wings) and a hand-eyeballed "nudge it down" correction on top of that
        // (confirmed by these same measurements to still be off by roughly a full block on `up`
        // for the neck/head/tail chain -- guessing a direction was right, but not nearly enough).
        //
        // Re-averaged once more from a second, larger capture: the first pass's average leaned on
        // the low end of the idle sway for the tail/neck/head chain (the idle animation swings the
        // tail through a real range, and that range is not symmetric around the first small sample),
        // which read as "boxes too low" against a screenshot from later in the sway. This second
        // pass averages twelve samples per bone from a fresh capture instead of a handful, landing
        // closer to the middle of the observed range rather than near one edge of it -- most visible
        // on stinger (0.40 -> 1.00) and tail_end (1.10 -> 1.43), the parts furthest down the chain
        // and so the most exaggerated by any bias in which slice of the sway got sampled.
        //
        // Sizes are still borrowed from the archived hitbox profile (same caveat as before, not
        // Rathian-specific), and `left` is set to 0 for every part: the measured samples oscillate
        // both sides of zero as the idle animation sways, with no consistent bias either way.
        this.parts = new MonsterPart[] {
                //              name          width height  left  up      forward
                new MonsterPart(this, "torso",  2.1F, 2.1F, 0.00D, 2.30D,  2.15D),
                new MonsterPart(this, "neck",   1.75F, 1.75F, 0.00D, 1.81D,  4.52D),
                new MonsterPart(this, "head",   2.0F, 2.0F, 0.00D, 1.61D,  7.25D),
                new MonsterPart(this, "tail_base", 1.75F, 1.6F, 0.00D, 2.03D, -1.61D),
                new MonsterPart(this, "tail_mid",  1.6F, 1.3F, 0.00D, 1.68D, -3.93D),
                new MonsterPart(this, "tail_end",  2.0F, 2.0F, 0.00D, 1.43D, -6.17D),
                new MonsterPart(this, "stinger",   1.75F, 1.6F, 0.00D, 1.00D, -9.31D),
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
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
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

    /** Same convention as {@link GreatIzuchi}: model -Z is the direction the entity faces. */
    public Vec3 localToWorld(double left, double up, double forward) {
        double rad = Math.toRadians(this.yBodyRot);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);
        return new Vec3(
                getX() + left * cos - forward * sin,
                getY() + up,
                getZ() + left * sin + forward * cos);
    }

    /**
     * Vanilla parts have no spawn packet, so a client would allocate their ids independently and
     * disagree with the server about which id is which part; numbering them as successors of the
     * parent id is how {@code EnderDragon} solves the same problem (MC-158205), reused here exactly
     * as it already is for {@link GreatIzuchi}.
     */
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

    /**
     * super.tick() runs aiStep(), which is where the body actually moves (and, further inside
     * LivingEntity.tick() after aiStep() returns, where yBodyRot itself gets smoothed by
     * tickHeadTurn), so positioning the parts here, after super.tick() fully returns, is the one
     * point in the tick that reflects the final position and rotation. Do this exactly ONCE per
     * tick: setOldPosAndRot() is what interpolation renders from, and an earlier aiStep() override
     * calling positionParts() a second time made that first call's result get immediately
     * overwritten while corrupting the interpolation's "old" value with an intermediate,
     * never-rendered position (same bug fixed for Great Izuchi; see that class's tick() comment).
     */
    @Override
    public void tick() {
        super.tick();
        positionParts();
    }

    /**
     * Single damage entry point for the whole creature, same contract as {@link GreatIzuchi#hurt},
     * including the same correction: the source-identity guard alone stops a repeat from the exact
     * same source object, but does nothing for two genuinely different attackers, since vanilla's
     * own invulnerability check underneath compares only raw damage amount to the previous hit
     * ({@code amount <= this.lastHurt}), not source identity. Without resetting
     * {@code invulnerableTime} whenever the incoming source is a new one, a second distinct
     * attacker in the same tick dealing equal or smaller damage would have been silently dropped by
     * vanilla's own logic regardless of this override, the actual A06 fairness case.
     */
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

    /**
     * Vanilla's default culling box is the hitbox inflated by a flat 0.5 block, correct for a
     * normal mob but not one whose visible model reaches well outside its own hitbox: the neck and
     * head alone reach nearly 7.5 blocks forward, the tail 6.2 back, and the wings spread several
     * blocks up. Without this, the game stops rendering the whole entity as soon as that small root
     * box leaves the camera frustum, which reads as the head vanishing mid-turn well before the
     * body is actually off screen. Same hook vanilla's own large/long entities use for the same
     * reason; see {@link GreatIzuchi#getBoundingBoxForCulling} for the smaller-scale version of it.
     */
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

    private PlayState mainAnim(AnimationState<Rathian> state) {
        if (isDeadOrDying()) {
            return state.setAndContinue(DEATH);
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
