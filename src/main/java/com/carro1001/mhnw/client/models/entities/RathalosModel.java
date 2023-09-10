package com.carro1001.mhnw.client.models.entities;

import com.carro1001.mhnw.entities.DragonEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

import static com.carro1001.mhnw.utils.MHNWReferences.*;

public class RathalosModel extends DefaultedEntityGeoModel<DragonEntity> {

    public RathalosModel() {
        super(new ResourceLocation(MODID, RATHALOS), true);
    }

    @Override
    public ResourceLocation getModelResource(DragonEntity object)
    {
        return new ResourceLocation(MODID, "geo/rathalos.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DragonEntity object)
    {
        return new ResourceLocation(MODID, "textures/entity/rathalos.png");
    }

    @Override
    public ResourceLocation getAnimationResource(DragonEntity object)
    {
        return new ResourceLocation(MODID, "animations/rathalos.animation.json");
    }

}
