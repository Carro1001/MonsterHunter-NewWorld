package com.carro1001.mhnw.client.models.entities;

import com.carro1001.mhnw.entities.AptonothEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class AptonothModel extends AnimatedGeoModel<AptonothEntity> {
    @Override
    public ResourceLocation getModelLocation(AptonothEntity object)
    {
        return new ResourceLocation(MODID, "geo/aptonoth.geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(AptonothEntity object)
    {
        return new ResourceLocation(MODID, "textures/entity/aptonoth.png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(AptonothEntity object)
    {
        return new ResourceLocation(MODID, "animations/aptonoth.animation.json");
    }

}
