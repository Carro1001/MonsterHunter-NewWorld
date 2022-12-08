package com.carro1001.mhnw.client.models.entities;

import com.carro1001.mhnw.entities.FlashBugEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedTickingGeoModel;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class FlashBugModel extends AnimatedTickingGeoModel<FlashBugEntity> {



    @Override
    public ResourceLocation getModelResource(FlashBugEntity object) {
        return new ResourceLocation(MODID, "geo/flashbug.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(FlashBugEntity object) {
        return new ResourceLocation(MODID, "textures/entity/flashbug.png");
    }

    @Override
    public ResourceLocation getAnimationResource(FlashBugEntity animatable) {
        return new ResourceLocation(MODID, "animations/flashbug.animation.json");
    }
}
