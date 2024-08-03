package com.carro1001.mhnw.entities;

import com.carro1001.mhnw.entities.ai.RallyGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import static com.carro1001.mhnw.utils.MHNWReferences.BLANGONGA;

public class BlangongaEntity extends BlangoEntity {

    public BlangongaEntity(EntityType<? extends LargeMonster> p_27557_, Level p_27558_) {
        super(p_27557_, p_27558_);
        this.name = BLANGONGA;
    }
    protected void registerGoals() {
        this.goalSelector.addGoal(10, new RallyGoal(this));
        this.addBehaviourGoals();
    }

}
