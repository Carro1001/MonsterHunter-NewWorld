package com.carro1001.mhnw.client.models.entities;

import com.carro1001.mhnw.entities.LargeMonster;
import software.bernie.geckolib.core.animation.AnimationState;

import static com.carro1001.mhnw.utils.MHNWReferences.RATHALOS;

public class RathalosModel extends MonsterModel {

    public RathalosModel() {
        super(RATHALOS);
    }

    @Override
    public void setCustomAnimations(LargeMonster entity, long uniqueID, AnimationState<LargeMonster> customPredicate) {

    }
}
