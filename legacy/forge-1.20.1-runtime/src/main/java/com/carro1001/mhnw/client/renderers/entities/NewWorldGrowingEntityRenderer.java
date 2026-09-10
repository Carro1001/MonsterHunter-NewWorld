package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.entities.NewWorldGrowingEntity;
import com.mojang.blaze3d.vertex.PoseStack;
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

public class NewWorldGrowingEntityRenderer<T extends NewWorldGrowingEntity> extends GeoEntityRenderer<T> {
    private final ResourceLocation RESOURCE_LOCATION;
    protected final ResourceLocation TAIL_CUT_RESOURCE_LOCATION;

    public NewWorldGrowingEntityRenderer(EntityRendererProvider.Context context, GeoModel<T> model, String name) {
        super(context, model);
        RESOURCE_LOCATION = new ResourceLocation(MODID, "textures/entity/"+ name +".png");
        TAIL_CUT_RESOURCE_LOCATION = new ResourceLocation(MODID, "textures/entity/"+ name +"_tailcut.png");
    }

    @Override
    protected float getDeathMaxRotation(T entityLivingBaseIn) {
        return 0.0F;
    }
    @Override
    public float getMotionAnimThreshold(T animatable) {
        return 0.005f;
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack, T animatable, BakedGeoModel model, boolean isReRender, float partialTick, int packedLight, int packedOverlay) {
        widthScale = animatable.getMonsterScale();
        heightScale = animatable.getMonsterScale();
        super.scaleModelForRender(widthScale, heightScale, poseStack, animatable, model, isReRender, partialTick, packedLight, packedOverlay);
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(getTextureLocation(animatable));
    }

    public void render(T pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
        this.shadowRadius = pEntity.getMonsterScale();
        super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull T pEntity) {
        return RESOURCE_LOCATION;
    }
}