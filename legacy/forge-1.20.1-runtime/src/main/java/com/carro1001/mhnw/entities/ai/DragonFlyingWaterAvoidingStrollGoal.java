package com.carro1001.mhnw.entities.ai;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.entities.DragonEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class DragonFlyingWaterAvoidingStrollGoal extends WaterAvoidingRandomFlyingGoal {
    private final DragonEntity dragonEntity;

    public DragonFlyingWaterAvoidingStrollGoal(DragonEntity dragonEntity, double speedModifier) {
        super(dragonEntity, speedModifier);
        this.dragonEntity = dragonEntity;
        //this.probability = 0.05F;
    }

    @Override
    public boolean canUse() {
        return this.dragonEntity.getState() == DragonEntity.State.FLYING && this.dragonEntity.getAggressionState() == DragonEntity.AggressionState.PASSIVE && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return this.dragonEntity.getState() == DragonEntity.State.FLYING && this.dragonEntity.getAggressionState() == DragonEntity.AggressionState.PASSIVE && super.canContinueToUse();
    }

    @Nullable
    @Override
    protected Vec3 getPosition() { // TODO: Something smarter
        int range = 32;
        MHNW.debugLog("DragonFlyingWaterAvoidingStrollGoal: getPosition" );
        return this.dragonEntity.position().add(Mth.randomBetween(this.dragonEntity.getRandom(), -range, range), Mth.randomBetween(this.dragonEntity.getRandom(), -range, range), Mth.randomBetween(this.dragonEntity.getRandom(), -range, range));
    }
}
