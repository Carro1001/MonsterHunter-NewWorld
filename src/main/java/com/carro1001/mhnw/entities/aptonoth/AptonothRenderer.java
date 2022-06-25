package com.carro1001.mhnw.entities.aptonoth;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

import static com.carro1001.mhnw.utils.MHNWReferences.APTONOTH;
import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class AptonothRenderer extends GeoEntityRenderer<AptonothEntity> {
    private static final ResourceLocation RESOURCE_LOCATION =  new ResourceLocation(MODID, "textures/entity/"+ APTONOTH +".png");

    public AptonothRenderer(EntityRendererProvider.Context context) {
        super(context, new AptonothModel(context.bakeLayer(AptonothModel.LAYER_LOCATION)));
    }
    @Override
    public RenderType getRenderType(AptonothEntity animatable, float partialTicks, PoseStack stack,
                                    MultiBufferSource renderTypeBuffer, VertexConsumer vertexBuilder, int packedLightIn,
                                    ResourceLocation textureLocation) {
        return RenderType.entityCutout(getTextureLocation(animatable));
    }
    @Override
    public ResourceLocation getTextureLocation(AptonothEntity pEntity) {
        return RESOURCE_LOCATION;
    }
}
