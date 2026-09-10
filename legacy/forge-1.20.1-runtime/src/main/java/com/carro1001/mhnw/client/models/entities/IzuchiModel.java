package com.carro1001.mhnw.client.models.entities;

import com.carro1001.mhnw.entities.NewWorldGrowingEntity;
import software.bernie.geckolib.core.animation.AnimationState;

import static com.carro1001.mhnw.utils.MHNWReferences.IZUCHI;

public class IzuchiModel extends NewWorldGrowingEntityModel {

    public IzuchiModel() {
        super(IZUCHI);
    }

    @Override
    public void setCustomAnimations(NewWorldGrowingEntity entity, long uniqueID, AnimationState<NewWorldGrowingEntity> customPredicate) {

    }
}
