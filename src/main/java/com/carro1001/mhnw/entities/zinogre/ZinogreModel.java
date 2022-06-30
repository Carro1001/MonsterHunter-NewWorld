package com.carro1001.mhnw.entities.zinogre;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;
import static com.carro1001.mhnw.utils.MHNWReferences.ZINOGRE;

public class ZinogreModel extends AnimatedGeoModel<ZinogreEntity> {
    @Override
    public ResourceLocation getModelLocation(ZinogreEntity object)
    {
        return new ResourceLocation(MODID, "geo/"+ ZINOGRE +".geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(ZinogreEntity object)
    {
        return new ResourceLocation(MODID, "textures/entity/"+ ZINOGRE +".png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(ZinogreEntity object)
    {
        return new ResourceLocation(MODID, "animations/"+ ZINOGRE +".animation.json");
    }

}
