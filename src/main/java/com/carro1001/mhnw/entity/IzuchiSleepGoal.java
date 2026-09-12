package com.carro1001.mhnw.entity;

import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Izuchi naps when nothing is threatening it, using the preserved {@code sleep} clip, and wakes on
 * its own after a while even if nothing disturbs it.
 *
 * <p>Deliberately does not implement "wake up when a target appears" itself: {@code Monster} runs
 * a separate {@code targetSelector}, independent of this {@code goalSelector}, so a target can be
 * acquired while this goal is running. This goal is given a lower priority (a higher priority
 * number) than the melee attack goal, so as soon as {@code NearestAttackableTargetGoal} or
 * {@code HurtByTargetGoal} sets a target, vanilla's own goal-selector preemption stops this goal
 * and starts the attack goal automatically, the same mechanism that already lets
 * {@code PanicGoal} interrupt ordinary wandering elsewhere in this mod. No custom "check for
 * threats" logic is needed here at all.
 *
 * <p>Deliberately does not track "how long has it been idle" before allowing sleep, the way an
 * earlier draft of this goal did. That field would only ever have been incremented inside
 * {@code tick()}, and {@code tick()} only runs while a goal {@code isRunning()}, so a counter
 * meant to accumulate while this goal is NOT selected would never advance, the exact class of bug
 * {@code EndemicAreaEffectGoal}'s cooldown had. A flat per-check random chance sidesteps it.
 */
public class IzuchiSleepGoal extends Goal {

    /** Checked roughly every other tick (goal selection cadence); keeps naps a rare event. */
    private static final float SLEEP_CHANCE_PER_CHECK = 1.0F / 1200.0F;
    /** Wakes on its own after this long even with nothing around, so it is not static forever. */
    private static final int SLEEP_DURATION_TICKS = 400;

    private final Izuchi izuchi;
    private int sleepTicksLeft = -1;

    public IzuchiSleepGoal(Izuchi izuchi) {
        this.izuchi = izuchi;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return this.izuchi.getTarget() == null
                && this.izuchi.onGround()
                && this.izuchi.getRandom().nextFloat() < SLEEP_CHANCE_PER_CHECK;
    }

    @Override
    public boolean canContinueToUse() {
        return this.izuchi.getTarget() == null && this.sleepTicksLeft > 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.sleepTicksLeft = SLEEP_DURATION_TICKS;
        this.izuchi.setSleeping(true);
        this.izuchi.getNavigation().stop();
    }

    @Override
    public void stop() {
        this.izuchi.setSleeping(false);
        this.sleepTicksLeft = -1;
    }

    @Override
    public void tick() {
        if (this.sleepTicksLeft > 0) {
            this.sleepTicksLeft--;
        }
    }
}
