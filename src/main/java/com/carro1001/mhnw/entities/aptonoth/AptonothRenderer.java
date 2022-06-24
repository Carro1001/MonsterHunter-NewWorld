package com.carro1001.mhnw.entities.aptonoth;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import static com.carro1001.mhnw.utils.MHNWReferences.APTONOTH;
import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class AptonothRenderer extends MobRenderer<AptonothEntity, AptonothModel<AptonothEntity>> {
    private static final ResourceLocation RESOURCE_LOCATION =  new ResourceLocation(MODID, "textures/entity/"+ APTONOTH +".png");

    public AptonothRenderer(EntityRendererProvider.Context context) {
        super(context, new AptonothModel<>(context.bakeLayer(AptonothModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(AptonothEntity pEntity) {
        return RESOURCE_LOCATION;
    }
}
