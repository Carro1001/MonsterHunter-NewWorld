package com.carro1001.mhnw.client.models.entities;

import com.carro1001.mhnw.entities.ToadEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

import static com.carro1001.mhnw.utils.MHNWReferences.*;

public class ToadModel extends DefaultedEntityGeoModel<ToadEntity> {

    public static final ResourceLocation RESOURCE_LOCATION_POISON =  new ResourceLocation(MODID, "textures/entity/"+ POISON+TOAD +".png");
    public static final ResourceLocation RESOURCE_LOCATION_SLEEP =  new ResourceLocation(MODID, "textures/entity/"+ SLEEP+TOAD +".png");
    public static final ResourceLocation RESOURCE_LOCATION_PARAS =  new ResourceLocation(MODID, "textures/entity/"+ PARALIZE+TOAD +".png");
    public static final ResourceLocation RESOURCE_LOCATION_BLAST =  new ResourceLocation(MODID, "textures/entity/"+ BLAST+TOAD +".png");

    public ToadModel() {
        super(new ResourceLocation(MODID, TOAD), true);
    }

    @Override
    public ResourceLocation getModelResource(ToadEntity object) {
        return new ResourceLocation(MODID, "geo/toad.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ToadEntity object) {
        return switch (object.getTypeDir()) {
            case 0 -> RESOURCE_LOCATION_POISON;
            case 1 -> RESOURCE_LOCATION_SLEEP;
            case 2 -> RESOURCE_LOCATION_PARAS;
            default -> RESOURCE_LOCATION_BLAST;
        };
    }

    @Override
    public ResourceLocation getAnimationResource(ToadEntity animatable) {
        return new ResourceLocation(MODID, "animations/toad.animation.json");
    }
}
