package com.carro1001.mhnw.client.models.entities;

import com.carro1001.mhnw.entities.BlangongaEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

import static com.carro1001.mhnw.utils.MHNWReferences.BLANGONGA;
import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class BlangongaModel extends DefaultedEntityGeoModel<BlangongaEntity> {

    public BlangongaModel() {
        super(new ResourceLocation(MODID, BLANGONGA), true);
    }

    @Override
    public ResourceLocation getModelResource(BlangongaEntity object)
    {
        return new ResourceLocation(MODID, "geo/"+ BLANGONGA +".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BlangongaEntity object)
    {
        return new ResourceLocation(MODID, "textures/entity/"+ BLANGONGA +".png");
    }

    @Override
    public ResourceLocation getAnimationResource(BlangongaEntity object)
    {
        return new ResourceLocation(MODID, "animations/"+ BLANGONGA +".animation.json");
    }

}
