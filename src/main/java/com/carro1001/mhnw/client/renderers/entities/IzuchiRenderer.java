package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.client.models.entities.IzuchiModel;
import com.carro1001.mhnw.entities.IzuchiEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import static com.carro1001.mhnw.utils.MHNWReferences.IZUCHI;
import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class IzuchiRenderer extends GeoEntityRenderer<IzuchiEntity> {
    private static final ResourceLocation RESOURCE_LOCATION =  new ResourceLocation(MODID, "textures/entity/"+ IZUCHI +".png");

    public IzuchiRenderer(EntityRendererProvider.Context context) {
        super(context, new IzuchiModel());
    }
    @Override
    public RenderType getRenderType(IzuchiEntity animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(getTextureLocation(animatable));
    }
    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull IzuchiEntity pEntity) {
        return RESOURCE_LOCATION;
    }
}
