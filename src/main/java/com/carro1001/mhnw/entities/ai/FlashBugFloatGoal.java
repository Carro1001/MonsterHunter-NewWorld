package com.carro1001.mhnw.entities.ai;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class FlashBugFloatGoal extends Goal{
        private final Mob mob;

        public FlashBugFloatGoal(Mob mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        /**
         * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
         * method as well.
         */
        public boolean canUse() {
            MoveControl movecontrol = this.mob.getMoveControl();
            if (!movecontrol.hasWanted()) {
                return true;
            } else {
                double d0 = movecontrol.getWantedX() - this.mob.getX();
                double d1 = movecontrol.getWantedY() - this.mob.getY();
                double d2 = movecontrol.getWantedZ() - this.mob.getZ();
                double d3 = d0 * d0 + d1 * d1 + d2 * d2;
                return d3 < 1.0D || d3 > 3600.0D;
            }
        }

        /**
         * Returns whether an in-progress EntityAIBase should continue executing
         */
        public boolean canContinueToUse() {
            return false;
        }

        /**
         * Execute a one shot task or start executing a continuous task
         */
        public void start() {
            RandomSource randomsource = this.mob.getRandom();
            double d0 = this.mob.getX() + (double)((randomsource.nextFloat() * 2.0F - 1.0F) * 32.0F);
            double d1 = this.mob.getY() + (double)((randomsource.nextFloat() * 2.0F - 1.0F) * 32.0F);
            double d2 = this.mob.getZ() + (double)((randomsource.nextFloat() * 2.0F - 1.0F) * 32.0F);
            this.mob.getMoveControl().setWantedPosition(d0, d1, d2, 1.0D);
            this.mob.lookAt(EntityAnchorArgument.Anchor.EYES,new Vec3(d0, d1, d2));
        }

}
