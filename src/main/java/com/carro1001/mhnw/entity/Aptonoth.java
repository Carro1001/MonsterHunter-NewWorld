package com.carro1001.mhnw.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
 * The P3 proof of reuse: a passive herbivore, not another combat monster.
 *
 * <p>Ordinary in every way except one: it is {@link MonsterPart} multipart for the chest, head, and
 * a three-segment tail, because a single vanilla AABB genuinely cannot fit a long-necked, long-tailed
 * quadruped any better than it can a wyvern's tail. This was originally single-hurtbox on the theory
 * that section 4.2's "a single fitted region is fine for a small creature" applied; a screenshot of
 * the actual shape made clear that theory did not survive contact with this particular body plan,
 * and {@link MonsterPart} generalizing to any {@link net.minecraft.world.entity.Mob} rather than
 * only hostile {@code Monster}s is what made fixing that possible without a parallel class
 * hierarchy.
 *
 * <p>A "chest" part was tried and dropped once already on the theory that a passive animal has no
 * combat precision to gain from one over the plain root hitbox ({@link #BODY_WIDTH}/
 * {@link #BODY_HEIGHT}) -- but even after enlarging the root box to reach the measured chest height,
 * feedback from a screenshot was that the torso still needed its own volume, so it is back. The
 * lesson: "this creature doesn't fight, so precision doesn't matter" turned out to be true for combat
 * but not for how the F3+B debug view reads visually; the root box alone still looked wrong even once
 * correctly sized.
 *
 * <p>None of Great Izuchi's attack-timeline or attack-profile machinery is used here; only the part
 * positioning, which is a different, smaller piece of that infrastructure.
 *
 * <p>{@link Animal} rather than {@link PathfinderMob} directly, purely so {@link EatBlockGoal}
 * (used for the grass-eating flavour the {@code eat} clip was authored for) has the convenience
 * base it expects. Breeding is not implemented: {@link #isFood} always returns false, so normal
 * play can never enter love mode, and {@link #getBreedOffspring} returns null for the rare edge
 * case (a data command, another mod) that forces it anyway.
 *
 * <h2>Behaviour: roam, flee, defend</h2>
 * {@link PanicGoal} and the retaliation goal both react to being hurt, and Panic sits at a higher
 * priority and shares its move flag, so in practice a hurt Aptonoth almost always flees rather than
 * fights. The weak counter-attack still exists as the "defend when provoked" fallback the handoff
 * describes, for the rarer case where panic's own conditions have already lapsed while the target
 * memory has not. This is a deliberately soft, not-strongly-enforced distinction; nothing here
 * promises Aptonoth ever really turns and fights.
 */
public class Aptonoth extends Animal implements GeoEntity {

    // Provisional balance constants, not yet tuned.
    public static final double MAX_HEALTH = 12.0D;
    public static final double MOVE_SPEED = 0.22D;
    public static final double ATTACK_DAMAGE = 1.0D;

    // BODY_HEIGHT was reduced to 1.0 early on for reading "too tall and boxy" in a screenshot, but
    // that undershot: the real BoneProbe measurement for the "body" bone (the torso's own pivot, see
    // the constructor below) sits at up=1.87, meaning a 1.0-tall root box doesn't even reach the
    // CENTRE of the chest, let alone its top -- confirmed by a later screenshot showing the white
    // root box sitting well below the visible chest. Raised to comfortably clear that measured
    // centre; still not itself a direct measurement of the box's own correct height (that would need
    // a leg/foot bone in the BoneProbe capture to fix the ground reference precisely), so treat as
    // closer, not necessarily final. BODY_WIDTH nudged up slightly toward the real mesh's ~1.25-wide
    // main body cube (aptonoth.geo.json), a much smaller, low-risk change by comparison.
    public static final float BODY_WIDTH = 1.3F;
    public static final float BODY_HEIGHT = 2.0F;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.aptonoth.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.aptonoth.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.aptonoth.run");
    private static final RawAnimation EAT = RawAnimation.begin().thenLoop("animation.aptonoth.eat");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.aptonoth.death");

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);
    private final MonsterPart[] parts;

    /**
     * Client-side countdown driving the eat animation, the same mechanism vanilla's own grazing
     * animals use (see {@code Sheep.eatAnimationTick}): the server's {@link EatBlockGoal} fires a
     * one-shot entity event on {@code start()}, and each tracking client counts its own copy down
     * from there. This is a cosmetic idle detail, not synchronized combat state, so a client that
     * starts tracking mid-graze simply does not see that particular cycle's animation; it is not
     * the "join in progress must see the current action" contract section 4.4 requires of combat.
     */
    private int eatAnimationTicks;

    /** Same fairness-corrected guard as the large monsters; see {@link Rathian#hurt} for why the
     * {@code invulnerableTime} reset matters and not just the source-identity check. */
    private DamageSource lastDamageSource;
    private int lastDamageTick = -1;

    public Aptonoth(EntityType<? extends Animal> type, Level level) {
        super(type, level);
        // Real BoneProbe measurements for head/tail_1/tail_3 (see docs/TEST_PLAN.md and Rathian's
        // constructor for the same change): the maintainer stood near a live Aptonoth across several
        // sessions, and the head/tail1/tail2 bones' logged positions came back the same every time.
        //
        // `chest` moved forward: it was originally placed at the measured "body" bone position
        // itself (forward=0.00), which is also where the vanilla root hitbox is centred -- a
        // screenshot showed the root box reading rear-biased (toward the hips, not the chest), with
        // no separate chest box visible at all because it sat exactly on top of the root box. Moved
        // forward toward the neck base (measured at forward=1.74) to actually cover the front of the
        // ribcage the root box was missing, which is the whole reason this part exists.
        //
        // Tail simplified back to three points (two measured endpoints, one interpolated midpoint)
        // sized to exactly touch their neighbours, not six: aptonoth.geo.json's tail1/tail2 bones
        // carry their own additional rotation (tail2 tilts a further -15 degrees beyond tail1's own
        // -7.5), meaning the tail genuinely curves -- so six points spaced by straight-line
        // interpolation between the two measured ends don't lie on that real curve, and sized
        // generously enough to avoid gaps, ended up overlapping each other instead (confirmed by a
        // screenshot). There is no per-bone "binding" to get wrong here (parts are static offsets,
        // not attached to a live bone), so the fix is fewer points sized by the actual math: three
        // points 0.96 blocks apart, each 0.96 wide, meet their neighbours exactly -- no gap, no
        // overlap -- rather than guessing at the curve's true shape with more synthetic points.
        this.parts = new MonsterPart[] {
                //              name          width height  left    up      forward
                new MonsterPart(this, "chest",  1.1F, 1.1F, 0.00D, 1.90D,  1.00D),
                new MonsterPart(this, "head",   0.9F, 0.9F, 0.00D, 2.25D,  2.85D),
                new MonsterPart(this, "tail_1", 0.96F, 0.6F, 0.00D, 2.60D, -1.31D),
                new MonsterPart(this, "tail_2", 0.96F, 0.6F, 0.00D, 2.75D, -2.27D),
                new MonsterPart(this, "tail_3", 0.96F, 0.6F, 0.00D, 2.89D, -3.23D),
        };
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, MOVE_SPEED)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new AptonothPanicGoal(this, 1.5D));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(3, new EatBlockGoal(this));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        // Never seeks a fight; HurtByTargetGoal only means it can retaliate against whoever
        // actually hit it, and only while Panic is not itself already handling that same trigger.
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    /**
     * No offspring, since breeding is not implemented. {@code null}, not an exception: normal
     * player interaction can never reach this (love mode requires {@link #isFood} to accept an
     * item first, and that always returns false), but {@code Animal.spawnChildFromBreeding}
     * already treats a null result as "no child" and resets both animals' love state cleanly, so
     * that is what an edge case that forces love mode some other way (a data command, another mod)
     * gets, rather than an uncaught exception out of goal ticking.
     */
    @Override
    public net.minecraft.world.entity.AgeableMob getBreedOffspring(
            net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.AgeableMob other) {
        return null;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 10) {
            this.eatAnimationTicks = 40;
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public void aiStep() {
        if (level().isClientSide && this.eatAnimationTicks > 0) {
            this.eatAnimationTicks--;
        }
        super.aiStep();
        // positionParts() is NOT called here: it belongs in tick(), after super.tick() (which
        // calls this method) fully returns, since LivingEntity.tick() further smooths yBodyRot via
        // tickHeadTurn() after aiStep() itself is done. See tick()'s comment.
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

    /** Same fix as the large monsters: a client independently allocating part ids would disagree
     * with the server about which id is which part (MC-158205). */
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

    /** Position parts exactly ONCE per tick, here, after super.tick() (and the aiStep() + body-yaw
     * smoothing it runs) fully returns. See {@link GreatIzuchi#tick()}'s comment for why calling
     * this a second time from aiStep() as well was a real bug (a corrupted interpolation "old"
     * value, not just wasted work) rather than harmless redundancy. */
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

    /**
     * {@code getTarget()} and {@code getLastHurtByMob()} are plain server-side fields, never synced
     * to the client (unlike, say, an {@code EntityDataAccessor}); reading either one in client
     * presentation code is always null there. Driving vanilla's own already-synced aggressive flag
     * here, the same fix {@code GreatIzuchi} needed for the same reason, is what actually lets
     * {@link #mainAnim} tell a fleeing or retaliating Aptonoth from one that is merely wandering.
     */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        setAggressive(getTarget() != null || getLastHurtByMob() != null);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0D;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 6, this::mainAnim));
    }

    private PlayState mainAnim(AnimationState<Aptonoth> state) {
        if (isDeadOrDying()) {
            return state.setAndContinue(DEATH);
        }
        if (this.eatAnimationTicks > 0) {
            return state.setAndContinue(EAT);
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
