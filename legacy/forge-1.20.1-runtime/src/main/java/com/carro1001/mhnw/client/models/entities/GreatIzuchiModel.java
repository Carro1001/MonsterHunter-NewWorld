package com.carro1001.mhnw.client.models.entities;

import com.carro1001.mhnw.entities.NewWorldGrowingEntity;
import software.bernie.geckolib.core.animation.AnimationState;

import static com.carro1001.mhnw.utils.MHNWReferences.GREAT;
import static com.carro1001.mhnw.utils.MHNWReferences.IZUCHI;

public class GreatIzuchiModel extends NewWorldGrowingEntityModel {
    public GreatIzuchiModel() {
        super(GREAT+ "_" + IZUCHI);
    }

    @Override
    public void setCustomAnimations(NewWorldGrowingEntity entity, long uniqueID, AnimationState<NewWorldGrowingEntity> customPredicate) {

    }
}
