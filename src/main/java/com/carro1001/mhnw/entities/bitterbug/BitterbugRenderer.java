package com.carro1001.mhnw.entities.bitterbug;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import static com.carro1001.mhnw.utils.MHNWReferences.BITTERBUG;
import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class BitterbugRenderer extends MobRenderer<BitterbugEntity, BitterbugModel<BitterbugEntity>> {
    private static final ResourceLocation RESOURCE_LOCATION =  new ResourceLocation(MODID, "textures/entity/"+ BITTERBUG +".png");

    public BitterbugRenderer(EntityRendererProvider.Context context) {
        super(context, new BitterbugModel<>(context.bakeLayer(BitterbugModel.LAYER_LOCATION)), 0.2F);
    }

    @Override
    public ResourceLocation getTextureLocation(BitterbugEntity pEntity) {
        return RESOURCE_LOCATION;
    }
}
