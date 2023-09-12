package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.client.models.entities.AptonothModel;
import com.carro1001.mhnw.entities.AptonothEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull AptonothEntity pEntity) {
        return RESOURCE_LOCATION;
    }
}
