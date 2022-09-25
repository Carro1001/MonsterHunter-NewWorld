package com.carro1001.mhnw.entities.ai;

import com.carro1001.mhnw.entities.ToadEntity;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class ToadWalkGoal extends RandomStrollGoal{
        protected final float probability = 0.001F;
        private ToadEntity toadEntity;
        public ToadWalkGoal(ToadEntity pMob, double pSpeedModifier) {
            super(pMob, pSpeedModifier);
            this.toadEntity = pMob;
        }

        @Nullable
        protected Vec3 getPosition() {
            if (this.mob.isInWaterOrBubble()) {
                Vec3 vec3 = LandRandomPos.getPos(this.mob, 2, 2);
                return vec3 == null ? super.getPosition() : vec3;
            } else {
                return this.mob.getRandom().nextFloat() >= this.probability ? LandRandomPos.getPos(this.mob, 4, 2) : super.getPosition();
            }
        }


}
