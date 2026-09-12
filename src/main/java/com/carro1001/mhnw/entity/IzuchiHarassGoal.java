package com.carro1001.mhnw.entity;

import java.util.EnumSet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

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
 *   DART    : close at 1.25x for at most {@value #DART_MAX_TICKS} ticks, land at most one ordinary
 *             melee hit, then leave whether it connected or not.
 *   RETREAT : back out to the 6-8 block band for 20-40 ticks, then circle again.
 * </pre>
 *
 * <p>Damage is still ordinary {@code Mob.doHurtTarget}, not a synchronized attack timeline: this
 * species has no attack clip to time a swing against (see {@link Izuchi}), and inventing one is
 * explicitly not this packet's job. The locomotion clips already in motion are the presentation.
 *
 * <h2>The one-darter rule, without a pack leader</h2>
 * At most one Izuchi within {@value #PACK_RADIUS} blocks may be mid-dart. That is enforced by
 * asking the neighbours directly rather than by a leader-owned service or a shared registry:
 * {@link Izuchi#isDarting()} is a plain transient field this goal sets, and the scan ignores any
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

    public enum Phase { CIRCLE, DART, RETREAT }

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

    private final Izuchi mob;
    private int phaseTicks;
    private int windowTicks;
    private int repathTicks;
    private int retreatTicks;
    private boolean hitThisDart;

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
        LivingEntity target = this.mob.getTarget();
        return target != null && target.isAlive() && this.mob.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
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
        this.phaseTicks = 0;
        this.hitThisDart = false;
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) {
            return;
        }
        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        this.phaseTicks++;

        Phase phase = this.mob.harassPhase();
        if (phase == null) {
            enterCircle();
            return;
        }
        switch (phase) {
            case CIRCLE -> tickCircle(target);
            case DART -> tickDart(target);
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
            this.mob.swing(InteractionHand.MAIN_HAND);
            this.mob.doHurtTarget(target);
            this.hitThisDart = true;
            enterRetreat(target);
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

    private void enterRetreat(LivingEntity target) {
        this.mob.setHarassPhase(Phase.RETREAT);
        this.mob.setAggressive(false);
        this.phaseTicks = 0;
        this.retreatTicks = RETREAT_MIN_TICKS
                + this.mob.getRandom().nextInt(RETREAT_MAX_TICKS - RETREAT_MIN_TICKS + 1);
        moveToRing(target, RETREAT_MIN, RETREAT_MAX, CIRCLE_SPEED, true);
    }

    /** True when no living Izuchi nearby is already mid-dart. */
    private boolean packSlotFree() {
        for (Izuchi other : this.mob.level().getEntitiesOfClass(
                Izuchi.class, this.mob.getBoundingBox().inflate(PACK_RADIUS))) {
            if (other != this.mob && other.isAlive() && other.isDarting()) {
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
