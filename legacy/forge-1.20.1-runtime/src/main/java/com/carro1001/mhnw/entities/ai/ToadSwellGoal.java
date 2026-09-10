package com.carro1001.mhnw.entities.ai;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.entities.ToadEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class ToadSwellGoal extends Goal {
    private final ToadEntity toad;
    @Nullable
    private LivingEntity target;

    public ToadSwellGoal(ToadEntity pCreeper) {
        this.toad = pCreeper;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    /**
     * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
     * method as well.
     */
    public boolean canUse() {
        LivingEntity livingentity = this.toad.getTarget();
        return this.toad.getSwellDir() > 0 || livingentity != null && this.toad.distanceToSqr(livingentity) < 9.0D;
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    public void start() {
        MHNW.debugLog("ToadSwellGoal: start");
        this.toad.getNavigation().stop();
        this.target = this.toad.getTarget();
    }

    /**
     * Reset the task's internal state. Called when this task is interrupted by another one
     */
    public void stop() {
        MHNW.debugLog("ToadSwellGoal: start");
        this.target = null;
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * Keep ticking a continuous task that has already been started
     */
    public void tick() {
        if (this.target == null) {
            this.toad.setSwellDir(-1);
        } else if (this.toad.distanceToSqr(this.target) > 49.0D) {
            this.toad.setSwellDir(-1);
        } else if (!this.toad.getSensing().hasLineOfSight(this.target)) {
            this.toad.setSwellDir(-1);
        } else {
            this.toad.setSwellDir(1);
        }
    }
}