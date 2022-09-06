package com.carro1001.mhnw.client.models.entities;

import com.carro1001.mhnw.entities.BlangoEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

import static com.carro1001.mhnw.utils.MHNWReferences.BLANGO;
import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class BlangoModel extends AnimatedGeoModel<BlangoEntity> {
    @Override
    public ResourceLocation getModelResource(BlangoEntity object)
    {
        return new ResourceLocation(MODID, "geo/"+ BLANGO +".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BlangoEntity object)
    {
        return new ResourceLocation(MODID, "textures/entity/"+ BLANGO +".png");
    }

    @Override
    public ResourceLocation getAnimationResource(BlangoEntity object)
    {
        return new ResourceLocation(MODID, "animations/"+ BLANGO +".animation.json");
    }

}
