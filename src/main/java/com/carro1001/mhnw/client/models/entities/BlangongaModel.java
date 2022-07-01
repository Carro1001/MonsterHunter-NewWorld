package com.carro1001.mhnw.client.models.entities;

import com.carro1001.mhnw.entities.BlangongaEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

import static com.carro1001.mhnw.utils.MHNWReferences.BLANGONGA;
import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class BlangongaModel extends AnimatedGeoModel<BlangongaEntity> {
    @Override
    public ResourceLocation getModelLocation(BlangongaEntity object)
    {
        return new ResourceLocation(MODID, "geo/"+ BLANGONGA +".geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(BlangongaEntity object)
    {
        return new ResourceLocation(MODID, "textures/entity/"+ BLANGONGA +".png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(BlangongaEntity object)
    {
        return new ResourceLocation(MODID, "animations/"+ BLANGONGA +".animation.json");
    }

}
