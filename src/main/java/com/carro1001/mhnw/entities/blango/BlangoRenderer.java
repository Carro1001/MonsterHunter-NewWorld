package com.carro1001.mhnw.entities.blango;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

import static com.carro1001.mhnw.utils.MHNWReferences.BLANGO;
import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class BlangoRenderer extends GeoEntityRenderer<BlangoEntity> {
    private static final ResourceLocation RESOURCE_LOCATION =  new ResourceLocation(MODID, "textures/entity/"+ BLANGO +".png");

    public BlangoRenderer(EntityRendererProvider.Context context) {
        super(context, new BlangoModel());
    }
    @Override
    public RenderType getRenderType(com.carro1001.mhnw.entities.blango.BlangoEntity animatable, float partialTicks, PoseStack stack,
                                    MultiBufferSource renderTypeBuffer, VertexConsumer vertexBuilder, int packedLightIn,
                                    ResourceLocation textureLocation) {
        return RenderType.entityCutoutNoCull(getTextureLocation(animatable));
    }
    @Override
    public ResourceLocation getTextureLocation(BlangoEntity pEntity) {
        return RESOURCE_LOCATION;
    }
}
