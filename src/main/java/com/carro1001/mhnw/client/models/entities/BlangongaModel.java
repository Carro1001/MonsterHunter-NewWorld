package com.carro1001.mhnw.client.models.entities;

import com.carro1001.mhnw.entities.NewWorldGrowingEntity;
import software.bernie.geckolib.core.animation.AnimationState;

import static com.carro1001.mhnw.utils.MHNWReferences.BLANGONGA;

public class BlangongaModel extends NewWorldGrowingEntityModel {

    public BlangongaModel() {
        super(BLANGONGA);
    }

    @Override
    public void setCustomAnimations(NewWorldGrowingEntity entity, long uniqueID, AnimationState<NewWorldGrowingEntity> customPredicate) {

    }
}
