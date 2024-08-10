package com.carro1001.mhnw.client.models.entities;

import com.carro1001.mhnw.entities.NewWorldMonsterEntity;
import software.bernie.geckolib.core.animation.AnimationState;

import static com.carro1001.mhnw.utils.MHNWReferences.BLANGONGA;

public class BlangongaModel extends MonsterModel {

    public BlangongaModel() {
        super(BLANGONGA);
    }

    @Override
    public void setCustomAnimations(NewWorldMonsterEntity entity, long uniqueID, AnimationState<NewWorldMonsterEntity> customPredicate) {

    }
}
