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
 * Rathian's combat owner: approach, orientation, and the two mirrored bite timelines -- the same
 * windup/active/recovery shape as {@link GreatIzuchiCombatGoal}, and for the same reason. Reported
 * problem this replaced: plain vanilla {@code MeleeAttackGoal} closes to contact range and deals
 * damage the instant it touches, which reads as a body-slam that only afterward plays a bite clip,
 * rather than a bite that actually connects. This goal stops the approach, winds up while the clip
 * plays, and only evaluates a hit volume during the active window.
 *
 * <p>Duplicated from {@link GreatIzuchiCombatGoal} rather than shared through a generalized base:
 * this is the second real attack-timeline implementation in the codebase, which is exactly the
 * point {@code docs/DEFERRED.md} names as worth reconsidering that decision at -- but doing so now
 * would mean restructuring Great Izuchi's already-shipped, player-tested combat at the same time as
 * standing up Rathian's first cut, with no way to interactively verify the result beyond GameTests.
 * Revisit once this one has also seen live play.
 */
public class RathianCombatGoal extends Goal {

    /**
     * A real path baked from a live capture (see {@code docs/TEST_PLAN.md}), not the round-one
     * hand-estimate it replaced.
     *
     * <p>Bucketing every {@code Jaw}-bone sample from that capture by its position in the clip
     * showed the earlier guess was wrong on both counts it was estimated: the jaw does not dip down
     * close to the body early on -- it stays reared up and far out (up 4+, forward 5.3-6.9) for most
     * of the clip -- and only actually descends toward something reachable in the clip's last third
     * (age 19-28, up dropping from 4.48 to 0.91, forward settling to 4.3-5.5). That descent is the
     * real bite, not the first half of the clip. Every sample's {@code left} oscillated with no
     * consistent sign (residual aiming noise from the goal's own per-attack facing, not a real
     * animation offset), so it's set to 0 throughout, the same convention every measured hurtbox in
     * this file already uses for the same reason.
     *
     * <p>{@code minRange}/{@code maxRange} come directly from where this path can actually reach
     * (forward 4.26-5.52, padded by the volume's own half-width); previously this fired from as
     * close as touching distance, which is well short of where this path lands.
     */
    public static final AttackProfile BITE_RIGHT = new AttackProfile(
            Rathian.ATTACK_BITE_RIGHT,
            18, 19, 28, 29,
            25, 1, 1.8D, Rathian.ATTACK_DAMAGE, 0.0F, 0.05D,
            2.0D, 6.0D,
            new double[][][] {{
                    // age    left      up   forward
                    {19, 0.00D, 4.48D, 5.52D},
                    {22, 0.00D, 2.51D, 5.48D},
                    {25, 0.00D, 1.38D, 4.26D},
                    {28, 0.00D, 0.91D, 4.37D},
            }});

    /**
     * {@code attack_charge_bite_left} mirrored from {@link #BITE_RIGHT}'s own measured path, not a
     * separate capture: {@code left} was already 0 throughout the right bite's real data (any real
     * per-side asymmetry was smaller than the aiming noise that data itself showed), and a left/right
     * pair of clips authored as a mirror of one another is the ordinary case, not an assumption
     * unique to this pair. If a live capture of this specific clip ever shows it isn't a clean
     * mirror, replace this with its own measured path the same way {@link #BITE_RIGHT} replaced its
     * own first estimate -- don't just nudge this one by eye.
     */
    public static final AttackProfile BITE_LEFT = new AttackProfile(
            Rathian.ATTACK_BITE_LEFT,
            BITE_RIGHT.windupEnd(), BITE_RIGHT.activeStart(), BITE_RIGHT.activeEnd(), BITE_RIGHT.actionEnd(),
            BITE_RIGHT.cooldown(), BITE_RIGHT.strikes(), BITE_RIGHT.volumeSize(), BITE_RIGHT.damage(),
            BITE_RIGHT.aimOffsetDeg(), BITE_RIGHT.lungeSpeed(), BITE_RIGHT.minRange(), BITE_RIGHT.maxRange(),
            new double[][][] {{
                    {19, 0.00D, 4.48D, 5.52D},
                    {22, 0.00D, 2.51D, 5.48D},
                    {25, 0.00D, 1.38D, 4.26D},
                    {28, 0.00D, 0.91D, 4.37D},
            }});

    private static final int REPATH_INTERVAL = 10;

    /** Same tuning as {@link GreatIzuchiCombatGoal}'s identical constants; see there for why. */
    private static final float WINDUP_TURN_RATE = 9.0F;
    private static final double WINDUP_DAMPING = 0.35D;
    private static final int LUNGE_RAMP_TICKS = 6;

    /** Every attack this species can choose from. {@link #chooseAttack} discourages repeating
     * whichever was used last, so a fight alternates bite angles rather than always picking the
     * same one -- distance-influenced, never guaranteed, the same shape as
     * {@link GreatIzuchiCombatGoal}'s selection. */
    private static final AttackProfile[] ALL = {BITE_RIGHT, BITE_LEFT};

    private final Rathian monster;

    /** Keys of {victim, strike window} already struck by the current action; see
     * {@link GreatIzuchiCombatGoal}'s identical field for why this is bounded and cleared per action. */
    private final List<Long> hitThisAction = new ArrayList<>();

    private int repathCooldown;
    private boolean attacking;
    private AttackProfile current;
    /** Last attack chosen, so the selector can prefer variety over repetition once there is more
     * than one candidate; see {@link #chooseAttack} and {@link GreatIzuchiCombatGoal}'s identical
     * field for why this alone isn't enough to guarantee anything, only to discourage repeats. */
    private AttackProfile previous;
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
            this.monster.attackCooldown = this.current != null ? this.current.cooldown() : 25;
        }
        this.current = null;
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

        if (this.monster.attackCooldown <= 0 && this.monster.hasLineOfSight(target)) {
            AttackProfile chosen = chooseAttack(distance);
            if (chosen != null) {
                this.monster.getNavigation().stop();
                beginAttack(chosen, target, distance);
                return;
            }
        }

        // Stays at whatever distance an attack can actually reach from, rather than closing to
        // touching range and then reaching backward for the bite: only approaches while genuinely
        // too far for anything, and only up to the point that changes.
        if (distance > furthestRange()) {
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

    /** The furthest any candidate attack can reach from, so the approach logic stops once
     * something is in range rather than assuming one specific profile's own maxRange. */
    private static double furthestRange() {
        double max = 0.0D;
        for (AttackProfile profile : ALL) {
            max = Math.max(max, profile.maxRange());
        }
        return max;
    }

    /**
     * Picks one attack: filters candidates by range, discourages repeating the last one, breaks
     * remaining ties randomly -- the same shape as {@link GreatIzuchiCombatGoal#chooseAttack}, so
     * distance can influence which bite angle gets picked without ever guaranteeing the same choice
     * at the same distance every time.
     */
    private AttackProfile chooseAttack(double distance) {
        List<AttackProfile> candidates = new ArrayList<>();
        for (AttackProfile profile : ALL) {
            if (profile.inRange(distance)) {
                candidates.add(profile);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() > 1) {
            candidates.remove(this.previous);
        }
        return candidates.get(this.monster.getRandom().nextInt(candidates.size()));
    }

    private void beginAttack(AttackProfile profile, LivingEntity target, double distance) {
        this.attacking = true;
        this.current = profile;
        this.previous = profile;
        this.hitThisAction.clear();
        this.lungeDirection = Vec3.ZERO;
        this.monster.setAggressive(true);
        this.monster.getNavigation().stop();
        this.monster.beginAttack(profile.id());
        debug("attack start: entity={} id={} seq={} target={} distance={}",
                this.monster.getId(), profile.id(), this.monster.getActionSequence(),
                target.getName().getString(), String.format("%.2f", distance));
    }

    private void debug(String message, Object... args) {
        if (MHNWConfig.DEBUG_COMBAT.get()) {
            MHNW.LOG.info("[rathian] " + message, args);
        }
    }

    private void tickAttack() {
        AttackProfile profile = this.current;
        int age = this.monster.getAttackAge();

        if (profile == null || age < 0 || age > profile.actionEnd()) {
            debug("attack end: entity={} seq={} age={} victims={}",
                    this.monster.getId(), this.monster.getActionSequence(), age,
                    this.hitThisAction.size());
            this.attacking = false;
            this.monster.endAttack();
            this.monster.attackCooldown = profile != null ? profile.cooldown() : 25;
            this.current = null;
            this.monster.setCommittedBodyYaw(null);
            this.lungeDirection = Vec3.ZERO;
            this.hitThisAction.clear();
            this.monster.setAggressive(false);
            return;
        }

        this.monster.getNavigation().stop();

        LivingEntity target = this.monster.getTarget();
        if (age <= profile.windupEnd()) {
            if (target != null) {
                this.monster.getLookControl().setLookAt(target, 20.0F, 20.0F);
                aimAt(target);
            }
            plantForWindup();
            return;
        }

        if (age >= profile.activeStart() && age <= profile.activeEnd()) {
            lunge(profile, age, target);
            applyContact(profile, age);
        }
    }

    private void plantForWindup() {
        Vec3 velocity = this.monster.getDeltaMovement();
        this.monster.setDeltaMovement(
                velocity.x * WINDUP_DAMPING, velocity.y, velocity.z * WINDUP_DAMPING);
    }

    /** Same commit-once-then-hold contract as {@link GreatIzuchiCombatGoal#lunge}: aimed once on the
     * first active tick, not re-aimed at a target that moves after the strike is under way (rule 6). */
    private void lunge(AttackProfile profile, int age, LivingEntity target) {
        if (age == profile.activeStart() && target != null) {
            Vec3 toTarget = new Vec3(
                    target.getX() - this.monster.getX(), 0.0D, target.getZ() - this.monster.getZ());
            this.lungeDirection = toTarget.lengthSqr() > 1.0E-4D ? toTarget.normalize() : Vec3.ZERO;
        }
        if (this.lungeDirection.lengthSqr() <= 0.0D) {
            return;
        }
        double easeIn = Math.min(1.0D, (age - profile.activeStart() + 1.0D) / LUNGE_RAMP_TICKS);
        double speed = profile.lungeSpeed() * easeIn;
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

    /** Resolves a synchronized attack id back to its profile, or null; same role as
     * {@link AttackProfile#byId} but scoped to this species' own {@link #ALL} so a future id never
     * collides with Great Izuchi's identically-numbered ids in that shared lookup. Public so the
     * developer overlay can look up the active profile's window generically instead of hardcoding
     * one attack, the same way it already does for Great Izuchi via {@link AttackProfile#byId}. */
    public static AttackProfile byId(byte id) {
        for (AttackProfile profile : ALL) {
            if (profile.id() == id) {
                return profile;
            }
        }
        return null;
    }

    /** Same reason this is static and public as {@link GreatIzuchiCombatGoal#attackVolumes}: the
     * developer overlay draws the exact geometry the server hits with, not a second approximation. */
    public static AABB[] attackVolumes(Rathian monster, int age) {
        AttackProfile profile = byId(monster.getAttackId());
        if (profile == null) {
            return new AABB[0];
        }
        double[] local = profile.limbLocalAt(0, age);
        Vec3 centre = monster.localToWorld(local[0], local[1], local[2]);
        return new AABB[] {AABB.ofSize(centre, profile.volumeSize(), profile.volumeSize(), profile.volumeSize())};
    }

    private void applyContact(AttackProfile profile, int age) {
        if (this.monster.level().isClientSide) {
            return;
        }
        for (AABB volume : attackVolumes(this.monster, age)) {
            applyContactIn(profile, age, volume);
        }
    }

    private void applyContactIn(AttackProfile profile, int age, AABB volume) {
        for (Entity candidate : this.monster.level().getEntities(this.monster, volume)) {
            Entity resolved = candidate instanceof PartEntity<?> part ? part.getParent() : candidate;
            if (!(resolved instanceof LivingEntity victim)) {
                continue;
            }
            if (victim == this.monster || victim.is(this.monster) || !victim.isAlive()) {
                continue;
            }
            long strikeKey = ((long) victim.getId() << 8) | profile.strikeIndexAt(age);
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
                    age, profile.strikeIndexAt(age), victim.getName().getString(), damage,
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
