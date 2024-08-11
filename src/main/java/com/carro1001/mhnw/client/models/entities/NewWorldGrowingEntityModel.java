package com.carro1001.mhnw.client.models.entities;

import com.carro1001.mhnw.entities.NewWorldGrowingEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public abstract class NewWorldGrowingEntityModel extends DefaultedEntityGeoModel<NewWorldGrowingEntity> {
    public NewWorldGrowingEntityModel(String name) {
        super(new ResourceLocation(MODID, name), false);
    }


    public abstract void setCustomAnimations(NewWorldGrowingEntity entity, long uniqueID, AnimationState<NewWorldGrowingEntity> customPredicate);
}
