package com.carro1001.mhnw.entities.ai;

import com.carro1001.mhnw.entities.FlashBugEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.HoverRandomPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class FlashBugFloatGoal extends Goal{
    private final FlashBugEntity mob;

    public FlashBugFloatGoal(FlashBugEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    /**
     * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
     * method as well.
     */
    public boolean canUse() {
        return mob.getNavigation().isDone() && mob.getRandom().nextInt(10) == 0;
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    public boolean canContinueToUse() {
        return mob.getNavigation().isInProgress();
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    public void start() {
        Vec3 vec3 = this.findPos();
        if (vec3 != null) {
            mob.getNavigation().moveTo(mob.getNavigation().createPath(BlockPos.containing(vec3), 1), 0.7D);
        }
        mob.setFlying(true);

    }
    @Nullable
    private Vec3 findPos() {
        Vec3 vec3= mob.getViewVector(0.0F);

        int i = 8;
        Vec3 vec32 = HoverRandomPos.getPos(mob, 4, 7, vec3.x, vec3.z, ((float)Math.PI / 2F), 6, 1);
        return vec32 != null ? vec32 : AirAndWaterRandomPos.getPos(mob, 8, 5, -2, vec3.x, vec3.z, (double)((float)Math.PI / 2F));
    }
    @Override
    public void stop() {
        super.stop();
        mob.setFlying(false);

    }
}
