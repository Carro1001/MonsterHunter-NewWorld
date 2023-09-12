package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.client.models.entities.BlangoModel;
import com.carro1001.mhnw.entities.BlangoEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import static com.carro1001.mhnw.utils.MHNWReferences.BLANGO;
import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class BlangoRenderer extends GeoEntityRenderer<BlangoEntity> {
    private static final ResourceLocation RESOURCE_LOCATION =  new ResourceLocation(MODID, "textures/entity/"+ BLANGO +".png");

    public BlangoRenderer(EntityRendererProvider.Context context) {
        super(context, new BlangoModel());
    }
    @Override
    public RenderType getRenderType(BlangoEntity animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(getTextureLocation(animatable));
    }
    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull BlangoEntity pEntity) {
        return RESOURCE_LOCATION;
    }
}
