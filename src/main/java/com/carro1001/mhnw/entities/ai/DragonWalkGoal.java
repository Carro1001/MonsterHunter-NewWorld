package com.carro1001.mhnw.entities.ai;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class DragonWalkGoal extends RandomStrollGoal {
    public static final float PROBABILITY = 0.001F;
    protected final float probability;

    public DragonWalkGoal(PathfinderMob pMob, double pSpeedModifier) {
        this(pMob, pSpeedModifier, 0.001F);
    }

    public DragonWalkGoal(PathfinderMob pMob, double pSpeedModifier, float pProbability) {
        super(pMob, pSpeedModifier);
        this.probability = pProbability;
    }

    @Nullable
    protected Vec3 getPosition() {
        if (this.mob.isInWaterOrBubble()) {
            Vec3 vec3 = LandRandomPos.getPos(this.mob, 50, 17);
            return vec3 == null ? super.getPosition() : vec3;
        } else {
            return this.mob.getRandom().nextFloat() >= this.probability ? LandRandomPos.getPos(this.mob, 50, 17) : super.getPosition();
        }
    }
}
