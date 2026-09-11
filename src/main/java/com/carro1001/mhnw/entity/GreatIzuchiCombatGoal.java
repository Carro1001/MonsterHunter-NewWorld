package com.carro1001.mhnw.entity;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.MHNWConfig;
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
 * orientation, and the scratch attack timeline (handoff section 4.3).
 *
 * <p>No other goal, no animation callback and no client code may start this attack or deal its
 * damage. The phase is derived from a single number, the action age, rather than stored in several
 * counters that could contradict one another.
 *
 * <h2>Calibrated scratch timeline</h2>
 * {@code animation.great_izuchi.attack_scratch} is 3.25 s = 65 ticks at normal playback. The
 * boundaries below were measured from the asset, not guessed from the file name: the clawHitbox
 * bone was solved through the clip frame by frame and its speed and position tabulated.
 *
 * <pre>
 *   ticks  0-17   WINDUP    claw rears up from 0.88 to 2.37 blocks, speed &lt; 0.18 blocks/tick
 *   ticks 18-47   ACTIVE    the multi-motion sweep, speed 0.2 - 2.1 blocks/tick
 *   ticks 48-64   RECOVERY  settles back to rest, speed decaying 0.09 -&gt; 0.03
 * </pre>
 *
 * The scratch is visibly several slashes. Per the handoff, one damage application per action per
 * victim is the accepted provisional design; the active window is evaluated every tick so contact
 * is wherever the animation actually put the claw, but each victim is recorded and skipped after
 * its first hit.
 *
 * <p>{@link #CLAW_PATH} is a short authored offset path sampled from that same solve, in the local
 * (left, up, forward) frame documented on {@link GreatIzuchi}, linearly interpolated between keys.
 */
public class GreatIzuchiCombatGoal extends Goal {

    /**
     * Provisional damage, used as the default value of the ATTACK_DAMAGE attribute. The attribute
     * is the single source of truth at runtime; this constant only seeds it.
     */
    public static final double SCRATCH_DAMAGE = 8.0D;

    /** Last tick of the telegraph. Facing locks at the end of this phase. */
    public static final int WINDUP_END = 17;
    /** First tick on which the claw volume is evaluated. */
    public static final int ACTIVE_START = 18;
    /** Last tick on which the claw volume is evaluated. */
    public static final int ACTIVE_END = 47;
    /** Total clip length; the action ends after this tick. */
    public static final int ACTION_END = 64;
    /** Ticks after recovery before another attack may start. */
    public static final int COOLDOWN = 30;

    /** Edge length of the cubic claw volume. The solved hand mesh measures 0.75 to 1.06 across. */
    public static final double CLAW_SIZE = 0.9D;

    /**
     * Distance at which the attack may be started, measured from the monster position to the
     * nearest point of the victim bounding box.
     *
     * <p>Derived from the measured claw path rather than picked by feel. The hand crosses the
     * centre line at about 1.78 blocks forward, and the volume is 0.9 across, so the reliably
     * striking face is near 2.2. Closing to 1.9 leaves margin on both sides of that, since
     * approach stops as soon as this threshold is met and the monster then commits.
     */
    public static final double REACH = 1.9D;

    /**
     * Measured {@code right_hand} positions, {tick, left, up, forward} in blocks, across the
     * active window, logged from the running game by {@code client/BoneProbe}.
     *
     * <p>An offline solve of this path was wrong by about 1.4 blocks and put the claw on the wrong
     * side of the body, which is why the attack connected 3 times in 10. See the note on
     * {@link GreatIzuchi} for why the offline solve is untrustworthy for the arm chain.
     *
     * <p>Note the shape this reveals: the hand sits about half a block to a block and a half to the
     * monster's right for most of the swing, crossing the centre line only around ticks 34 and 44.
     * The scratch is a cross-body swipe, not a straight lunge, so the attack genuinely does miss a
     * target that is not where the arc passes. That is the animation being honest, not a bug.
     */
    private static final double[][] CLAW_PATH = {
            {18, -1.07D, 1.29D, 1.96D},
            {19, -1.10D, 1.31D, 1.97D},
            {25, -1.28D, 1.83D, 1.98D},
            {30, -0.94D, 2.06D, 2.31D},
            {34, -0.12D, 1.33D, 1.78D},
            {39, -1.24D, 3.13D, 1.95D},
            {44, -0.46D, 1.44D, 1.77D},
            {47, -1.29D, 1.29D, 0.87D},
    };

    private static final int REPATH_INTERVAL = 10;

    private final GreatIzuchi monster;
    /** Entity ids already hit by the current action. Cleared per action, so it cannot grow. */
    private final List<Integer> hitThisAction = new ArrayList<>();

    private int repathCooldown;
    private boolean attacking;

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
        // An action that is already committed finishes even if the target is lost, but it cannot
        // acquire a new victim, because the claw volume is a fixed body-local path (rule 6).
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
            this.monster.attackCooldown = COOLDOWN;
        }
        this.hitThisAction.clear();
        this.monster.getNavigation().stop();
        this.monster.setAggressive(false);
    }

    @Override
    public void tick() {
        if (!this.monster.isAlive()) {
            // Death overrides every phase, immediately (rule 6).
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
        if (distance > REACH) {
            if (--this.repathCooldown <= 0) {
                this.repathCooldown = REPATH_INTERVAL;
                // Bounded re-pathing: a failed path abandons the approach for this interval
                // instead of recalculating every tick (handoff section 4.5, A10).
                boolean pathed = this.monster.getNavigation().moveTo(target, 1.0D);
                if (!pathed) {
                    debug("approach: no path to {} at {} blocks", target.getName().getString(), distance);
                }
            }
            return;
        }

        this.monster.getNavigation().stop();
        if (this.monster.attackCooldown > 0) {
            return;
        }
        if (!this.monster.hasLineOfSight(target)) {
            debug("attack rejected: no line of sight to {}", target.getName().getString());
            return;
        }
        beginAttack(target, distance);
    }

    private void beginAttack(LivingEntity target, double distance) {
        this.attacking = true;
        this.hitThisAction.clear();
        this.monster.setAggressive(true);
        this.monster.getNavigation().stop();
        this.monster.beginAttack(GreatIzuchi.ATTACK_SCRATCH);
        debug("attack start: id={} seq={} target={} distance={}",
                GreatIzuchi.ATTACK_SCRATCH, this.monster.getActionSequence(),
                target.getName().getString(), String.format("%.2f", distance));
    }

    /**
     * Sparse development diagnostics: transitions and contact decisions only, never per tick.
     * Off unless the server config enables it (handoff P2).
     */
    private void debug(String message, Object... args) {
        if (MHNWConfig.DEBUG_COMBAT.get()) {
            MHNW.LOG.info("[great_izuchi] " + message, args);
        }
    }

    private void tickAttack() {
        int age = this.monster.getAttackAge();

        if (age < 0 || age > ACTION_END) {
            debug("attack end: seq={} age={} victims={}",
                    this.monster.getActionSequence(), age, this.hitThisAction.size());
            this.attacking = false;
            this.monster.endAttack();
            this.monster.attackCooldown = COOLDOWN;
            this.hitThisAction.clear();
            this.monster.setAggressive(false);
            return;
        }

        this.monster.getNavigation().stop();

        LivingEntity target = this.monster.getTarget();
        if (age <= WINDUP_END && target != null) {
            // The telegraph tracks; after it, facing is locked so a committed swing cannot snap
            // around behind the monster to follow a dodging player (rule 2).
            this.monster.getLookControl().setLookAt(target, 20.0F, 20.0F);
        }

        if (age >= ACTIVE_START && age <= ACTIVE_END) {
            applyContact(age);
        }
    }

    /** Interpolates the authored claw path at this action age, in the local frame. */
    private static double[] clawLocalAt(int age) {
        double[] first = CLAW_PATH[0];
        if (age <= first[0]) {
            return new double[] {first[1], first[2], first[3]};
        }
        double[] last = CLAW_PATH[CLAW_PATH.length - 1];
        if (age >= last[0]) {
            return new double[] {last[1], last[2], last[3]};
        }
        for (int i = 0; i < CLAW_PATH.length - 1; i++) {
            double[] a = CLAW_PATH[i];
            double[] b = CLAW_PATH[i + 1];
            if (age >= a[0] && age <= b[0]) {
                double f = (age - a[0]) / (b[0] - a[0]);
                return new double[] {
                        a[1] + (b[1] - a[1]) * f,
                        a[2] + (b[2] - a[2]) * f,
                        a[3] + (b[3] - a[3]) * f,
                };
            }
        }
        return new double[] {last[1], last[2], last[3]};
    }

    /** World-space claw volume for this action age. Exposed so tests and the overlay agree. */
    public AABB clawVolume(int age) {
        double[] local = clawLocalAt(age);
        Vec3 centre = this.monster.localToWorld(local[0], local[1], local[2]);
        return AABB.ofSize(centre, CLAW_SIZE, CLAW_SIZE, CLAW_SIZE);
    }

    private void applyContact(int age) {
        if (this.monster.level().isClientSide) {
            return;
        }
        AABB volume = clawVolume(age);

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
            if (this.hitThisAction.contains(victim.getId())) {
                continue;
            }
            if (!victim.getBoundingBox().intersects(volume)) {
                continue;
            }
            // A melee swing must not reach through a wall (rule 3).
            if (!this.monster.hasLineOfSight(victim)) {
                debug("contact rejected at age={}: {} is behind cover", age, victim.getName().getString());
                continue;
            }
            this.hitThisAction.add(victim.getId());
            float damage = (float) this.monster.getAttributeValue(Attributes.ATTACK_DAMAGE);
            victim.hurt(this.monster.damageSources().mobAttack(this.monster), damage);
            debug("contact accepted at age={}: {} for {} damage (seq={})",
                    age, victim.getName().getString(), damage, this.monster.getActionSequence());
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
