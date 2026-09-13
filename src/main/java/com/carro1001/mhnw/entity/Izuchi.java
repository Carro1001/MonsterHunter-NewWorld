package com.carro1001.mhnw.entity;

import com.carro1001.mhnw.animation.ServerTimedAnimationController;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
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
 * The small Izuchi: P4's "simple independent movement/targeting" species (handoff section 5, P4),
 * a genuinely hostile ground monster, unlike any of the P3 endemic life.
 *
 * <p>The root box covers the torso, with a head and two tail parts extending the long model. Its
 * recovered tail swipe uses the same server-owned action clock and server-side damage-volume rule
 * as Great Izuchi, but remains part of {@link IzuchiHarassGoal}'s bounded pack turn rather than
 * acquiring a second combat goal.
 *
 * <p>R1 replaced the vanilla {@code MeleeAttackGoal} this used to run with
 * {@link IzuchiHarassGoal}: same ordinary damage, but the escorts now circle at a distance and take
 * bounded turns darting in, instead of four of them closing to melee and staying there. Reaching
 * the target now commits the recovered tail swipe before the member retreats.
 *
 * <h2>Carving</h2>
 * One of R1's three carvable species. Participation, the personal three-carve quota and the corpse
 * window all live in {@link CarveState}; this class forwards damage, interaction, death, save and
 * load to it and owns nothing of that contract itself.
 *
 * <h2>Recovered attack art</h2>
 * The archived {@code brain} branch contained a real small-Izuchi tail swipe that had never been
 * merged into master. The maintainer supplied video of it running and approved restoring it. Its
 * nine tracks for Great-Izuchi-only bones were removed; the remaining tracks target this model's
 * actual rig and are played from synchronized action state. The other candidate clips remain
 * quarantined, and there is still no authored death clip, so vanilla's corpse flop remains.
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

    public static final byte ATTACK_NONE = 0;
    public static final byte ATTACK_TAIL_SWIPE = 1;

    private static final EntityDataAccessor<Byte> DATA_ATTACK_ID =
            SynchedEntityData.defineId(Izuchi.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Long> DATA_ATTACK_START =
            SynchedEntityData.defineId(Izuchi.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DATA_ACTION_SEQUENCE =
            SynchedEntityData.defineId(Izuchi.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> DATA_SLEEPING =
            SynchedEntityData.defineId(Izuchi.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.izuchi.idle");
    private static final RawAnimation SLEEP = RawAnimation.begin().thenLoop("animation.izuchi.sleep");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.izuchi.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.izuchi.run");
    private static final RawAnimation TAIL_SWIPE =
            RawAnimation.begin().thenPlay("animation.izuchi.attack_tailswipe");

    public static final int TRANSITION_TICKS = 5;

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);
    private final MonsterPart[] parts;

    /** R1 carving: participants, personal counters and the deterministic reward table. */
    private final CarveState carveState = new CarveState(CarveState.Table.IZUCHI);

    /**
     * Which phase of {@link IzuchiHarassGoal} this Izuchi is in, or null when it is not harassing.
     *
     * <p>Transient on purpose: a half-finished dart is not a fact worth restoring, and the one-darter
     * rule is enforced by reading this field off the neighbours rather than by any shared owner.
     */
    private IzuchiHarassGoal.Phase harassPhase;
    private int actionSequenceCounter;
    private Float committedBodyYaw;
    private DamageSource lastDamageSource;
    private int lastDamageTick = -1;

    public Izuchi(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        // F3+B acceptance 2026-09-13: the far tip box read as surplus; two smaller tail segments
        // give the visible tail a more faithful, less intrusive static picking envelope.
        this.parts = new MonsterPart[] {
                new MonsterPart(this, "head",      0.70F, 0.70F, 0.00D, 1.35D,  0.80D),
                new MonsterPart(this, "tail_base", 0.95F, 0.60F, 0.00D, 1.25D, -1.00D),
                new MonsterPart(this, "tail_mid",  0.85F, 0.60F, 0.00D, 1.25D, -1.95D),
        };
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
        this.goalSelector.addGoal(1, new IzuchiHarassGoal(this));
        this.goalSelector.addGoal(2, new IzuchiSleepGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
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
        builder.define(DATA_SLEEPING, false);
        builder.define(DATA_ATTACK_ID, ATTACK_NONE);
        builder.define(DATA_ATTACK_START, 0L);
        builder.define(DATA_ACTION_SEQUENCE, 0);
    }

    public boolean isSleeping() {
        return this.entityData.get(DATA_SLEEPING);
    }

    /** Test/behaviour seam: the live harassment phase, or null when the goal is not running. */
    public IzuchiHarassGoal.Phase harassPhase() {
        return this.harassPhase;
    }

    void setHarassPhase(IzuchiHarassGoal.Phase phase) {
        this.harassPhase = phase;
    }

    /** Read by neighbouring {@link IzuchiHarassGoal}s while approaching the target. */
    public boolean isDarting() {
        return this.harassPhase == IzuchiHarassGoal.Phase.DART;
    }

    /** The one pack slot stays occupied through both approach and committed swing. */
    public boolean isTakingAttackTurn() {
        return isDarting() || this.harassPhase == IzuchiHarassGoal.Phase.ATTACK;
    }

    public byte getAttackId() {
        return this.entityData.get(DATA_ATTACK_ID);
    }

    public int getAttackAge() {
        return getAttackId() == ATTACK_NONE
                ? -1
                : (int) (level().getGameTime() - this.entityData.get(DATA_ATTACK_START));
    }

    public int getActionSequence() {
        return this.entityData.get(DATA_ACTION_SEQUENCE);
    }

    void beginTailSwipe() {
        this.actionSequenceCounter++;
        this.entityData.set(DATA_ATTACK_ID, ATTACK_TAIL_SWIPE);
        this.entityData.set(DATA_ATTACK_START, level().getGameTime());
        this.entityData.set(DATA_ACTION_SEQUENCE, this.actionSequenceCounter);
        this.committedBodyYaw = this.yBodyRot;
    }

    void endAttack() {
        this.entityData.set(DATA_ATTACK_ID, ATTACK_NONE);
        this.committedBodyYaw = null;
    }

    /** Rotates a local (left, up, forward) offset into world space using the committed body yaw. */
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
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return this.parts;
    }

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
        for (MonsterPart part : this.parts) {
            if (part.partName.equals(name)) {
                return part;
            }
        }
        throw new IllegalArgumentException("no such part: " + name);
    }

    private void positionParts() {
        for (MonsterPart part : this.parts) {
            part.setOldPosAndRot();
            Vec3 centre = localToWorld(part.localLeft, part.localUp, part.localForward);
            part.setPos(centre.x, part.restingY(centre.y), centre.z);
        }
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        positionParts();
    }

    /** R1 carving state. */
    public CarveState carveState() {
        return this.carveState;
    }

    /**
     * Parts forward here, so one source touching the root and several parts in a tick still costs
     * one hit while two distinct attackers remain independent.
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
        float before = getHealth();
        boolean accepted = super.hurt(source, amount);
        if (accepted && !level().isClientSide && getHealth() < before) {
            this.carveState.creditDamage(source);
        }
        return accepted;
    }

    /** Shift + right-click carving; see {@link GreatIzuchi#interactAt}. */
    @Override
    public net.minecraft.world.InteractionResult interactAt(Player player, Vec3 location,
                                                            net.minecraft.world.InteractionHand hand) {
        net.minecraft.world.InteractionResult carved = this.carveState.interact(this, player, hand);
        return carved == net.minecraft.world.InteractionResult.PASS
                ? super.interactAt(player, location, hand)
                : carved;
    }

    /**
     * Corpse persistence, and the one place the harassment state can still be cleared on death.
     *
     * <p>{@code setPersistenceRequired} is the {@link GreatIzuchi#die} reason: a corpse must outlive
     * the distance-despawn rule. The rest is {@link IzuchiHarassGoal}'s transient state, cleared
     * here because it cannot clear itself: vanilla stops ticking every goal the instant
     * {@code isDeadOrDying()} is true, so a mob killed mid-dart never runs {@code stop()} and would
     * otherwise sit there flagged as darting and aggressive for the whole corpse window. The
     * neighbours' pack scan already ignores the dead, but that hides the symptom rather than
     * clearing the state -- and it is the scan's real job to cover the case this hook cannot,
     * a body removed by {@code discard()} without {@code die()} ever running.
     */
    @Override
    public void die(DamageSource source) {
        super.die(source);
        setPersistenceRequired();
        endAttack();
        setHarassPhase(null);
        setAggressive(false);
        getNavigation().stop();
    }

    /**
     * Hold the body for the R1 carving window; see {@link GreatIzuchi#tickDeath}. Unlike the large
     * monsters this species has no authored death clip, so the held body is vanilla's ordinary
     * corpse flop, kept around rather than replaced.
     */
    @Override
    protected void tickDeath() {
        if (!CarveState.corpseExpired(this)) {
            this.deathTime++;
            return;
        }
        super.tickDeath();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        this.carveState.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.carveState.load(tag);
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
    public void tick() {
        super.tick();
        if (!level().isClientSide && this.committedBodyYaw != null) {
            this.yBodyRot = this.committedBodyYaw;
            setYRot(this.committedBodyYaw);
        }
        positionParts();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new ServerTimedAnimationController<>(
                this, "main", TRANSITION_TICKS, this::mainAnim));
    }

    private PlayState mainAnim(AnimationState<Izuchi> state) {
        ServerTimedAnimationController<Izuchi> controller = ServerTimedAnimationController.of(state);
        if (getAttackId() == ATTACK_TAIL_SWIPE) {
            return controller.playTimed(state, TAIL_SWIPE,
                    ServerTimedAnimationController.KIND_ATTACK, getActionSequence(),
                    getAttackAge() + state.getPartialTick());
        }
        if (isSleeping()) {
            return controller.playFree(state, SLEEP);
        }
        if (state.isMoving()) {
            return controller.playFree(state, isAggressive() ? RUN : WALK);
        }
        return controller.playFree(state, IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animCache;
    }
}
