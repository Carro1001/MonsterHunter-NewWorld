package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.client.models.entities.FlashBugModel;
import com.carro1001.mhnw.entities.FlashBugEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import static com.carro1001.mhnw.utils.MHNWReferences.FLASHBUG;
import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class FlashBugRenderer extends GeoEntityRenderer<FlashBugEntity> {
    private static final ResourceLocation RESOURCE_LOCATION =  new ResourceLocation(MODID, "textures/entity/"+ FLASHBUG +".png");

    public FlashBugRenderer(EntityRendererProvider.Context context) {
        super(context, new FlashBugModel());
    }
    @Override
    public RenderType getRenderType(FlashBugEntity animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(getTextureLocation(animatable));
    }

    @Override
    protected int getBlockLightLevel(FlashBugEntity pEntity, BlockPos pPos) {
        return 12;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull FlashBugEntity pEntity) {
        return RESOURCE_LOCATION;
    }
}
