package com.carro1001.mhnw.entities.blango;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

import static com.carro1001.mhnw.utils.MHNWReferences.BLANGO;
import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class BlangoModel extends AnimatedGeoModel<BlangoEntity> {
    @Override
    public ResourceLocation getModelLocation(BlangoEntity object)
    {
        return new ResourceLocation(MODID, "geo/"+ BLANGO +".geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(BlangoEntity object)
    {
        return new ResourceLocation(MODID, "textures/entity/"+ BLANGO +".png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(BlangoEntity object)
    {
        return new ResourceLocation(MODID, "animations/"+ BLANGO +".animation.json");
    }

}
