package com.carro1001.mhnw.entity;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.MHNWConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Rathian's combat owner: approach, orientation, and the {@code attack_charge_bite_right} timeline
 * -- the same windup/active/recovery shape as {@link GreatIzuchiCombatGoal}, and for the same
 * reason. Reported problem this replaced: plain vanilla {@code MeleeAttackGoal} closes to contact
 * range and deals damage the instant it touches, which reads as a body-slam that only afterward
 * plays a bite clip, rather than a bite that actually connects. This goal stops the approach, winds
 * up while the clip plays, and only evaluates a hit volume during the active window.
 *
 * <p>Duplicated from {@link GreatIzuchiCombatGoal} rather than shared through a generalized base:
 * this is the second real attack-timeline implementation in the codebase, which is exactly the
 * point {@code docs/DEFERRED.md} names as worth reconsidering that decision at -- but doing so now
 * would mean restructuring Great Izuchi's already-shipped, player-tested combat at the same time as
 * standing up Rathian's first cut, with no way to interactively verify the result beyond GameTests.
 * Revisit once this one has also seen live play.
 *
 * <p>{@link #BITE} is a single, hand-estimated profile, not a live bone-probe capture -- see
 * {@link Rathian}'s own class doc for exactly what is and is not measured about it yet.
 */
public class RathianCombatGoal extends Goal {

    /**
     * The one attack this species owns for real so far. {@code windupEnd}/{@code activeStart}/
     * {@code activeEnd}/{@code actionEnd} split the clip's 30 ticks (1.5s, see {@code Rathian}'s
     * {@code BITE} animation) into thirds as an estimate of the bite's shape, not measured timing.
     *
     * <p>The path is a single static point close in front of the body, deliberately NOT the
     * already-measured {@code head} hurtbox offset (up 1.61, forward 7.30): that idle position is
     * where the head sits at the end of the fully-extended resting neck, nowhere near where a bite
     * needs to land against a target within melee reach. Widened generously (volume size 2.2, well
     * past every other profile in this codebase) rather than chased to a precise centre, since
     * there is no captured path yet to say where the jaw actually closes -- the same "cover the
     * range, don't guess the exact point" lesson the hurtbox tuning already learned the hard way.
     * Replace both the point and this width once a live capture gives a real path to bake.
     */
    public static final AttackProfile BITE = new AttackProfile(
            Rathian.ATTACK_BITE,
            10, 11, 20, 29,
            25, 1, 2.2D, Rathian.ATTACK_DAMAGE, 0.0F, 0.05D,
            0.0D, 3.0D,
            new double[][][] {{
                    // age    left      up   forward
                    {11, 0.00D, 1.90D, 1.60D},
            }});

    /** How close the monster tries to get before it may attack; matches the bite's own maxRange. */
    private static final double CLOSE_RANGE = 3.0D;

    private static final int REPATH_INTERVAL = 10;

    /** Same tuning as {@link GreatIzuchiCombatGoal}'s identical constants; see there for why. */
    private static final float WINDUP_TURN_RATE = 9.0F;
    private static final double WINDUP_DAMPING = 0.35D;
    private static final int LUNGE_RAMP_TICKS = 6;

    private final Rathian monster;

    /** Keys of {victim, strike window} already struck by the current action; see
     * {@link GreatIzuchiCombatGoal}'s identical field for why this is bounded and cleared per action. */
    private final List<Long> hitThisAction = new ArrayList<>();

    private int repathCooldown;
    private boolean attacking;
    private Vec3 lungeDirection = Vec3.ZERO;

    public RathianCombatGoal(Rathian monster) {
        this.monster = monster;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean targetIsValid() {
        LivingEntity target = this.monster.getTarget();
        return target != null && target.isAlive() && this.monster.isAlive();
    }

    @Override
    public boolean canUse() {
        return targetIsValid();
    }

    @Override
    public boolean canContinueToUse() {
        return this.attacking || targetIsValid();
    }

    @Override
    public boolean isInterruptable() {
        return !this.attacking;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.repathCooldown = 0;
        this.attacking = false;
    }

    @Override
    public void stop() {
        if (this.attacking) {
            this.attacking = false;
            this.monster.endAttack();
            this.monster.attackCooldown = BITE.cooldown();
        }
        this.monster.setCommittedBodyYaw(null);
        this.lungeDirection = Vec3.ZERO;
        this.hitThisAction.clear();
        this.monster.getNavigation().stop();
        this.monster.setAggressive(false);
    }

    @Override
    public void tick() {
        if (!this.monster.isAlive()) {
            stop();
            return;
        }

        if (this.attacking) {
            tickAttack();
            return;
        }

        LivingEntity target = this.monster.getTarget();
        if (target == null) {
            return;
        }

        this.monster.getLookControl().setLookAt(target, 30.0F, 30.0F);
        this.monster.setAggressive(true);

        double distance = distanceToBox(target);

        if (this.monster.attackCooldown <= 0 && distance <= BITE.maxRange()
                && this.monster.hasLineOfSight(target)) {
            this.monster.getNavigation().stop();
            beginAttack(target, distance);
            return;
        }

        if (distance > CLOSE_RANGE) {
            if (--this.repathCooldown <= 0) {
                this.repathCooldown = REPATH_INTERVAL;
                if (!this.monster.getNavigation().moveTo(target, 1.0D)) {
                    debug("approach: no path to {} at {} blocks",
                            target.getName().getString(), String.format("%.2f", distance));
                }
            }
            return;
        }
        this.monster.getNavigation().stop();
    }

    private void beginAttack(LivingEntity target, double distance) {
        this.attacking = true;
        this.hitThisAction.clear();
        this.lungeDirection = Vec3.ZERO;
        this.monster.setAggressive(true);
        this.monster.getNavigation().stop();
        this.monster.beginAttack(BITE.id());
        debug("attack start: entity={} id={} seq={} target={} distance={}",
                this.monster.getId(), BITE.id(), this.monster.getActionSequence(),
                target.getName().getString(), String.format("%.2f", distance));
    }

    private void debug(String message, Object... args) {
        if (MHNWConfig.DEBUG_COMBAT.get()) {
            MHNW.LOG.info("[rathian] " + message, args);
        }
    }

    private void tickAttack() {
        int age = this.monster.getAttackAge();

        if (age < 0 || age > BITE.actionEnd()) {
            debug("attack end: entity={} seq={} age={} victims={}",
                    this.monster.getId(), this.monster.getActionSequence(), age,
                    this.hitThisAction.size());
            this.attacking = false;
            this.monster.endAttack();
            this.monster.attackCooldown = BITE.cooldown();
            this.monster.setCommittedBodyYaw(null);
            this.lungeDirection = Vec3.ZERO;
            this.hitThisAction.clear();
            this.monster.setAggressive(false);
            return;
        }

        this.monster.getNavigation().stop();

        LivingEntity target = this.monster.getTarget();
        if (age <= BITE.windupEnd()) {
            if (target != null) {
                this.monster.getLookControl().setLookAt(target, 20.0F, 20.0F);
                aimAt(target);
            }
            plantForWindup();
            return;
        }

        if (age >= BITE.activeStart() && age <= BITE.activeEnd()) {
            lunge(age, target);
            applyContact(age);
        }
    }

    private void plantForWindup() {
        Vec3 velocity = this.monster.getDeltaMovement();
        this.monster.setDeltaMovement(
                velocity.x * WINDUP_DAMPING, velocity.y, velocity.z * WINDUP_DAMPING);
    }

    /** Same commit-once-then-hold contract as {@link GreatIzuchiCombatGoal#lunge}: aimed once on the
     * first active tick, not re-aimed at a target that moves after the strike is under way (rule 6). */
    private void lunge(int age, LivingEntity target) {
        if (age == BITE.activeStart() && target != null) {
            Vec3 toTarget = new Vec3(
                    target.getX() - this.monster.getX(), 0.0D, target.getZ() - this.monster.getZ());
            this.lungeDirection = toTarget.lengthSqr() > 1.0E-4D ? toTarget.normalize() : Vec3.ZERO;
        }
        if (this.lungeDirection.lengthSqr() <= 0.0D) {
            return;
        }
        double easeIn = Math.min(1.0D, (age - BITE.activeStart() + 1.0D) / LUNGE_RAMP_TICKS);
        double speed = BITE.lungeSpeed() * easeIn;
        Vec3 velocity = this.monster.getDeltaMovement();
        this.monster.setDeltaMovement(
                this.lungeDirection.x * speed, velocity.y, this.lungeDirection.z * speed);
    }

    /** Bite has no aim offset (straight ahead), so this only turns to face, unlike
     * {@link GreatIzuchiCombatGoal#aimArcAt}'s cross-body-swipe correction. */
    private void aimAt(LivingEntity target) {
        double dx = target.getX() - this.monster.getX();
        double dz = target.getZ() - this.monster.getZ();
        float bearing = (float) Math.toDegrees(Math.atan2(-dx, dz));

        Float held = this.monster.getCommittedBodyYaw();
        float currentYaw = held != null ? held : this.monster.yBodyRot;
        float step = Mth.clamp(
                Mth.wrapDegrees(bearing - currentYaw), -WINDUP_TURN_RATE, WINDUP_TURN_RATE);
        this.monster.setCommittedBodyYaw(Mth.wrapDegrees(currentYaw + step));
    }

    /** Same reason this is static and public as {@link GreatIzuchiCombatGoal#attackVolumes}: the
     * developer overlay draws the exact geometry the server hits with, not a second approximation. */
    public static AABB[] attackVolumes(Rathian monster, int age) {
        if (monster.getAttackId() != Rathian.ATTACK_BITE) {
            return new AABB[0];
        }
        double[] local = BITE.limbLocalAt(0, age);
        Vec3 centre = monster.localToWorld(local[0], local[1], local[2]);
        return new AABB[] {AABB.ofSize(centre, BITE.volumeSize(), BITE.volumeSize(), BITE.volumeSize())};
    }

    private void applyContact(int age) {
        if (this.monster.level().isClientSide) {
            return;
        }
        for (AABB volume : attackVolumes(this.monster, age)) {
            applyContactIn(age, volume);
        }
    }

    private void applyContactIn(int age, AABB volume) {
        for (Entity candidate : this.monster.level().getEntities(this.monster, volume)) {
            Entity resolved = candidate instanceof PartEntity<?> part ? part.getParent() : candidate;
            if (!(resolved instanceof LivingEntity victim)) {
                continue;
            }
            if (victim == this.monster || victim.is(this.monster) || !victim.isAlive()) {
                continue;
            }
            long strikeKey = ((long) victim.getId() << 8) | BITE.strikeIndexAt(age);
            if (this.hitThisAction.contains(strikeKey)) {
                continue;
            }
            if (!victim.getBoundingBox().intersects(volume)) {
                continue;
            }
            if (!this.monster.hasLineOfSight(victim)) {
                debug("contact rejected at age={}: {} is behind cover",
                        age, victim.getName().getString());
                continue;
            }
            this.hitThisAction.add(strikeKey);
            float damage = (float) this.monster.getAttributeValue(Attributes.ATTACK_DAMAGE);
            victim.invulnerableTime = 0;
            victim.hurt(this.monster.damageSources().mobAttack(this.monster), damage);
            debug("contact accepted at age={} strike={}: {} for {} damage (seq={})",
                    age, BITE.strikeIndexAt(age), victim.getName().getString(), damage,
                    this.monster.getActionSequence());
        }
    }

    private double distanceToBox(LivingEntity target) {
        return Math.sqrt(target.getBoundingBox().distanceToSqr(this.monster.position()));
    }

    /** True while an action is committed. Used by tests and the developer overlay. */
    public boolean isAttacking() {
        return this.attacking;
    }
}
