package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.client.models.entities.ToadModel;
import com.carro1001.mhnw.entities.ToadEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

import static com.carro1001.mhnw.client.models.entities.ToadModel.*;

public class ToadRenderer extends GeoEntityRenderer<ToadEntity> {

    public ToadRenderer(EntityRendererProvider.Context context) {
        super(context, new ToadModel());
    }
    @Override
    public RenderType getRenderType(ToadEntity animatable, float partialTicks, PoseStack stack,
                                    MultiBufferSource renderTypeBuffer, VertexConsumer vertexBuilder, int packedLightIn,
                                    ResourceLocation textureLocation) {
        return RenderType.entityCutoutNoCull(getTextureLocation(animatable));
    }
    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ToadEntity pEntity) {
        return switch (pEntity.getTypeDir()) {
            case 0 -> RESOURCE_LOCATION_POISON;
            case 1 -> RESOURCE_LOCATION_SLEEP;
            case 2 -> RESOURCE_LOCATION_PARAS;
            default -> RESOURCE_LOCATION_BLAST;
        };
    }
}
