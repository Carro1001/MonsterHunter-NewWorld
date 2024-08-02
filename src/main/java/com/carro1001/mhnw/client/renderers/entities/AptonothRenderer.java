package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.client.models.entities.AptonothModel;
import com.carro1001.mhnw.entities.AptonothEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import static com.carro1001.mhnw.utils.MHNWReferences.APTONOTH;
import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class AptonothRenderer extends GeoEntityRenderer<AptonothEntity> {
    private static final ResourceLocation RESOURCE_LOCATION =  new ResourceLocation(MODID, "textures/entity/"+ APTONOTH +".png");

    public AptonothRenderer(EntityRendererProvider.Context context) {
        super(context, new AptonothModel());
    }

    @Override
    public RenderType getRenderType(AptonothEntity animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(getTextureLocation(animatable));
    }

    public void render(AptonothEntity pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
        this.shadowRadius = pEntity.getMonsterScale();
        super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
    }

    @Override
    public void preRender(PoseStack poseStack, AptonothEntity animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        poseStack.scale(animatable.getMonsterScale(),animatable.getMonsterScale(),animatable.getMonsterScale());
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull AptonothEntity pEntity) {
        return RESOURCE_LOCATION;
    }
}
