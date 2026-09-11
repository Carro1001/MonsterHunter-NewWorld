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
    private int cooldown;

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

    /** True while the fuse is burning. Exposed for tests and for subclasses' own presentation. */
    public final boolean isActive() {
        return this.fuseTicksLeft > 0;
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            return false;
        }
        return isProvoked() || nearestTrigger() != null;
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
        this.cooldown = this.cooldownTicks;
    }

    @Override
    public void tick() {
        if (this.cooldown > 0) {
            this.cooldown--;
        }
        if (this.fuseTicksLeft <= 0) {
            return;
        }
        this.fuseTicksLeft--;
        if (this.fuseTicksLeft == 0) {
            release();
        }
    }
}
