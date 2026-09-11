package com.carro1001.mhnw.entity;

import com.carro1001.mhnw.MHNWConfig;
import com.carro1001.mhnw.registry.ModEntities;
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
import net.minecraft.world.entity.monster.AbstractIllager;
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
 * They are <em>measured</em>, by logging where GeckoLib actually places each bone at runtime and
 * reading the numbers off in this local frame. See {@code client/BoneProbe}, which produced them.
 *
 * <p>Two earlier approaches were wrong and are recorded here so nobody repeats them. First, the
 * {@code *Hitbox} marker bones in the geometry: authored for MultiHitboxLib, which positioned them
 * by a different rule, they do not sit on the body under this renderer. Second, solving the
 * geometry offline by walking the bone chain: that agrees with the runtime to within 0.05 block
 * for the tail, but diverges badly for chains carrying large bind rotations, by 0.8 block up and
 * 1.4 forward at the head, and about 1.4 across at the hand. A brute force over all six rotation
 * orders and every sign convention found none that fixes the head while keeping the torso and feet
 * right, so the divergence is not a simple axis convention; the likely cause is that an animated
 * position channel applies in bone-local space rather than parent space. It was not worth chasing
 * once the runtime could simply be asked.
 *
 * <p>The probe is a development measuring tape only. Nothing here evaluates animation at runtime,
 * and no gameplay decision reads a client bone position: the numbers below are constants
 * (handoff section 4.2).
 *
 * <p>Known limitations: a static local offset cannot track a tail that swings sideways during
 * locomotion, so the tail boxes approximate a swept envelope rather than the instantaneous tail.
 */
public class GreatIzuchi extends Monster implements GeoEntity {

    // Provisional balance constants. Tuning is a later packet; the behaviour itself is finished.
    // ponytail: 40 HP is a deliberately low testing value so a slice can be killed quickly during
    // development. Raise toward 120 once the combat slice is signed off.
    public static final double MAX_HEALTH = 40.0D;
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
    /** Plays {@code animation.great_izuchi.attack_tailswipe}. */
    public static final byte ATTACK_TAIL_SWIPE = 2;
    /** Plays {@code animation.great_izuchi.attack_tailslam}. */
    public static final byte ATTACK_TAIL_SLAM = 3;

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
    private static final RawAnimation TAIL_SWIPE = RawAnimation.begin().thenPlay("animation.great_izuchi.attack_tailswipe");
    private static final RawAnimation TAIL_SLAM = RawAnimation.begin().thenPlay("animation.great_izuchi.attack_tailslam");
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

    /** Server-only source of truth for the action sequence. Never read back from synced data. */
    private int actionSequenceCounter;

    /**
     * Body yaw the combat goal wants held, or null when nothing is committed.
     *
     * <p>Applied in {@link #tick()} after {@code super.tick()} rather than inside the goal,
     * because the vanilla body-rotation control runs during the AI step and would otherwise
     * overwrite it. Whatever is applied here is what the part offsets and the claw volume are
     * rotated by, so aim, hurtboxes and attack geometry cannot disagree.
     */
    private Float committedBodyYaw;

    void setCommittedBodyYaw(Float yaw) {
        this.committedBodyYaw = yaw;
    }

    Float getCommittedBodyYaw() {
        return this.committedBodyYaw;
    }

    public GreatIzuchi(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        // Four tail segments rather than three: the tail is 4.8 blocks long and tapers, so three
        // boxes either left a gap or made the tip box far bigger than the tip. Each segment
        // overlaps its neighbour slightly so there is no seam to slip a hit through. The hip gap
        // between the torso box and tail_1 is covered by the root envelope, which is also hittable.
        // Every box overlaps its neighbour along the body axis. A seam between two boxes is a
        // strip of the creature that cannot be hit, which reads in game as the attack randomly
        // failing; the spans below are written out so the overlaps stay checkable by eye.
        //
        //   torso   0.00 ..  1.60       tail_1  -0.45 .. -1.85
        //   neck    1.45 ..  2.55       tail_2  -1.70 .. -3.00
        //   head    2.40 ..  3.70       tail_3  -2.90 .. -4.10
        //                               tail_4  -4.00 .. -5.10
        //
        // The hips, between the torso box and tail_1, are covered by the root envelope, which is
        // hittable in its own right.
        this.parts = new MonsterPart[] {
                //              name          width height  left    up      forward
                new MonsterPart(this, "torso",  1.6F, 1.9F, 0.00D, 2.15D,  0.80D),
                new MonsterPart(this, "neck",   1.1F, 1.3F, 0.00D, 2.25D,  2.00D),
                new MonsterPart(this, "head",   1.3F, 1.2F, 0.00D, 2.35D,  3.05D),
                new MonsterPart(this, "tail_1", 1.4F, 1.3F, 0.00D, 2.15D, -1.15D),
                new MonsterPart(this, "tail_2", 1.3F, 1.2F, 0.00D, 2.10D, -2.35D),
                new MonsterPart(this, "tail_3", 1.2F, 1.1F, 0.00D, 2.00D, -3.50D),
                new MonsterPart(this, "tail_4", 1.1F, 1.2F, 0.00D, 1.85D, -4.55D),
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

    /** How many escort Izuchi spawn alongside a Great Izuchi, inclusive both ends. */
    private static final int MIN_ESCORTS = 1;
    private static final int MAX_ESCORTS = 4;

    /** A Great Izuchi is a pack leader: it never appears alone in the wild (section 2, pack mechanic). */
    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty,
            MobSpawnType spawnType, net.minecraft.world.entity.SpawnGroupData groupData) {
        net.minecraft.world.entity.SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, groupData);
        if (spawnType != MobSpawnType.NATURAL && spawnType != MobSpawnType.SPAWNER) {
            // A summoned or otherwise deliberately placed Great Izuchi doesn't drag escorts along;
            // only genuine wild spawns get the pack.
            return result;
        }
        int count = MIN_ESCORTS + this.random.nextInt(MAX_ESCORTS - MIN_ESCORTS + 1);
        for (int i = 0; i < count; i++) {
            Izuchi izuchi = ModEntities.IZUCHI.get().create(level.getLevel());
            if (izuchi == null) {
                continue;
            }
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            double dist = 2.0D + this.random.nextDouble() * 3.0D;
            double x = getX() + Math.cos(angle) * dist;
            double z = getZ() + Math.sin(angle) * dist;
            izuchi.moveTo(x, getY(), z, this.random.nextFloat() * 360.0F, 0.0F);
            izuchi.finalizeSpawn(level, difficulty, MobSpawnType.MOB_SUMMONED, null);
            level.addFreshEntity(izuchi);
        }
        return result;
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
        // Pillagers make the attack observable from outside the fight, which a player being hit
        // cannot easily do. Also reasonable flavour: a large monster does not care who you are.
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractIllager.class, true));
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

    /**
     * Server: begin an action. Allocates a new sequence so a repeat restarts cleanly.
     *
     * <p>The counter is a plain server-side field rather than a read-modify-write of the synced
     * value. Reading the synced sequence back to increment it made every attack report sequence 1
     * in testing, and since the client restarts the animation only when the sequence changes, a
     * stuck sequence means a repeated attack deals its damage without ever replaying its swing:
     * the monster appears to stop attacking while the log shows hits landing.
     */
    void beginAttack(byte attackId) {
        this.actionSequenceCounter++;
        this.entityData.set(DATA_ATTACK_ID, attackId);
        this.entityData.set(DATA_ATTACK_START, level().getGameTime());
        this.entityData.set(DATA_ACTION_SEQ, this.actionSequenceCounter);
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

    /**
     * Parts have no spawn packet of their own, so a client allocates their entity ids from its own
     * counter and would disagree with the server about which id is which part. A player clicking a
     * hurtbox sends that client-side id, the server resolves it to nothing, and the hit silently
     * vanishes: the parts render but cannot be attacked.
     *
     * <p>Vanilla solves this by numbering parts as successors of the parent id, which both sides
     * derive identically once the parent id arrives in the spawn packet. See EnderDragon.setId and
     * MC-158205. The parts are constructed in this constructor, immediately after the parent, so
     * the ids they reserve from the shared counter are exactly the ones claimed here.
     */
    @Override
    public void setId(int id) {
        super.setId(id);
        for (int i = 0; i < this.parts.length; i++) {
            this.parts[i].setId(id + i + 1);
        }
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
        if (!level().isClientSide && this.committedBodyYaw != null) {
            this.yBodyRot = this.committedBodyYaw;
            setYRot(this.committedBodyYaw);
        }
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
     *
     * <h2>A correction to that claim</h2>
     * The early-return guard below only ever fires for the literal same source object, so on its
     * own it does nothing for two genuinely different attackers; the fairness half of that claim
     * was not actually true until the {@code invulnerableTime} reset was added. Vanilla's own
     * invulnerability check compares only the raw damage amount to the previous hit
     * ({@code amount <= this.lastHurt} in {@code LivingEntity.hurt}), not which source dealt it, so
     * without the reset, a second distinct attacker in the same tick whose damage happened to be
     * equal to or smaller than the first would have been silently dropped by vanilla's own logic,
     * underneath this override, regardless of the source-identity guard. Resetting
     * {@code invulnerableTime} before delegating to a genuinely new source forces vanilla to treat
     * it as a fresh hit. This was caught by a test that made the second hit strictly larger than
     * the first, which passes either way and therefore never actually exercised the gap; see the
     * corrected version in {@code MHNWGameTests}.
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

    /**
     * Vanilla's default culling box is the collision hitbox inflated by a flat 0.5 block
     * ({@code EntityRenderer.shouldRender}), which is correct for a normal mob but not for one
     * whose visible model reaches well outside its own hitbox: the tail alone reaches 4.9 blocks
     * behind the 1.6-wide root box. Without this, the game stops rendering the whole entity the
     * moment that small root box leaves the camera frustum, which reads as the head or tail
     * abruptly vanishing while clearly still on screen, well before the body itself is off camera.
     * This is the same hook vanilla's own long/large entities use for the same reason.
     */
    @Override
    public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(6.0D, 4.0D, 6.0D);
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
        byte attack = getAttackId();
        if (attack != ATTACK_NONE) {
            if (this.presentedSeq != getActionSequence()) {
                this.presentedSeq = getActionSequence();
                state.getController().forceAnimationReset();
            }
            return state.setAndContinue(switch (attack) {
                case ATTACK_TAIL_SWIPE -> TAIL_SWIPE;
                case ATTACK_TAIL_SLAM -> TAIL_SLAM;
                default -> SCRATCH;
            });
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
