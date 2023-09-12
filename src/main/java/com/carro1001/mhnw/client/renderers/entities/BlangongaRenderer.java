package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.client.models.entities.BlangongaModel;
import com.carro1001.mhnw.entities.BlangongaEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import static com.carro1001.mhnw.utils.MHNWReferences.BLANGONGA;
import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class BlangongaRenderer extends GeoEntityRenderer<BlangongaEntity> {
    private static final ResourceLocation RESOURCE_LOCATION =  new ResourceLocation(MODID, "textures/entity/"+ BLANGONGA +".png");

    public BlangongaRenderer(EntityRendererProvider.Context context) {
        super(context, new BlangongaModel());
    }
    @Override
    public RenderType getRenderType(BlangongaEntity animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(getTextureLocation(animatable));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull BlangongaEntity pEntity) {
        return RESOURCE_LOCATION;
    }
}
