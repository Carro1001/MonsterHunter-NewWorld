package com.carro1001.mhnw.client.models.entities;

import com.carro1001.mhnw.entities.NewWorldMonsterEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public abstract class MonsterModel extends DefaultedEntityGeoModel<NewWorldMonsterEntity> {
    public MonsterModel(String name) {
        super(new ResourceLocation(MODID, name), false);
    }


    public abstract void setCustomAnimations(NewWorldMonsterEntity entity, long uniqueID, AnimationState<NewWorldMonsterEntity> customPredicate);
}
