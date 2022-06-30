package com.carro1001.mhnw.entities.izuchi;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

import static com.carro1001.mhnw.utils.MHNWReferences.IZUCHI;
import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class IzuchiModel extends AnimatedGeoModel<IzuchiEntity> {
    @Override
    public ResourceLocation getModelLocation(IzuchiEntity object)
    {
        return new ResourceLocation(MODID, "geo/"+ IZUCHI +".geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(IzuchiEntity object)
    {
        return new ResourceLocation(MODID, "textures/entity/"+ IZUCHI +".png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(IzuchiEntity object)
    {
        return new ResourceLocation(MODID, "animations/"+ IZUCHI +".animation.json");
    }

}
