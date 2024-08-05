package com.carro1001.mhnw.client.models.entities;


import com.carro1001.mhnw.entities.BlangoEntity;
import com.carro1001.mhnw.entities.LagiacrusEntity;
import com.carro1001.mhnw.entities.LargeMonster;
import software.bernie.geckolib.core.animation.AnimationState;

import static com.carro1001.mhnw.utils.MHNWReferences.BLANGO;

public class BlangoModel extends MonsterModel {

    public BlangoModel() {
        super(BLANGO);
    }

    @Override
    public void setCustomAnimations(LargeMonster entity, long uniqueID, AnimationState<LargeMonster> customPredicate) {

    }
}
