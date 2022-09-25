package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.client.models.entities.FlashBugModel;
import com.carro1001.mhnw.entities.FlashBugEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

import static com.carro1001.mhnw.utils.MHNWReferences.FLASHBUG;
import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class FlashBugRenderer extends GeoEntityRenderer<FlashBugEntity> {
    private static final ResourceLocation RESOURCE_LOCATION =  new ResourceLocation(MODID, "textures/entity/"+ FLASHBUG +".png");

    public FlashBugRenderer(EntityRendererProvider.Context context) {
        super(context, new FlashBugModel());
    }
    @Override
    public RenderType getRenderType(FlashBugEntity animatable, float partialTicks, PoseStack stack,
                                    MultiBufferSource renderTypeBuffer, VertexConsumer vertexBuilder, int packedLightIn,
                                    ResourceLocation textureLocation) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }
    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull FlashBugEntity pEntity) {
        return RESOURCE_LOCATION;
    }
}
