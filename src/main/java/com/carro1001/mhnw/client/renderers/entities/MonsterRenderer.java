package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.entities.Monster;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;


public class MonsterRenderer extends GeoEntityRenderer<Monster> {
    private final ResourceLocation RESOURCE_LOCATION;

    public MonsterRenderer(EntityRendererProvider.Context context, GeoModel<Monster> model, String name) {
        super(context, model);
        RESOURCE_LOCATION = new ResourceLocation(MODID, "textures/entity/"+ name +".png");
    }

    @Override
    public RenderType getRenderType(Monster animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(getTextureLocation(animatable));
    }

    public void render(Monster pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
        this.shadowRadius = pEntity.getMonsterScale();
        super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
    }

    @Override
    public void preRender(PoseStack poseStack, Monster animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        poseStack.scale(animatable.getMonsterScale(),animatable.getMonsterScale(),animatable.getMonsterScale());
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull Monster pEntity) {
        return RESOURCE_LOCATION;
    }
}