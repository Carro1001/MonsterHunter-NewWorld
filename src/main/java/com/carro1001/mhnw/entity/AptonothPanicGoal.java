package com.carro1001.mhnw.entity;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.PanicGoal;

/**
 * Vanilla {@link PanicGoal} dashes to one random point roughly 5 blocks away and then stops,
 * re-evaluating {@code shouldPanic()} from scratch (which checks only whether
 * {@code getLastDamageSource()} is still the most recent damage and is panic-tagged). A single
 * dash read as barely fleeing at all rather than a real flee, so this keeps panicking for a fixed
 * window after being hurt, chaining several of vanilla's own dashes into one sustained flee instead
 * of one short hop.
 */
public class AptonothPanicGoal extends PanicGoal {

    /** How long a hit keeps this fleeing, in ticks. */
    private static final int PANIC_DURATION_TICKS = 100;

    private int panicUntilTick = -1;

    public AptonothPanicGoal(PathfinderMob mob, double speedModifier) {
        super(mob, speedModifier);
    }

    @Override
    protected boolean shouldPanic() {
        if (super.shouldPanic()) {
            this.panicUntilTick = this.mob.tickCount + PANIC_DURATION_TICKS;
        }
        return this.mob.tickCount <= this.panicUntilTick;
    }
}
