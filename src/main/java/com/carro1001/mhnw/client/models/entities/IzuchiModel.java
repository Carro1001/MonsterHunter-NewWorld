package com.carro1001.mhnw.client.models.entities;

import com.carro1001.mhnw.entities.LargeMonster;
import software.bernie.geckolib.core.animation.AnimationState;

import static com.carro1001.mhnw.utils.MHNWReferences.IZUCHI;

public class IzuchiModel extends MonsterModel {

    public IzuchiModel() {
        super(IZUCHI);
    }

    @Override
    public void setCustomAnimations(LargeMonster entity, long uniqueID, AnimationState<LargeMonster> customPredicate) {

    }
}
