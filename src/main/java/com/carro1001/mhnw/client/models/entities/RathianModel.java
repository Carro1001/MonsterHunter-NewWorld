package com.carro1001.mhnw.client.models.entities;

import com.carro1001.mhnw.entities.DragonEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;
import static com.carro1001.mhnw.utils.MHNWReferences.RATHIAN;

public class RathianModel extends DefaultedEntityGeoModel<DragonEntity> {

    public RathianModel() {
        super(new ResourceLocation(MODID, RATHIAN), true);
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
