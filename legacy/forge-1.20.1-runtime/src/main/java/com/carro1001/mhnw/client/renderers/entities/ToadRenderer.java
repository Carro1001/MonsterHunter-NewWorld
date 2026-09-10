package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.client.models.entities.ToadModel;
import com.carro1001.mhnw.entities.ToadEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import static com.carro1001.mhnw.client.models.entities.ToadModel.*;

public class ToadRenderer extends GeoEntityRenderer<ToadEntity> {

    public ToadRenderer(EntityRendererProvider.Context context) {
        super(context, new ToadModel());
    }
    @Override
    public RenderType getRenderType(ToadEntity animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(getTextureLocation(animatable));
    }

    @Override
    public float getMotionAnimThreshold(ToadEntity animatable) {
        return  0.005f;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ToadEntity pEntity) {
        return switch (pEntity.getTypeDir()) {
            case 0 -> RESOURCE_LOCATION_POISON;
            case 1 -> RESOURCE_LOCATION_SLEEP;
            case 2 -> RESOURCE_LOCATION_PARAS;
            default -> RESOURCE_LOCATION_BLAST;
        };
    }
}
