package com.carro1001.mhnw.client.models.entities;

import com.carro1001.mhnw.entities.NewWorldGrowingEntity;
import software.bernie.geckolib.core.animation.AnimationState;

import static com.carro1001.mhnw.utils.MHNWReferences.APTONOTH;

public class AptonothModel extends NewWorldGrowingEntityModel {

    public AptonothModel() {
        super(APTONOTH);
    }

    @Override
    public void setCustomAnimations(NewWorldGrowingEntity entity, long uniqueID, AnimationState<NewWorldGrowingEntity> customPredicate) {

    }
}
