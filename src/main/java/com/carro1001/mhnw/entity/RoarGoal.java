package com.carro1001.mhnw.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * The MHW-style opening roar: a large monster roars once when it first engages a target, not again
 * for the rest of that fight, and only roars again once it has genuinely disengaged (no target) for
 * a real stretch of time -- not the next tick a target flickers away and back (a dodge, a brief
 * line-of-sight loss).
 *
 * <p>Generic over any {@link Mob} that also implements {@link Roarable} (see that interface's own
 * doc for why this is a small interface, not a shared base class). This goal owns only the
 * engage/re-arm state machine and freezing the monster in place while the clip plays; each species
 * keeps its own synced countdown field, clip length, and animation wiring.
 *
 * <p>Priority must sit above whatever goal owns combat movement/look (vanilla's {@code GoalSelector}
 * gives flag ownership to the highest-priority goal claiming it), so a roar genuinely freezes a
 * fight rather than getting silently skipped while the attack goal keeps moving/swinging underneath
 * it.
 */
public class RoarGoal<T extends Mob & Roarable> extends Goal {

    /** How long with no target before the next engagement roars again, not just the next tick a
     * target flickers away and back. 100 ticks = 5s; provisional, not yet tuned live. */
    private static final int DISENGAGE_TICKS = 100;

    /** Fraction of velocity kept each tick while roaring; same idea as the attack goals' own windup
     * damping, just held for the whole clip instead of a few ticks. */
    private static final double DAMPING = 0.2D;

    private final T monster;
    /** Game time the target was first noticed missing, or -1 while it has one. Real elapsed time,
     * deliberately not a tick counter incremented once per {@link #canUse()} call: {@code Mob}'s
     * own {@code serverAiStep} only polls a non-running goal's {@code canUse()} every *other* tick,
     * so a call-counter would silently take twice as long (10s, not 5) to re-arm. */
    private long noTargetSince = -1L;

    public RoarGoal(T monster) {
        this.monster = monster;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /** Also where the re-arm timer lives, polled here for the same reason {@link #noTargetSince}'s
     * own doc explains. */
    @Override
    public boolean canUse() {
        LivingEntity target = this.monster.getTarget();
        long now = this.monster.level().getGameTime();
        if (target == null) {
            if (this.noTargetSince < 0L) {
                this.noTargetSince = now;
            } else if (now - this.noTargetSince >= DISENGAGE_TICKS) {
                this.monster.setRoaredThisEngagement(false);
            }
            return false;
        }
        this.noTargetSince = -1L;
        return !this.monster.hasRoaredThisEngagement() && this.monster.getRoarTicks() <= 0;
    }

    @Override
    public boolean canContinueToUse() {
        return this.monster.getRoarTicks() > 0;
    }

    @Override
    public boolean isInterruptable() {
        return false;
    }

    @Override
    public void start() {
        this.monster.setRoaredThisEngagement(true);
        this.monster.setRoarTicks(this.monster.roarDurationTicks());
        this.monster.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity target = this.monster.getTarget();
        if (target != null) {
            this.monster.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
        Vec3 velocity = this.monster.getDeltaMovement();
        this.monster.setDeltaMovement(velocity.x * DAMPING, velocity.y, velocity.z * DAMPING);
        this.monster.setRoarTicks(this.monster.getRoarTicks() - 1);
    }

    @Override
    public void stop() {
        this.monster.setRoarTicks(0);
    }
}
