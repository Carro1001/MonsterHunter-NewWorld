package com.carro1001.mhnw.entities.ai;

import com.carro1001.mhnw.entities.NewWorldMonsterEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class ExhaustedStallGoal extends Goal {
    private final NewWorldMonsterEntity mob;
    private double relX;
    private double relZ;
    private int lookTime;

    public ExhaustedStallGoal(NewWorldMonsterEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));

    }
    public boolean canUse() {
        return mob.isExhausted();
    }

    public boolean canContinueToUse() {
        return this.lookTime >= 0 && mob.isExhausted();
    }

    public void start() {
        double d0 = (Math.PI * 2D) * this.mob.getRandom().nextDouble();
        this.relX = Math.cos(d0);
        this.relZ = Math.sin(d0);
        this.lookTime = 20 + this.mob.getRandom().nextInt(20);
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }
    public void tick() {
        --this.lookTime;
        mob.tickExhaustBuildUp(-1);
        this.mob.getLookControl().setLookAt(this.mob.getX() + this.relX, this.mob.getEyeY(), this.mob.getZ() + this.relZ);
    }
}
