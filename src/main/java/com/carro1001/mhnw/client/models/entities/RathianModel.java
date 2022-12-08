package com.carro1001.mhnw.client.models.entities;

import com.carro1001.mhnw.entities.DragonEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedTickingGeoModel;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class RathianModel extends AnimatedTickingGeoModel<DragonEntity> {
    public RathianModel() {

    }
    @Override
    public ResourceLocation getModelResource(DragonEntity object)
    {
        return new ResourceLocation(MODID, "geo/rathian.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DragonEntity object)
    {
        return new ResourceLocation(MODID, "textures/entity/rathian.png");
    }

    @Override
    public ResourceLocation getAnimationResource(DragonEntity object)
    {
        return new ResourceLocation(MODID, "animations/rathian.animation.json");
    }


}
