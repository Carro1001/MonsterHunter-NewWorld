package com.carro1001.mhnw.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * The shared shape behind small endemic life that reacts to something getting close or hurting it
 * by telegraphing, then releasing a bounded area effect once, then going quiet for a while: notice,
 * telegraph, release, cooldown. {@link Toad} was the first user; this was deliberately not
 * extracted until {@link Flashbug} became the second, matching the handoff's own instruction to
 * extract a shared base only where an implementation actually repeats, not defensively ahead of it.
 *
 * <p>What is NOT shared, and stays in each subclass: the actual effect. A toad's poison cloud and a
 * flashbug's blinding burst have nothing in common mechanically, so {@link #release()} is the one
 * abstract method with all the species-specific meaning; everything here is bookkeeping around it.
 *
 * <p>Every subclass must override {@link #requiresUpdateEveryTick()}... except it already does,
 * here, once: {@code ToadFuseGoal}'s fuse silently ran at half speed until that was added, because
 * {@code Mob.serverAiStep} only calls a non-tick-required goal's {@code tick()} on roughly half of
 * all ticks (alternating by tick-and-entity-id parity). Centralising the fix here means the next
 * subclass gets it automatically instead of needing to rediscover it.
 */
public abstract class EndemicAreaEffectGoal extends Goal {

    private final PathfinderMob owner;
    private final double triggerRange;
    private final int fuseTicks;
    private final int cooldownTicks;

    private int fuseTicksLeft = -1;
    /**
     * Game time the cooldown ends, not a countdown decremented in {@link #tick()}. That was the
     * bug this replaced: {@code tick()} only ever runs while {@code GoalSelector} considers this
     * goal running, which is exactly the window a cooldown is NOT active in, so a manually
     * decremented counter set in {@link #stop()} would sit at its starting value forever and the
     * goal could never be selected again after its first release. {@link #canUse()} is polled on
     * its own cadence regardless of whether this goal is running, so comparing against a stored
     * target time here works where decrementing inside {@code tick()} could not.
     */
    private long cooldownUntil = Long.MIN_VALUE;

    protected EndemicAreaEffectGoal(PathfinderMob owner, double triggerRange, int fuseTicks, int cooldownTicks) {
        this.owner = owner;
        this.triggerRange = triggerRange;
        this.fuseTicks = fuseTicks;
        this.cooldownTicks = cooldownTicks;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    protected final PathfinderMob owner() {
        return this.owner;
    }

    /** Whether the owner was hurt since the last time this cleared it. Provocation, not proximity. */
    protected abstract boolean isProvoked();

    /** Called once, in {@link #start()}, so one provocation starts exactly one fuse. */
    protected abstract void clearProvoked();

    /** Presentation only: tell the entity to show or hide its telegraph animation. */
    protected abstract void setPresenting(boolean presenting);

    /** Applies the actual effect. Called exactly once, at the end of the fuse. */
    protected abstract void release();

    /**
     * Whether merely getting close (no hit needed) can also start the fuse. True by default,
     * matching the flashbug, which should go off before something walks right into it. The toad
     * overrides this to false: it should only ever go off from being interacted with or hit, not
     * proximity alone.
     */
    protected boolean allowsProximityTrigger() {
        return true;
    }

    /** True while the fuse is burning. Exposed for tests and for subclasses' own presentation. */
    public final boolean isActive() {
        return this.fuseTicksLeft > 0;
    }

    @Override
    public boolean canUse() {
        if (this.owner.level().getGameTime() < this.cooldownUntil) {
            return false;
        }
        return isProvoked() || (allowsProximityTrigger() && nearestTrigger() != null);
    }

    @Override
    public boolean canContinueToUse() {
        // The fuse burns down regardless of whether whatever set it off is still around: the
        // release is an area effect evaluated once at the end, not a strike tracked against one
        // victim, so there is no "target" to lose the way rule 6 means for a committed melee hit.
        return this.fuseTicksLeft > 0;
    }

    @Override
    public boolean isInterruptable() {
        return this.fuseTicksLeft <= 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /** Nearest living entity within trigger range that this owner can actually see. */
    protected final LivingEntity nearestTrigger() {
        AABB range = this.owner.getBoundingBox().inflate(this.triggerRange);
        List<LivingEntity> nearby = this.owner.level().getEntitiesOfClass(LivingEntity.class, range,
                e -> e != this.owner && e.isAlive());
        for (LivingEntity candidate : nearby) {
            if (this.owner.hasLineOfSight(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    @Override
    public void start() {
        this.fuseTicksLeft = this.fuseTicks;
        clearProvoked();
        setPresenting(true);
        this.owner.getNavigation().stop();
    }

    @Override
    public void stop() {
        setPresenting(false);
        this.fuseTicksLeft = -1;
        this.cooldownUntil = this.owner.level().getGameTime() + this.cooldownTicks;
    }

    @Override
    public void tick() {
        if (this.fuseTicksLeft <= 0) {
            return;
        }
        this.fuseTicksLeft--;
        if (this.fuseTicksLeft == 0) {
            release();
        }
    }
}
