package com.carro1001.mhnw.entity;

import com.carro1001.mhnw.MHNWConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
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
 * Great Izuchi: the first combat vertical slice.
 *
 * <h2>Model-to-world frame</h2>
 * The Bedrock geometry in {@code assets/mhnw/geo/entity/great_izuchi.geo.json} is authored in
 * model units (1/16 block), Y up, with the head at -Z and the tail at +Z. Minecraft renders an
 * entity model rotated by {@code 180 - yBodyRot}, so model -Z becomes the direction the entity
 * faces, which is world +Z at yaw 0. The local frame used throughout this class is therefore:
 *
 * <pre>
 *   localForward = -modelZ / 16     localUp = modelY / 16     localLeft = modelX / 16
 * </pre>
 *
 * Model Y = 0 is the ground the creature stands on: its feet solve to Y = -0.20 .. 0.10.
 *
 * <h2>Where the part offsets came from</h2>
 * The raw bone pivots in the geometry are NOT rest positions. The bind pose carries real rotations
 * (neck +30 deg, head -22.78 deg, arms -62.5 deg, legs bent) and the locomotion clips add more.
 * The constants below are the result of running forward kinematics offline, once, over the bind
 * rotations plus the constant term of the walk clip, for the six marker bones the original artists
 * authored (torsoHitbox, headHitbox, clawHitbox, baseTailHitbox, midTailHitbox, tailEndHitbox).
 *
 * <p>Cross-checks that the transform is right: the idle and walk poses agree within 0.1 block, the
 * walk pose solves the two feet symmetrically at +/- 0.47, and the head solves to 3.0 blocks tall,
 * matching the 2.8-tall main hitbox the original MultiHitboxLib profile declared. Part sizes below
 * are the authored values preserved from that same profile.
 *
 * <p>Nothing here evaluates animation at runtime. The server never runs a skeletal-animation
 * engine and never asks a client where a bone is (handoff section 4.2).
 *
 * <p>Known limitation: the sign of the local X axis (which side the claw is on) has only the
 * symmetric-feet cross-check, not visual confirmation. The creature is bilaterally symmetric and
 * the claw is the sole asymmetric volume, so a mirrored claw would shift contact by 0.21 block.
 */
public class GreatIzuchi extends Monster implements GeoEntity {

    // Provisional balance constants. Tuning is a later packet; the behaviour itself is finished.
    public static final double MAX_HEALTH = 120.0D;
    public static final double MOVE_SPEED = 0.28D;
    public static final double FOLLOW_RANGE = 32.0D;
    public static final double KNOCKBACK_RESISTANCE = 0.6D;

    public static final float BODY_WIDTH = 1.6F;
    public static final float BODY_HEIGHT = 2.8F;

    /** Length of {@code animation.great_izuchi.death}: 1.875 s at normal playback. */
    public static final int DEATH_ANIMATION_TICKS = 38;

    /** No attack in progress. */
    public static final byte ATTACK_NONE = 0;
    /** Plays {@code animation.great_izuchi.attack_scratch}. */
    public static final byte ATTACK_SCRATCH = 1;

    private static final EntityDataAccessor<Byte> DATA_ATTACK_ID =
            SynchedEntityData.defineId(GreatIzuchi.class, EntityDataSerializers.BYTE);
    /**
     * Level game time at which the current action began; meaningless when the id is NONE.
     *
     * <p>Deliberately game time rather than {@code tickCount}: a client creates the entity when the
     * spawn packet arrives, so its {@code tickCount} starts at zero and bears no relation to the
     * server's. Game time is shared, which keeps {@link #getAttackAge()} meaningful on both sides.
     */
    private static final EntityDataAccessor<Long> DATA_ATTACK_START =
            SynchedEntityData.defineId(GreatIzuchi.class, EntityDataSerializers.LONG);
    /** Bumped for every new action so that repeating an attack restarts its animation. */
    private static final EntityDataAccessor<Integer> DATA_ACTION_SEQ =
            SynchedEntityData.defineId(GreatIzuchi.class, EntityDataSerializers.INT);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.great_izuchi.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.great_izuchi.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.great_izuchi.run");
    private static final RawAnimation SCRATCH = RawAnimation.begin().thenPlay("animation.great_izuchi.attack_scratch");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.great_izuchi.death");

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);
    private final MonsterPart[] parts;

    /** Server-only: ticks until another attack may start. Never saved, so a reload cools down. */
    public int attackCooldown = 20;

    /** De-duplicates one damage source that enumerates several parts within a single tick. */
    private DamageSource lastDamageSource;
    private int lastDamageTick = -1;

    /** Client-only: the action sequence currently being presented. */
    private int presentedSeq = -1;

    public GreatIzuchi(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.parts = new MonsterPart[] {
                //              name          width  height  left    up      forward
                new MonsterPart(this, "torso",     1.3F, 1.2F, 0.00D, 1.89D,  1.72D),
                new MonsterPart(this, "head",      0.9F, 0.9F, 0.00D, 3.00D,  1.85D),
                new MonsterPart(this, "tail_base", 1.2F, 1.2F, 0.00D, 1.69D, -1.63D),
                new MonsterPart(this, "tail_mid",  1.2F, 1.2F, 0.00D, 1.81D, -3.06D),
                new MonsterPart(this, "tail_end",  1.2F, 1.2F, 0.00D, 1.75D, -4.88D),
        };
        this.xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, MOVE_SPEED)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE)
                .add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE)
                .add(Attributes.ATTACK_DAMAGE, GreatIzuchiCombatGoal.SCRATCH_DAMAGE)
                .add(Attributes.STEP_HEIGHT, 1.0D);
    }

    /** Natural spawning honours the server config so it can be switched off (handoff A11). */
    public static boolean checkSpawnRules(EntityType<GreatIzuchi> type, ServerLevelAccessor level,
                                          MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (spawnType == MobSpawnType.NATURAL && !MHNWConfig.NATURAL_SPAWNING.get()) {
            return false;
        }
        return Monster.checkMonsterSpawnRules(type, level, spawnType, pos, random);
    }

    @Override
    protected void registerGoals() {
        // One combat owner. GreatIzuchiCombatGoal selects, approaches, orients and executes the
        // attack; no other goal moves this mob toward a target or deals its damage (section 4.3).
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new GreatIzuchiCombatGoal(this));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ATTACK_ID, ATTACK_NONE);
        builder.define(DATA_ATTACK_START, 0L);
        builder.define(DATA_ACTION_SEQ, 0);
    }

    // ---------------------------------------------------------------- action state

    public byte getAttackId() {
        return this.entityData.get(DATA_ATTACK_ID);
    }

    public long getAttackStartTime() {
        return this.entityData.get(DATA_ATTACK_START);
    }

    public int getActionSequence() {
        return this.entityData.get(DATA_ACTION_SEQ);
    }

    /** Ticks elapsed in the current action, or -1 when idle. Valid on both sides. */
    public int getAttackAge() {
        return getAttackId() == ATTACK_NONE
                ? -1
                : (int) (level().getGameTime() - getAttackStartTime());
    }

    /** Server: begin an action. Allocates a new sequence so a repeat restarts cleanly. */
    void beginAttack(byte attackId) {
        this.entityData.set(DATA_ATTACK_ID, attackId);
        this.entityData.set(DATA_ATTACK_START, level().getGameTime());
        this.entityData.set(DATA_ACTION_SEQ, getActionSequence() + 1);
    }

    /** Server: end an action. A client that starts tracking after this replays nothing. */
    void endAttack() {
        this.entityData.set(DATA_ATTACK_ID, ATTACK_NONE);
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

    /** Rotates a local (left, up, forward) offset into world space using this monster's body yaw. */
    public Vec3 localToWorld(double left, double up, double forward) {
        double rad = Math.toRadians(this.yBodyRot);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);
        return new Vec3(
                getX() + left * cos - forward * sin,
                getY() + up,
                getZ() + left * sin + forward * cos);
    }

    private void positionParts() {
        for (MonsterPart part : this.parts) {
            part.setOldPosAndRot();
            Vec3 centre = localToWorld(part.localLeft, part.localUp, part.localForward);
            part.setPos(centre.x, centre.y - part.halfHeight(), centre.z);
        }
    }

    /** Place the parts before the first tick, so they never sit at the world origin. */
    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        positionParts();
    }

    @Override
    public void tick() {
        // super.tick() runs aiStep, which is where the body actually moves, so positioning the
        // parts afterwards keeps them on the body within the same tick. Position them exactly
        // ONCE per tick: setOldPosAndRot is what F3+B interpolates part boxes from, and calling
        // it twice would collapse xOld onto x and make correctly placed parts render as though
        // they were lagging the body.
        super.tick();
        positionParts();
        if (!level().isClientSide && this.attackCooldown > 0) {
            this.attackCooldown--;
        }
    }

    /**
     * The death clip is 1.875 s (38 ticks) but vanilla removes a corpse at 20. Hold the body long
     * enough for its authored death to finish, then defer to vanilla removal (handoff A13).
     */
    @Override
    protected void tickDeath() {
        if (this.deathTime < DEATH_ANIMATION_TICKS) {
            this.deathTime++;
            return;
        }
        super.tickDeath();
    }

    /**
     * Single damage entry point for the whole creature.
     *
     * <p>Parts forward here and the root envelope is hit directly. One damage source that
     * enumerates several of those in a single tick (an explosion, a splash effect) must cost
     * exactly one hit, but two different attackers in the same tick must both land. The key is
     * therefore the {@link DamageSource} instance, which vanilla allocates per attack, rather than
     * the tick alone (handoff section 4.1, A03/A06).
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide) {
            if (source == this.lastDamageSource && this.tickCount == this.lastDamageTick) {
                return false;
            }
            this.lastDamageSource = source;
            this.lastDamageTick = this.tickCount;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        // Transient combat state is deliberately not saved. Reloading cancels the action, and the
        // fresh cooldown prevents an instant swing on load (handoff section 4.3 rule 7).
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.attackCooldown = 20;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 16384.0D;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    // ---------------------------------------------------------------- presentation

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::mainAnim));
    }

    /**
     * Presentation follows the synchronized action identity, never the reverse. Because that state
     * is synched entity data rather than a one-shot packet, a client that starts tracking mid-fight
     * receives the live action; once the server clears it, nothing replays.
     *
     * <p>Known limitation: a client that starts tracking part way through an action begins the clip
     * at its first frame rather than seeking to the action age, so for that one client presentation
     * can lead contact by up to the 65-tick clip length. Contact is decided server-side and is
     * unaffected.
     */
    private PlayState mainAnim(AnimationState<GreatIzuchi> state) {
        if (isDeadOrDying()) {
            return state.setAndContinue(DEATH);
        }
        if (getAttackId() != ATTACK_NONE) {
            if (this.presentedSeq != getActionSequence()) {
                this.presentedSeq = getActionSequence();
                state.getController().forceAnimationReset();
            }
            return state.setAndContinue(SCRATCH);
        }
        if (state.isMoving()) {
            // isAggressive() rides the synched mob flags, so it is readable here. getTarget() is
            // server-only state and is always null on a client, which would pin this to WALK.
            return state.setAndContinue(isAggressive() ? RUN : WALK);
        }
        return state.setAndContinue(IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animCache;
    }
}
