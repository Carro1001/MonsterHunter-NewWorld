package com.carro1001.mhnw.client.models.entities;

import com.carro1001.mhnw.entities.AptonothEntity;
import com.carro1001.mhnw.utils.MHNWReferences;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class AptonothModel extends DefaultedEntityGeoModel<AptonothEntity> {

    public AptonothModel() {
        super(new ResourceLocation(MODID, MHNWReferences.APTONOTH), true);
    }

    @Override
    public ResourceLocation getModelResource(AptonothEntity object) {
        return new ResourceLocation(MODID, "geo/aptonoth.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AptonothEntity object) {
        return new ResourceLocation(MODID, "textures/entity/aptonoth.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AptonothEntity animatable) {
        return new ResourceLocation(MODID, "animations/aptonoth.animation.json");
    }
}
