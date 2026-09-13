package com.carro1001.mhnw.entity;

import com.carro1001.mhnw.animation.ServerTimedAnimationController;
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
public class GreatIzuchi extends Monster implements GeoEntity, Roarable {

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
    private static final RawAnimation ROAR = RawAnimation.begin().thenPlay("animation.great_izuchi.roar");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.great_izuchi.death");

    /** {@code animation.great_izuchi.roar} is 3.5417s; see {@code docs/ANIMATION_MANIFEST.json}. */
    private static final int ROAR_TICKS = 71;

    /**
     * Ticks the animation controller spends blending into a newly set clip. Named rather than
     * inlined because {@link ServerTimedAnimationController} treats it as part of the clock
     * contract: an action of age {@code a} samples clip time {@code a - TRANSITION_TICKS}, which is
     * what this controller has always shown an ordinary observer, so aging a late one changes
     * nothing for everybody else.
     */
    public static final int TRANSITION_TICKS = 5;

    private static final EntityDataAccessor<Integer> DATA_ROAR_TICKS =
            SynchedEntityData.defineId(GreatIzuchi.class, EntityDataSerializers.INT);

    /** Game time the current roar began; see {@link Roarable#getRoarStartTime()}. */
    private static final EntityDataAccessor<Long> DATA_ROAR_START =
            SynchedEntityData.defineId(GreatIzuchi.class, EntityDataSerializers.LONG);

    /**
     * Game time this body's death began, or {@link #NO_DEATH} while alive.
     *
     * <p>Vanilla's own {@code deathTime} counts the corpse hold, but it is never synced, so a client
     * that starts tracking a body already half way through its death clip would restart that clip.
     * This is the anchor that fixes it: stamped once, server-side, from {@code gameTime - deathTime},
     * which makes it correct both for a fresh death (where {@code deathTime} is 0) and for a body
     * restored from disk (where vanilla has already reloaded its saved {@code DeathTime}), without a
     * second death state machine, a second saved field, or any second call to {@code die()}.
     */
    private static final EntityDataAccessor<Long> DATA_DEATH_START =
            SynchedEntityData.defineId(GreatIzuchi.class, EntityDataSerializers.LONG);

    /** Sentinel for {@link #DATA_DEATH_START} while this monster is alive. */
    public static final long NO_DEATH = Long.MIN_VALUE;

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);
    private final MonsterPart[] parts;

    /** R1 carving: participants, personal counters and the deterministic reward table. */
    private final CarveState carveState = new CarveState(CarveState.Table.GREAT_IZUCHI);

    /** Server-only: ticks until another attack may start. Never saved, so a reload cools down. */
    public int attackCooldown = 20;

    /** De-duplicates one damage source that enumerates several parts within a single tick. */
    private DamageSource lastDamageSource;
    private int lastDamageTick = -1;

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

    /**
     * Automatic spawning honours both the habitat selector and the server off switch; see
     * {@link HuntingSpawnRules}, which owns that decision for every R1a species so the two cannot
     * drift apart. Before R1a this consulted the config for {@code NATURAL} only, which let
     * {@code CHUNK_GENERATION} straight past it.
     */
    public static boolean checkSpawnRules(EntityType<GreatIzuchi> type, ServerLevelAccessor level,
                                          MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return HuntingSpawnRules.checkMonster(type, level, spawnType, pos, random);
    }

    /** How many escort Izuchi spawn alongside a Great Izuchi, inclusive both ends. */
    private static final int MIN_ESCORTS = 1;
    private static final int MAX_ESCORTS = 4;

    /**
     * Candidate positions tried per requested escort before giving that one up. Bounded on purpose:
     * on a cliff face or in dense forest there may be no valid spot at all, and a smaller pack -- or
     * none -- is the right answer there. Retrying until it succeeds would either hang worldgen or
     * push an Izuchi into stone.
     */
    private static final int ESCORT_ATTEMPTS = 8;

    /** Escort ring around the leader, in blocks. Unchanged from the pre-R1a behaviour. */
    private static final double ESCORT_MIN_RADIUS = 2.0D;
    private static final double ESCORT_RADIUS_SPREAD = 3.0D;

    /**
     * How far above and below the leader an escort's own ground may be found, in blocks. Keeps a pack
     * on the same hillside instead of dropping a member off a ravine lip it would then have to path
     * back up, and bounds each candidate to a dozen block reads.
     *
     * <p>Deliberately a local scan rather than the world's surface heightmap: the heightmap answers
     * "where is the sky", which is the wrong question for a leader standing in a cave, under an
     * overhang or inside a structure -- resolving against it would put the pack on the roof above.
     */
    private static final int ESCORT_MAX_RISE = 2;
    private static final int ESCORT_MAX_DROP = 8;

    /**
     * A Great Izuchi is a pack leader: it never appears alone in the wild (section 2, pack mechanic).
     *
     * <p>Only genuinely wild origins bring a pack. A spawn egg, {@code /summon} or any other
     * deliberate placement gets a lone leader, which is what makes the egg usable as a development
     * tool.
     *
     * <p>Escort <em>placement</em> is the R1a correction. The original loop put every escort at the
     * leader's own Y, which is correct only on flat ground: in real terrain that buries members in a
     * hillside or hangs them in the air over a drop. Each escort now resolves its own surface Y from
     * the spawn heightmap and is accepted only if it has solid ground, a clear body-sized volume, no
     * fluid, no other entity in the way and -- for a naturally spawned leader standing at the biome
     * edge -- a position still inside the habitat selector. A manually spawned leader keeps its
     * unrestricted behaviour, so a spawner or egg still works outside the biome.
     */
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
        int requested = MIN_ESCORTS + this.random.nextInt(MAX_ESCORTS - MIN_ESCORTS + 1);
        for (int i = 0; i < requested; i++) {
            trySpawnEscort(level, difficulty, spawnType);
        }
        return result;
    }

    /** One escort, or none if {@link #ESCORT_ATTEMPTS} nearby candidates all fail their checks. */
    private void trySpawnEscort(ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty,
                                MobSpawnType spawnType) {
        for (int attempt = 0; attempt < ESCORT_ATTEMPTS; attempt++) {
            BlockPos pos = findEscortPos(level, spawnType);
            if (pos == null) {
                continue;
            }
            Izuchi izuchi = ModEntities.IZUCHI.get().create(level.getLevel());
            if (izuchi == null) {
                // Not terrain rejection: the entity type failed to build one. Say so rather than
                // letting it look like a crowded hillside.
                com.carro1001.mhnw.MHNW.LOG.warn(
                        "Izuchi entity factory returned null; Great Izuchi at {} spawns without a full escort",
                        blockPosition());
                return;
            }
            izuchi.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                    this.random.nextFloat() * 360.0F, 0.0F);
            if (!level.getLevel().noCollision(izuchi)) {
                // Another entity, or a block the static checks could not see from the corner position.
                izuchi.discard();
                continue;
            }
            izuchi.finalizeSpawn(level, difficulty, MobSpawnType.MOB_SUMMONED, null);
            level.addFreshEntity(izuchi);
            return;
        }
    }

    /** A terrain-safe escort position in the ring around the leader, or {@code null} if this try failed. */
    private BlockPos findEscortPos(ServerLevelAccessor level, MobSpawnType spawnType) {
        double angle = this.random.nextDouble() * Math.PI * 2.0D;
        double dist = ESCORT_MIN_RADIUS + this.random.nextDouble() * ESCORT_RADIUS_SPREAD;
        int x = net.minecraft.util.Mth.floor(getX() + Math.cos(angle) * dist);
        int z = net.minecraft.util.Mth.floor(getZ() + Math.sin(angle) * dist);
        int leaderY = blockPosition().getY();
        if (!level.hasChunkAt(new BlockPos(x, leaderY, z))) {
            // Never load or generate a chunk to complete a pack.
            return null;
        }
        EntityType<Izuchi> type = ModEntities.IZUCHI.get();
        // Walk the candidate column from just above the leader down to the bottom of the allowed
        // drop, and take the first spot that is genuinely standable.
        for (int y = leaderY + ESCORT_MAX_RISE; y >= leaderY - ESCORT_MAX_DROP; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!HuntingSpawnRules.hasSolidGround(level, type, pos)
                    || !HuntingSpawnRules.isFree(level, type, pos)) {
                continue;
            }
            if (spawnType == MobSpawnType.NATURAL && !HuntingSpawnRules.inHabitat(level, pos)) {
                // The leader may stand at the biome edge; its wild pack still belongs in the habitat.
                return null;
            }
            return pos;
        }
        return null;
    }

    @Override
    protected void registerGoals() {
        // One combat owner. GreatIzuchiCombatGoal selects, approaches, orients and executes the
        // attack; no other goal moves this mob toward a target or deals its damage (section 4.3).
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Sits above the combat goal deliberately (section on RoarGoal): the opening roar must
        // freeze the fight, not play underneath an attack goal that keeps moving/swinging.
        this.goalSelector.addGoal(1, new RoarGoal<>(this));
        this.goalSelector.addGoal(2, new GreatIzuchiCombatGoal(this));
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
        builder.define(DATA_ROAR_TICKS, 0);
        builder.define(DATA_ROAR_START, 0L);
        builder.define(DATA_DEATH_START, NO_DEATH);
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
    public long getRoarStartTime() {
        return this.entityData.get(DATA_ROAR_START);
    }

    @Override
    public void setRoarStartTime(long gameTime) {
        this.entityData.set(DATA_ROAR_START, gameTime);
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

    /** Game time this body's death began, or {@link #NO_DEATH} while alive. Valid on both sides. */
    public long getDeathStartTime() {
        return this.entityData.get(DATA_DEATH_START);
    }

    /**
     * Server: stamp the death anchor once, the first tick this body is dead.
     *
     * <p>Derived from {@code gameTime - deathTime} rather than simply {@code gameTime}, which is the
     * whole trick: on a fresh death {@code deathTime} is 0 so the two are identical, and on a body
     * restored from disk vanilla has already read its saved {@code DeathTime} back, so the anchor
     * reconstructs the death's real age without persisting anything of our own and without calling
     * {@code die()} -- no second XP drop, no second loot roll, no revived state.
     */
    private void stampDeathStart() {
        if (!level().isClientSide && isDeadOrDying() && getDeathStartTime() == NO_DEATH) {
            this.entityData.set(DATA_DEATH_START, level().getGameTime() - this.deathTime);
        }
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
            part.setPos(centre.x, part.restingY(centre.y), centre.z);
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
        stampDeathStart();
        if (!level().isClientSide && this.attackCooldown > 0) {
            this.attackCooldown--;
        }
    }

    /**
     * Hold the body for the whole R1 carving window instead of vanilla's 20 ticks.
     *
     * <p>This used to hold only {@link #DEATH_ANIMATION_TICKS} (38), long enough for the authored
     * death clip; {@link CarveState#CORPSE_TICKS} subsumes that, and the clip's own
     * {@code thenPlayAndHold} keeps its last frame for the rest. The counter is vanilla's
     * {@code deathTime}, so it advances only while this entity actually ticks and vanilla already
     * persists it -- see {@link CarveState} on why there is no second counter.
     */
    @Override
    protected void tickDeath() {
        if (!CarveState.corpseExpired(this)) {
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
    /** Allied to its own escorts, so vanilla's own alert and targeting helpers never set the pack
     * against itself. The {@link #hurt} guard is the rule; this keeps vanilla agreeing with it. */
    @Override
    public boolean isAlliedTo(net.minecraft.world.entity.Entity other) {
        return Izuchi.isPackMember(other) || super.isAlliedTo(other);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // The pack does not wound its own; see {@link Izuchi#hurt} for why the guard sits on the
        // receiving end rather than inside each attack's volume.
        if (Izuchi.isPackMember(source.getEntity())) {
            return false;
        }
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
        // R1: credit the attacking player only once the hit is actually accepted AND health
        // genuinely falls, which is what makes absorbed, invulnerable and duplicated part damage
        // grant nothing. See CarveState.creditDamage.
        float before = getHealth();
        boolean accepted = super.hurt(source, amount);
        if (accepted && !level().isClientSide && getHealth() < before) {
            this.carveState.creditDamage(source);
        }
        return accepted;
    }

    /** R1 carving state. Package-visible to the goals and tests; nothing else reads it. */
    public CarveState carveState() {
        return this.carveState;
    }

    /**
     * Shift + right-click carving. {@code Mob.interact} is final and returns PASS for anything not
     * alive, so a corpse can only be reached through {@code interactAt} -- which the client tries
     * first anyway, and which {@link MonsterPart} forwards here so the tail is a usable carve
     * surface on a body longer than a player's reach.
     */
    @Override
    public net.minecraft.world.InteractionResult interactAt(Player player, Vec3 location,
                                                            net.minecraft.world.InteractionHand hand) {
        net.minecraft.world.InteractionResult carved = this.carveState.interact(this, player, hand);
        return carved == net.minecraft.world.InteractionResult.PASS
                ? super.interactAt(player, location, hand)
                : carved;
    }

    /**
     * Keep the corpse from being despawned out from under its carvers. A dead mob is still subject
     * to {@code Mob.checkDespawn}'s distance rule, which would delete the body long before the
     * carving window expires, and the window is deliberately not allowed to force-load chunks.
     */
    @Override
    public void die(DamageSource source) {
        super.die(source);
        setPersistenceRequired();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        // Transient combat state is deliberately not saved. Reloading cancels the action, and the
        // fresh cooldown prevents an instant swing on load (handoff section 4.3 rule 7).
        // Carving participation is the exception: it is earned, not transient, and has to survive a
        // chunk unload between the first hit and the kill.
        this.carveState.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.attackCooldown = 20;
        this.carveState.load(tag);
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
        controllers.add(new ServerTimedAnimationController<>(this, "main", TRANSITION_TICKS, this::mainAnim));
    }

    /**
     * Presentation follows the synchronized action identity, never the reverse. Because that state
     * is synched entity data rather than a one-shot packet, a client that starts tracking mid-fight
     * receives the live action; once the server clears it, nothing replays.
     *
     * <p>Since R0b the clip is also played at its real age rather than from its first frame, so a
     * client that starts tracking mid-action sees the current phase instead of replaying the windup
     * while the server is already landing the hit. {@link ServerTimedAnimationController} does that,
     * and its class doc carries the convention and why it does not move contact timing. Note what is
     * <em>not</em> changed: the action still ends at the profile's {@code actionEnd}, so the last
     * {@link #TRANSITION_TICKS} ticks of each attack clip have never been, and still are not, shown.
     */
    private PlayState mainAnim(AnimationState<GreatIzuchi> state) {
        ServerTimedAnimationController<GreatIzuchi> controller = ServerTimedAnimationController.of(state);
        double partial = state.getPartialTick();
        long now = level().getGameTime();

        // One priority decision picks the clip, its instance and its clock together (R0b). Splitting
        // them is how a death pose ends up driven by a stale attack clock.
        if (isDeadOrDying()) {
            long start = getDeathStartTime();
            return controller.playTimed(state, DEATH, ServerTimedAnimationController.KIND_DEATH,
                    start, start == NO_DEATH ? partial : now - start + partial);
        }
        if (isRoaring()) {
            long start = getRoarStartTime();
            return controller.playTimed(state, ROAR, ServerTimedAnimationController.KIND_ROAR,
                    start, now - start + partial);
        }
        byte attack = getAttackId();
        if (attack != ATTACK_NONE) {
            return controller.playTimed(state, switch (attack) {
                case ATTACK_TAIL_SWIPE -> TAIL_SWIPE;
                case ATTACK_TAIL_SLAM -> TAIL_SLAM;
                default -> SCRATCH;
            }, ServerTimedAnimationController.KIND_ATTACK, getActionSequence(), getAttackAge() + partial);
        }
        if (state.isMoving()) {
            // isAggressive() rides the synched mob flags, so it is readable here. getTarget() is
            // server-only state and is always null on a client, which would pin this to WALK.
            return controller.playFree(state, isAggressive() ? RUN : WALK);
        }
        return controller.playFree(state, IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animCache;
    }
}
