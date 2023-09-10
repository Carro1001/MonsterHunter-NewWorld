package com.carro1001.mhnw.client.models.entities;

import com.carro1001.mhnw.entities.ZinogreEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

import static com.carro1001.mhnw.utils.MHNWReferences.*;

public class ZinogreModel extends DefaultedEntityGeoModel<ZinogreEntity> {

    public ZinogreModel() {
        super(new ResourceLocation(MODID, ZINOGRE), true);
    }

    @Override
    public ResourceLocation getModelResource(ZinogreEntity object)
    {
        return new ResourceLocation(MODID, "geo/"+ ZINOGRE +".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ZinogreEntity object)
    {
        return new ResourceLocation(MODID, "textures/entity/"+ ZINOGRE +".png");
    }

    @Override
    public ResourceLocation getAnimationResource(ZinogreEntity object)
    {
        return new ResourceLocation(MODID, "animations/"+ ZINOGRE +".animation.json");
    }

}
