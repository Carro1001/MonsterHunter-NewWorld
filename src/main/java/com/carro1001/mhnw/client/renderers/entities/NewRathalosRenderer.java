package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.client.models.entities.NewRathalosModel;
import com.carro1001.mhnw.entities.NewRathalosEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class NewRathalosRenderer extends GeoEntityRenderer<NewRathalosEntity> {

    public NewRathalosRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new NewRathalosModel());
    }

    public RenderType getRenderType(NewRathalosEntity animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(getTextureLocation(animatable));
    }

    public boolean shouldRender(@NotNull NewRathalosEntity livingEntityIn, @NotNull Frustum camera, double camX, double camY, double camZ) {
        if (super.shouldRender(livingEntityIn, camera, camX, camY, camZ)) {
            return true;
        } else {
            // TODO: Multipart support
//            for(DragonPart part : livingEntityIn.dragonParts){
//                if(camera.isVisible(part.getBoundingBox())){
//                    return true;
//                }
//            }
            return false;
        }
    }

    @Override
    public void preRender(PoseStack poseStack, NewRathalosEntity animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        float monsterScale = animatable.getMonsterScale();
        poseStack.scale(monsterScale, monsterScale, monsterScale);
    }

}
