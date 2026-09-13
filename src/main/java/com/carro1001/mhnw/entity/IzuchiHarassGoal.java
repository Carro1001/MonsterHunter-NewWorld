package com.carro1001.mhnw.entity;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.MHNWConfig;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

/**
 * Small-Izuchi pack pressure: hang back, circle, pick at the target, leave.
 *
 * <p>Replaces the vanilla {@link net.minecraft.world.entity.ai.goal.MeleeAttackGoal} this species
 * used to run, which closed to melee and stayed there -- four escorts doing that at once read as
 * four constant rushers rather than a pack. The three phases are deliberately bounded so the
 * behaviour is legible and cannot wedge:
 *
 * <pre>
 *   CIRCLE  : hold 5-8 blocks off, repathing to a fresh point every {@value #REPATH_TICKS} ticks.
 *             After a randomized 40-80 tick opportunity window, dart -- if the pack slot is free.
 *   DART    : close at 1.25x for at most {@value #DART_MAX_TICKS} ticks.
 *   ATTACK  : plant, play the recovered tail swipe, evaluate its swept volume once per tick.
 *   RETREAT : back out to the 6-8 block band for 20-40 ticks, then circle again.
 * </pre>
 *
 * <p>The attack is server-owned: the animation follows synchronized action age, while damage comes
 * only from {@link #attackVolumes} during {@value #ACTIVE_START}..{@value #ACTIVE_END}. The client
 * overlay calls the same method, so its green boxes cannot disagree with server contact.
 *
 * <h2>The one-attack-turn rule, without a pack leader</h2>
 * At most one Izuchi within {@value #PACK_RADIUS} blocks may be approaching or swiping. That is enforced by
 * asking the neighbours directly rather than by a leader-owned service or a shared registry:
 * {@link Izuchi#isTakingAttackTurn()} reads this goal's transient phase, and the scan ignores any
 * neighbour that is not alive. Ignoring the dead is what keeps the slot from being held forever by
 * a member killed mid-dart -- once a mob dies vanilla stops ticking its goals entirely, so nothing
 * would ever run to clear the flag (see CLAUDE.md on dead entities and goals).
 *
 * <p>Everything this goal owns is transient. A reload, a lost target, death or a peaceful
 * transition leaves no half-finished dart to resume: {@link #stop()} clears the phase and the
 * navigation, and the field itself is never saved.
 *
 * <p>The timings below are initial tuning values from the R1 packet, not measurements. Adjust them
 * with evidence and record it; removing the pauses outright is a product change, not tuning.
 */
public class IzuchiHarassGoal extends Goal {

    public enum Phase { CIRCLE, DART, ATTACK, RETREAT }

    static final double CIRCLE_MIN = 5.0D;
    static final double CIRCLE_MAX = 8.0D;
    static final double RETREAT_MIN = 6.0D;
    static final double RETREAT_MAX = 8.0D;
    static final int WINDOW_MIN_TICKS = 40;
    static final int WINDOW_MAX_TICKS = 80;
    public static final int DART_MAX_TICKS = 30;
    static final int RETREAT_MIN_TICKS = 20;
    static final int RETREAT_MAX_TICKS = 40;
    static final int REPATH_TICKS = 40;
    static final double CIRCLE_SPEED = 1.0D;
    static final double DART_SPEED = 1.25D;
    static final double PACK_RADIUS = 12.0D;
    /** Attempts to find a reachable ring point before giving up for this repath. */
    private static final int RING_ATTEMPTS = 6;

    /** Preserved brain-branch timing: the clip is 2.375 seconds (48 ticks). */
    public static final int WINDUP_END = 35;
    public static final int ACTIVE_START = 36;
    public static final int ACTIVE_END = 47;
    public static final int ACTION_END = 47;

    private static final double VOLUME_SIZE = 0.9D;

    // Temporary capture path; replace with live BoneProbe measurements when they are available.
    private static final double[][][] TAIL_PATHS = {
            {{36, -1.2D, 0.9D, -0.8D}, {47, 0.8D, 0.8D, 1.2D}},
            {{36, -2.0D, 0.7D, -1.2D}, {47, 1.4D, 0.7D, 1.8D}},
    };

    private final Izuchi mob;
    private int phaseTicks;
    private int windowTicks;
    private int repathTicks;
    private int retreatTicks;
    private boolean hitThisDart;
    private final Set<Integer> hitThisAttack = new HashSet<>();

    public IzuchiHarassGoal(Izuchi mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    /**
     * Real elapsed ticks matter here -- the opportunity window, the dart deadline and the retreat
     * are all counted in them. Without this override vanilla ticks a running goal only every other
     * real tick, which would silently double every duration above. See CLAUDE.md's note on
     * {@code RoarGoal} making exactly that mistake.
     */
    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {
        return canHarass(this.mob, this.mob.getTarget(), this.mob.level().getDifficulty());
    }

    @Override
    public boolean canContinueToUse() {
        return this.mob.getAttackId() != Izuchi.ATTACK_NONE || canUse();
    }

    @Override
    public boolean isInterruptable() {
        return this.mob.getAttackId() == Izuchi.ATTACK_NONE;
    }

    /**
     * The whole precondition, as a function of its inputs so a test can state the difficulty
     * instead of changing the world's.
     *
     * <p>The peaceful clause is not redundant with vanilla's own handling. Peaceful despawns hostile
     * mobs through {@code Mob.checkDespawn}, but only those whose {@code shouldDespawnInPeaceful()}
     * says so, and {@link Izuchi} deliberately returns false there -- so a world switched to
     * peaceful mid-fight keeps the Izuchi, keeps its target, and without this would keep circling
     * and darting at it. Returning false here makes the goal stop, and {@link #stop()} is what
     * clears the phase and the navigation.
     */
    public static boolean canHarass(Izuchi mob, LivingEntity target, Difficulty difficulty) {
        return target != null && target.isAlive() && mob.isAlive() && difficulty != Difficulty.PEACEFUL;
    }

    @Override
    public void start() {
        enterCircle();
    }

    @Override
    public void stop() {
        this.mob.setHarassPhase(null);
        this.mob.setAggressive(false);
        this.mob.getNavigation().stop();
        this.mob.endAttack();
        this.phaseTicks = 0;
        this.hitThisDart = false;
        this.hitThisAttack.clear();
    }

    @Override
    public void tick() {
        this.phaseTicks++;

        Phase phase = this.mob.harassPhase();
        if (phase == null) {
            enterCircle();
            return;
        }
        LivingEntity target = this.mob.getTarget();
        if (phase == Phase.ATTACK) {
            tickAttack(target);
            return;
        }
        if (target == null) {
            return;
        }
        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        switch (phase) {
            case CIRCLE -> tickCircle(target);
            case DART -> tickDart(target);
            case ATTACK -> throw new IllegalStateException("handled above");
            case RETREAT -> tickRetreat(target);
        }
    }

    private void tickCircle(LivingEntity target) {
        this.windowTicks--;
        this.repathTicks--;
        if (this.windowTicks <= 0 && packSlotFree()) {
            enterDart();
            return;
        }
        if (this.repathTicks <= 0 || this.mob.getNavigation().isDone()) {
            moveToRing(target, CIRCLE_MIN, CIRCLE_MAX, CIRCLE_SPEED, false);
            this.repathTicks = REPATH_TICKS;
        }
    }

    private void tickDart(LivingEntity target) {
        if (!this.hitThisDart && this.mob.isWithinMeleeAttackRange(target)) {
            this.hitThisDart = true;
            enterAttack();
            return;
        }
        if (this.phaseTicks >= DART_MAX_TICKS) {
            // Bounded whether it connected, missed, or never found a path: the slot is released
            // here and nowhere else.
            enterRetreat(target);
            return;
        }
        if (this.mob.getNavigation().isDone() || this.phaseTicks % 4 == 0) {
            this.mob.getNavigation().moveTo(target, DART_SPEED);
        }
    }

    private void tickRetreat(LivingEntity target) {
        if (this.phaseTicks >= this.retreatTicks) {
            enterCircle();
            return;
        }
        if (this.mob.getNavigation().isDone()) {
            moveToRing(target, RETREAT_MIN, RETREAT_MAX, CIRCLE_SPEED, true);
        }
    }

    private void enterCircle() {
        this.mob.setHarassPhase(Phase.CIRCLE);
        this.mob.setAggressive(false);
        this.phaseTicks = 0;
        this.repathTicks = 0;
        this.hitThisDart = false;
        this.windowTicks = WINDOW_MIN_TICKS
                + this.mob.getRandom().nextInt(WINDOW_MAX_TICKS - WINDOW_MIN_TICKS + 1);
    }

    private void enterDart() {
        this.mob.setHarassPhase(Phase.DART);
        this.mob.setAggressive(true);
        this.phaseTicks = 0;
        this.hitThisDart = false;
    }

    private void enterAttack() {
        this.mob.setHarassPhase(Phase.ATTACK);
        this.mob.setAggressive(true);
        this.mob.getNavigation().stop();
        this.phaseTicks = 0;
        this.hitThisAttack.clear();
        this.mob.beginTailSwipe();
        debug("attack start: entity={} seq={}", this.mob.getId(), this.mob.getActionSequence());
    }

    private void tickAttack(LivingEntity target) {
        int age = this.mob.getAttackAge();
        if (age < 0 || age > ACTION_END) {
            debug("attack end: entity={} seq={} age={} victims={}", this.mob.getId(),
                    this.mob.getActionSequence(), age, this.hitThisAttack.size());
            this.mob.endAttack();
            this.hitThisAttack.clear();
            if (target != null && target.isAlive()) {
                enterRetreat(target);
            } else {
                enterCircle();
            }
            return;
        }

        this.mob.getNavigation().stop();
        Vec3 velocity = this.mob.getDeltaMovement();
        this.mob.setDeltaMovement(velocity.x * 0.35D, velocity.y, velocity.z * 0.35D);
        if (age >= ACTIVE_START && age <= ACTIVE_END) {
            applyContact(age);
        }
    }

    public static AABB[] attackVolumes(Izuchi mob, int age) {
        if (mob.getAttackId() != Izuchi.ATTACK_TAIL_SWIPE) {
            return new AABB[0];
        }
        AABB[] volumes = new AABB[TAIL_PATHS.length];
        for (int i = 0; i < volumes.length; i++) {
            double[] local = interpolate(TAIL_PATHS[i], age);
            volumes[i] = AABB.ofSize(mob.localToWorld(local[0], local[1], local[2]),
                    VOLUME_SIZE, VOLUME_SIZE, VOLUME_SIZE);
        }
        return volumes;
    }

    private static double[] interpolate(double[][] path, int age) {
        if (age <= path[0][0]) {
            return new double[] {path[0][1], path[0][2], path[0][3]};
        }
        for (int i = 0; i < path.length - 1; i++) {
            double[] a = path[i];
            double[] b = path[i + 1];
            if (age <= b[0]) {
                double f = (age - a[0]) / (b[0] - a[0]);
                return new double[] {
                        a[1] + (b[1] - a[1]) * f,
                        a[2] + (b[2] - a[2]) * f,
                        a[3] + (b[3] - a[3]) * f,
                };
            }
        }
        double[] last = path[path.length - 1];
        return new double[] {last[1], last[2], last[3]};
    }

    private void applyContact(int age) {
        if (this.mob.level().isClientSide) {
            return;
        }
        for (AABB volume : attackVolumes(this.mob, age)) {
            for (Entity candidate : this.mob.level().getEntities(this.mob, volume)) {
                Entity resolved = candidate instanceof PartEntity<?> part ? part.getParent() : candidate;
                if (!(resolved instanceof LivingEntity victim) || victim == this.mob
                        || !victim.isAlive() || this.hitThisAttack.contains(victim.getId())
                        || !victim.getBoundingBox().intersects(volume)
                        || !this.mob.hasLineOfSight(victim)) {
                    continue;
                }
                this.hitThisAttack.add(victim.getId());
                float damage = (float) this.mob.getAttributeValue(Attributes.ATTACK_DAMAGE);
                victim.hurt(this.mob.damageSources().mobAttack(this.mob), damage);
                debug("contact accepted at age={}: {} for {} damage (seq={})", age,
                        victim.getName().getString(), damage, this.mob.getActionSequence());
            }
        }
    }

    private void debug(String message, Object... args) {
        if (MHNWConfig.DEBUG_COMBAT.get()) {
            MHNW.LOG.info("[izuchi] " + message, args);
        }
    }

    private void enterRetreat(LivingEntity target) {
        this.mob.setHarassPhase(Phase.RETREAT);
        this.mob.setAggressive(false);
        this.phaseTicks = 0;
        this.retreatTicks = RETREAT_MIN_TICKS
                + this.mob.getRandom().nextInt(RETREAT_MAX_TICKS - RETREAT_MIN_TICKS + 1);
        moveToRing(target, RETREAT_MIN, RETREAT_MAX, CIRCLE_SPEED, true);
    }

    /** True when no living Izuchi nearby already owns the approach-and-swipe turn. */
    private boolean packSlotFree() {
        for (Izuchi other : this.mob.level().getEntitiesOfClass(
                Izuchi.class, this.mob.getBoundingBox().inflate(PACK_RADIUS))) {
            if (other != this.mob && other.isAlive() && other.isTakingAttackTurn()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Path to a point on a ring around the target.
     *
     * <p>Candidate heights come from the mob's own Y rather than a heightmap lookup, deliberately:
     * a heightmap answers "the top of the world column", which inside the GameTest arena is above
     * its barrier lid, and on real terrain would happily suggest a treetop. Letting the navigator
     * snap the request to a walkable node is both simpler and correct in both places.
     *
     * @param away when true, bias the point to the side of the target the mob is already on, so a
     *             retreat reads as backing off rather than as another random hop
     */
    private boolean moveToRing(LivingEntity target, double min, double max, double speed, boolean away) {
        Vec3 centre = target.position();
        double bias = away
                ? Math.atan2(this.mob.getZ() - centre.z, this.mob.getX() - centre.x)
                : 0.0D;
        for (int attempt = 0; attempt < RING_ATTEMPTS; attempt++) {
            double angle = away
                    ? bias + (this.mob.getRandom().nextDouble() - 0.5D) * (Math.PI / 1.5D)
                    : this.mob.getRandom().nextDouble() * Math.PI * 2.0D;
            double radius = min + this.mob.getRandom().nextDouble() * (max - min);
            double x = centre.x + Math.cos(angle) * radius;
            double z = centre.z + Math.sin(angle) * radius;
            if (this.mob.getNavigation().moveTo(x, this.mob.getY(), z, speed)) {
                return true;
            }
        }
        return false;
    }
}
