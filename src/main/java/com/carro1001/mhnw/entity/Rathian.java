package com.carro1001.mhnw.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
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
 * The nine hurtbox offsets below (seven measured, two interpolated) come from real {@code BoneProbe}
 * measurements read from the maintainer's own play sessions, not an offline solve: an early offline
 * FK attempt and a hand-eyeballed correction on top of it were both confirmed wrong by screenshots,
 * and even a first round of live measurement still needed a second, larger capture and a switch from
 * plain averaging to range-midpoint (see the constructor for why). {@code throat} and {@code tail_tip}
 * are interpolated between measured neighbours to close two gaps the measured positions alone left
 * open, not themselves measurements.
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
 * same reason (no attack clip it can currently trust), rather than leaving a multipart monster with
 * no combat behaviour at all.
 *
 * <h2>Measurement rig for {@code attack_charge_bite_right}</h2>
 * {@code doHurtTarget} now also starts a synced, purely cosmetic countdown that plays
 * {@code attack_charge_bite_right} for its 30-tick length and, while it plays, switches
 * {@code BoneProbe}'s Rathian logging to every tick instead of the idle sampling interval (see
 * {@code MHNWClient.RathianRenderer}). This changes nothing about combat: damage is still the exact
 * same instantaneous {@code doHurtTarget} call, unconditionally, the same tick the goal would have
 * dealt it anyway. It exists purely so a live capture of this one clip (`debugCombat` on, provoke an
 * attack, save {@code logs/latest.log}) is a single play session instead of also needing a code
 * change first -- the next step described above, now that hurtbox placement is measured rather than
 * guessed. Once a real path is baked from that capture, this rig is replaced by an actual
 * {@code AttackProfile}/attack-volume goal the same shape as {@link GreatIzuchiCombatGoal}, not kept
 * alongside it.
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
    private static final RawAnimation BITE = RawAnimation.begin().thenPlay("animation.rathian.attack_charge_bite_right");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.rathian.death");

    /** {@code attack_charge_bite_right} is 1.5s = 30 ticks (see the measurement-rig doc above). */
    private static final int BITE_TICKS = 30;

    private static final EntityDataAccessor<Integer> DATA_BITE_TICKS =
            SynchedEntityData.defineId(Rathian.class, EntityDataSerializers.INT);

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
        // the *Hitbox-named bones' logged left/up/forward values were captured across many idle
        // samples and plugged in directly. This replaced two earlier, both-wrong attempts: an
        // offline FK solve (confirmed wrong by screenshots showing the boxes floating above/in
        // front of the wings) and a hand-eyeballed "nudge it down" correction on top of that.
        //
        // Two rounds of plain averaging still weren't stable (each capture's mean leaned toward
        // whichever slice of the idle sway happened to get sampled more, so successive fixes kept
        // overshooting past each other -- 0.40, then 1.00, for stinger's `up` alone). Switched
        // methodology instead of guessing a third average: for a bone that sways through a real
        // range every idle loop, the *midpoint of the observed min/max* is robust to sampling bias
        // in a way an arithmetic mean isn't, as long as both extremes were seen at least once across
        // enough samples -- which ~25 samples per bone from a single capture reliably does. Every
        // value below is that range midpoint, computed once from one capture, not re-tuned by eye.
        //
        // The neck-head and tail_end-stinger gaps were also large enough (0.87 and 1.1 blocks, once
        // computed from these same measured centres and part widths) to leave real, visible empty
        // space between boxes even at the correct positions -- confirmed by screenshot. `throat` and
        // `tail_tip` are interpolated at each gap's midpoint (not themselves measured) to close that
        // space, the same "more segments," not "fatter boxes," fix used elsewhere in this file.
        //
        // A follow-up screenshot showed gaps still visible and the whole tail chain still reading low
        // against the actual rendered mesh. Widths for the torso/neck/throat/tail chain were set to
        // the exact distance to each neighbour (so consecutive boxes meet exactly, "start where the
        // next ends" -- some, like stinger and tail_tip, needed to shrink, since the interpolated
        // midpoints had made them overlap instead of gap). `head` keeps its original size: unlike the
        // connecting sections, it's a genuinely bulky part of the mesh, so sizing it down to the bare
        // touching-minimum would make it read too small.
        //
        // A +0.35 flat raise was tried on top of the range-midpoint `up` values for the tail chain,
        // on the theory that one screenshot's rendered pose sat higher than the statistical centre.
        // A further screenshot still showed the tail chain reading low, and closer analysis of
        // stinger's own full observed range (roughly -0.48 to 1.08, over a full block of real idle
        // sway) showed the +0.35 push had moved the *centre* without addressing the actual problem:
        // no single centre value reads correctly against every frame of a block-wide sway, whichever
        // moment a screenshot happens to catch. Reverted to the plain range-midpoint centre (the
        // mathematically correct "centred on the bone" value, per feedback asking for exactly that),
        // and instead widened `height` for the parts with the largest observed sway (tail_base
        // through stinger) so the box actually spans the swing rather than a single instant of it.
        //
        // Sizes otherwise still borrow from the archived hitbox profile (same caveat as before, not
        // Rathian-specific), and `left` is set to 0 for every part: the measured samples oscillate
        // both sides of zero as the idle animation sways, with no consistent bias either way.
        //
        // Refreshed against a capture of the actual *Hitbox locator bones (torsoHitbox, neckHitbox,
        // headHitbox, baseTailHitbox, midTailHitbox, tailEndHitbox, stingerHitbox) -- dedicated
        // placement bones the model already ships, not mesh bones being inferred from. The first such
        // capture read as near-zero-variance and was plugged in directly; a follow-up screenshot
        // showed the tail chain reading *worse*, moved further down than before, which is exactly
        // what happened: a second capture, taken during the part of the idle loop that actually sways,
        // showed these locator bones swing just as much as any mesh bone (`stingerHitbox` alone spans
        // roughly -0.46 to 1.24, a 1.7-block range) -- the first capture had simply caught a narrow,
        // low slice of that range and been mistaken for a stable value, the exact mistake range-
        // midpoint was adopted to avoid in the first place, just one level down the bone hierarchy.
        //
        // Fixed by combining both captures' observed min/max per bone and taking the midpoint of the
        // combined range, same range-midpoint methodology as before, just applied to the locator bones
        // instead of the mesh bones. `throat`/`tail_tip` recomputed as the midpoint of their own
        // updated neighbours; widths recomputed the same exact-touch-distance way as before.
        this.parts = new MonsterPart[] {
                //              name          width height  left  up      forward
                new MonsterPart(this, "torso",  2.4F, 2.3F, 0.00D, 2.34D,  2.15D),
                new MonsterPart(this, "neck",   2.4F, 2.3F, 0.00D, 1.80D,  4.51D),
                new MonsterPart(this, "throat", 1.4F, 1.4F, 0.00D, 1.71D,  5.91D),
                new MonsterPart(this, "head",   2.0F, 2.0F, 0.00D, 1.61D,  7.30D),
                new MonsterPart(this, "tail_base", 2.3F, 1.9F, 0.00D, 2.12D, -1.56D),
                new MonsterPart(this, "tail_mid",  2.3F, 1.7F, 0.00D, 1.64D, -3.83D),
                new MonsterPart(this, "tail_end",  2.2F, 2.0F, 0.00D, 1.18D, -5.99D),
                new MonsterPart(this, "tail_tip",  1.55F, 1.9F, 0.00D, 0.79D, -7.49D),
                new MonsterPart(this, "stinger",   1.55F, 1.9F, 0.00D, 0.39D, -8.98D),
        };
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BITE_TICKS, 0);
    }

    /** Whether the cosmetic bite-clip countdown is currently running; see the class doc for what
     * this does and, deliberately, does not yet do. */
    public boolean isBiting() {
        return this.entityData.get(DATA_BITE_TICKS) > 0;
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
        if (!level().isClientSide) {
            int ticks = this.entityData.get(DATA_BITE_TICKS);
            if (ticks > 0) {
                this.entityData.set(DATA_BITE_TICKS, ticks - 1);
            }
        }
    }

    /**
     * Starts the cosmetic bite-clip countdown; see the class doc's "Measurement rig" section. This
     * does not change combat at all -- {@code super.doHurtTarget} still deals damage the exact same
     * instantaneous way it already did.
     */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean result = super.doHurtTarget(target);
        if (!level().isClientSide) {
            this.entityData.set(DATA_BITE_TICKS, BITE_TICKS);
        }
        return result;
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
        if (isBiting()) {
            return state.setAndContinue(BITE);
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
