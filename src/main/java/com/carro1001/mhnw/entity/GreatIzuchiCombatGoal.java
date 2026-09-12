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
 * The one and only owner of Great Izuchi combat: target selection follow-through, approach,
 * orientation, attack choice, and the attack timeline (handoff section 4.3).
 *
 * <p>No other goal, no animation callback and no client code may start an attack or deal its
 * damage. The phase is derived from a single number, the action age, rather than stored in several
 * counters that can contradict one another. Everything that differs between attacks lives in
 * {@link AttackProfile}; this class is the machinery that runs one.
 *
 * <h2>Phases</h2>
 * <pre>
 *   0 .. windupEnd      WINDUP    telegraph; body turns to aim, movement damps to a stop
 *   activeStart..End    ACTIVE    volume evaluated every tick; monster paces forward
 *   .. actionEnd        RECOVERY  committed, no contact, movement unforced
 * </pre>
 * Death overrides all of them.
 */
public class GreatIzuchiCombatGoal extends Goal {

    /** Seeds the entity's attack damage attribute. Per-attack scaling lives in the profile. */
    public static final double SCRATCH_DAMAGE = 2.5D;

    /**
     * How close the monster tries to get when nothing is in range yet.
     *
     * <p>Not a gate on attacking. Each attack has its own range band, and the long-reach tail
     * attacks are deliberately usable further out than this, so the monster can open with one
     * while it is still closing. Gating selection on a single global reach made the tail slam,
     * whose band starts at 3.0, permanently unselectable.
     */
    public static final double CLOSE_RANGE = 2.6D;

    private static final int REPATH_INTERVAL = 10;

    /** Degrees per tick the body may turn while winding up. Keeps the telegraph readable. */
    private static final float WINDUP_TURN_RATE = 9.0F;

    /**
     * Horizontal speed retained each windup tick. Low enough that a run-up is gone within about
     * three ticks, so the monster plants and waits out the telegraph rather than drifting.
     */
    private static final double WINDUP_DAMPING = 0.35D;

    /** Ticks spent easing into the pace, so the first active tick is not a jolt. */
    private static final int LUNGE_RAMP_TICKS = 6;

    private final GreatIzuchi monster;

    /**
     * Keys of {victim, strike window} already struck by the current action. Cleared per action, so
     * it is bounded by victims times strikes and cannot grow over a fight.
     */
    private final List<Long> hitThisAction = new ArrayList<>();

    private int repathCooldown;
    private boolean attacking;
    private AttackProfile current;
    /** Last attack chosen, so the selector can prefer variety over repetition. */
    private AttackProfile previous;
    /** Direction of the committed lunge, fixed on the first active tick. */
    private Vec3 lungeDirection = Vec3.ZERO;

    public GreatIzuchiCombatGoal(GreatIzuchi monster) {
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
        // An action already committed finishes even if the target is lost, but it cannot acquire a
        // new victim, because the volume follows a fixed body-local path (rule 6).
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

    /** Every exit path funnels through here: goal stop, target loss, death, removal, reload. */
    @Override
    public void stop() {
        if (this.attacking) {
            this.attacking = false;
            this.monster.endAttack();
            this.monster.attackCooldown = this.current != null ? this.current.cooldown() : 30;
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
            // Death overrides every phase (rule 6) -- though in practice this can't actually fire:
            // vanilla stops ticking every goal at all, permanently, for the whole corpse-hold window
            // once isDeadOrDying() is true (see CLAUDE.md's "opening roar" section for the exact
            // mechanism, found while chasing the identical check in RoarGoal). Kept for parity and
            // because it's harmless either way -- mainAnim() already checks isDeadOrDying() before
            // reading any combat state, so presentation is correct regardless.
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
        // Drives the run animation on clients, which cannot see getTarget().
        this.monster.setAggressive(true);

        double distance = distanceToBox(target);

        // Attack the moment anything is in range, even mid-approach: that is what lets a
        // long-reach tail attack open the engagement instead of only ever trading claw swipes.
        if (this.monster.attackCooldown <= 0 && this.monster.hasLineOfSight(target)) {
            AttackProfile chosen = chooseAttack(distance);
            if (chosen != null) {
                this.monster.getNavigation().stop();
                beginAttack(chosen, target, distance);
                return;
            }
        }

        if (distance > CLOSE_RANGE) {
            if (--this.repathCooldown <= 0) {
                this.repathCooldown = REPATH_INTERVAL;
                // Bounded re-pathing: a failed path abandons the approach for this interval
                // instead of recalculating every tick (handoff section 4.5, A10).
                if (!this.monster.getNavigation().moveTo(target, 1.0D)) {
                    debug("approach: no path to {} at {} blocks",
                            target.getName().getString(), String.format("%.2f", distance));
                }
            }
            return;
        }
        this.monster.getNavigation().stop();
    }

    /**
     * Picks one attack, deliberately.
     *
     * <p>The previous implementation installed three melee goals at the same priority and carried a
     * TODO that it always chose the same one. Selection here is an explicit choice: narrow to the
     * attacks whose range band contains the target, then prefer one that was not used last time so
     * a fight does not become the same swing repeatedly, and break remaining ties randomly.
     */
    private AttackProfile chooseAttack(double distance) {
        List<AttackProfile> candidates = new ArrayList<>();
        for (AttackProfile profile : AttackProfile.all()) {
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

    /**
     * Sparse development diagnostics: transitions and contact decisions only, never per tick.
     * Off unless the config enables it (handoff P2).
     */
    private void debug(String message, Object... args) {
        if (MHNWConfig.DEBUG_COMBAT.get()) {
            MHNW.LOG.info("[great_izuchi] " + message, args);
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
            this.monster.attackCooldown = profile != null ? profile.cooldown() : 30;
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
                // The telegraph tracks, turn-rate limited; after it, facing stays at whatever the
                // windup committed to, so a swing already under way cannot snap around behind the
                // monster to follow a dodging player (rule 2).
                this.monster.getLookControl().setLookAt(target, 20.0F, 20.0F);
                aimArcAt(profile, target);
            }
            plantForWindup();
            return;
        }

        if (age >= profile.activeStart() && age <= profile.activeEnd()) {
            lunge(profile, age, target);
            applyContact(profile, age);
        }
    }

    /** Bleeds off horizontal momentum so the windup reads as planting, not gliding. */
    private void plantForWindup() {
        Vec3 velocity = this.monster.getDeltaMovement();
        this.monster.setDeltaMovement(
                velocity.x * WINDUP_DAMPING, velocity.y, velocity.z * WINDUP_DAMPING);
    }

    /**
     * Drives the monster forward through the swing so the limb carries its weight.
     *
     * <p>The direction is captured once, on the first active tick, and then held. Re-aiming it
     * every tick would let a committed strike home onto a target that has since moved, which the
     * runtime contract forbids (rule 6): once the strike is under way it travels where it was
     * launched, and missing a target that dodged is the correct outcome. The body yaw is likewise
     * locked after windup, so the arc cannot swing onto someone new mid-strike.
     */
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

    /**
     * Turns the body so the measured arc sweeps over the target, instead of beside it.
     *
     * <p>Turn-rate limited so the windup reads as a wind-up rather than a snap, and held in
     * {@link GreatIzuchi#setCommittedBodyYaw} so the vanilla body-rotation control cannot undo it
     * and so the hurtboxes rotate with the same yaw the attack volume does.
     */
    private void aimArcAt(AttackProfile profile, LivingEntity target) {
        double dx = target.getX() - this.monster.getX();
        double dz = target.getZ() - this.monster.getZ();
        float bearing = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float desired = bearing - profile.aimOffsetDeg();

        Float held = this.monster.getCommittedBodyYaw();
        float currentYaw = held != null ? held : this.monster.yBodyRot;
        float step = Mth.clamp(
                Mth.wrapDegrees(desired - currentYaw), -WINDUP_TURN_RATE, WINDUP_TURN_RATE);
        this.monster.setCommittedBodyYaw(Mth.wrapDegrees(currentYaw + step));
    }

    /**
     * World-space damage volume for the monster's current action at this age, or null when it is
     * not attacking.
     *
     * <p>Static and public so the developer overlay draws the very same geometry the server hits
     * with, rather than a second approximation of it that could drift out of agreement.
     */
    public static AABB[] attackVolumes(GreatIzuchi monster, int age) {
        AttackProfile profile = AttackProfile.byId(monster.getAttackId());
        if (profile == null) {
            return new AABB[0];
        }
        AABB[] volumes = new AABB[profile.volumeCount()];
        for (int i = 0; i < volumes.length; i++) {
            double[] local = profile.limbLocalAt(i, age);
            Vec3 centre = monster.localToWorld(local[0], local[1], local[2]);
            volumes[i] = AABB.ofSize(
                    centre, profile.volumeSize(), profile.volumeSize(), profile.volumeSize());
        }
        return volumes;
    }

    private void applyContact(AttackProfile profile, int age) {
        if (this.monster.level().isClientSide) {
            return;
        }
        // A sweeping tail carries a volume at several points along its length, so a victim beside
        // the mid tail is struck even though the tip passes well outside them. The per-strike key
        // below is shared across volumes, so overlapping two of them is still one hit.
        for (AABB volume : attackVolumes(this.monster, age)) {
            applyContactIn(profile, age, volume);
        }
    }

    private void applyContactIn(AttackProfile profile, int age, AABB volume) {
        // Intersect real bounding boxes, not centres. getEntities also returns PartEntity objects,
        // so victims are normalized to their parent before de-duplication (rule 3).
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
            // A melee swing must not reach through a wall (rule 3).
            if (!this.monster.hasLineOfSight(victim)) {
                debug("contact rejected at age={}: {} is behind cover",
                        age, victim.getName().getString());
                continue;
            }
            this.hitThisAction.add(strikeKey);
            float damage = (float) (this.monster.getAttributeValue(Attributes.ATTACK_DAMAGE)
                    * (profile.damage() / SCRATCH_DAMAGE));
            // The reviewed exception to leaving vanilla invulnerability alone. Strikes land about
            // ten ticks apart, inside vanilla's twenty tick window, so without this only the first
            // of them would ever be felt and the multi-hit design would be silent. Scoped by the
            // per-strike key above, so it cannot become per-tick damage.
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
