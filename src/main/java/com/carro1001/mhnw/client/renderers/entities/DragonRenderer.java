package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.entities.DragonEntity;
import com.carro1001.mhnw.entities.DragonPart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib3.model.AnimatedTickingGeoModel;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

public abstract class DragonRenderer extends GeoEntityRenderer<DragonEntity> {

    public DragonRenderer(EntityRendererProvider.Context renderManager, AnimatedTickingGeoModel<DragonEntity> modelProvider) {
        super(renderManager, modelProvider);
    }

    public RenderType getRenderType(DragonEntity animatable, float partialTicks, PoseStack stack,
                                    MultiBufferSource renderTypeBuffer, VertexConsumer vertexBuilder, int packedLightIn,
                                    ResourceLocation textureLocation) {
        return RenderType.entityCutoutNoCull(getTextureLocation(animatable));
    }

    public boolean shouldRender(@NotNull DragonEntity livingEntityIn, @NotNull Frustum camera, double camX, double camY, double camZ) {
        if (super.shouldRender(livingEntityIn, camera, camX, camY, camZ)) {
            return true;
        } else {
            for(DragonPart part : livingEntityIn.dragonParts){
                if(camera.isVisible(part.getBoundingBox())){
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public void renderEarly(DragonEntity animatable, PoseStack stackIn, float ticks, MultiBufferSource renderTypeBuffer, VertexConsumer vertexBuilder, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float partialTicks) {
        super.renderEarly(animatable, stackIn, ticks, renderTypeBuffer, vertexBuilder, packedLightIn, packedOverlayIn, red, green, blue, partialTicks);
        stackIn.scale(animatable.getMonsterScale(),animatable.getMonsterScale(),animatable.getMonsterScale());
    }
}
