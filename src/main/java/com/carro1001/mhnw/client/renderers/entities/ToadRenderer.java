package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.client.models.entities.ToadModel;
import com.carro1001.mhnw.entities.ToadEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import static com.carro1001.mhnw.utils.MHNWReferences.*;

public class ToadRenderer extends MobRenderer<ToadEntity, ToadModel<ToadEntity>> {
    private static final ResourceLocation RESOURCE_LOCATION_POISON =  new ResourceLocation(MODID, "textures/entity/"+ POISON+TOAD +".png");
    private static final ResourceLocation RESOURCE_LOCATION_SLEEP =  new ResourceLocation(MODID, "textures/entity/"+ SLEEP+TOAD +".png");
    private static final ResourceLocation RESOURCE_LOCATION_PARAS =  new ResourceLocation(MODID, "textures/entity/"+ PARALIZE+TOAD +".png");
    private static final ResourceLocation RESOURCE_LOCATION_BLAST =  new ResourceLocation(MODID, "textures/entity/"+ BLAST+TOAD +".png");


    public ToadRenderer(EntityRendererProvider.Context context) {
        super(context, new ToadModel<>(context.bakeLayer(ToadModel.LAYER_LOCATION)), 0.2F);
    }

    @Override
    protected void scale(ToadEntity pLivingEntity, PoseStack pMatrixStack, float pPartialTickTime) {
        float f = pLivingEntity.getSwelling(pPartialTickTime);
        float f1 = 1.0F + Mth.sin(f * 100.0F) * f * 0.01F;
        f = Mth.clamp(f, 0.0F, 1.0F);
        f *= f;
        f *= f;
        float f2 = (1.0F + f * 0.4F) * f1;
        float f3 = (1.0F + f * 0.1F) / f1;
        pMatrixStack.scale(f2, f3, f2);
    }

    @Override
    protected float getWhiteOverlayProgress(ToadEntity pLivingEntity, float pPartialTicks) {
        float f = pLivingEntity.getSwelling(pPartialTicks);
        return (int)(f * 10.0F) % 2 == 0 ? 0.0F : Mth.clamp(f, 0.5F, 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(ToadEntity pEntity) {
        return switch (pEntity.getTypeDir()) {
            case 0 -> RESOURCE_LOCATION_POISON;
            case 1 -> RESOURCE_LOCATION_SLEEP;
            case 2 -> RESOURCE_LOCATION_PARAS;
            default -> RESOURCE_LOCATION_BLAST;
        };
    }
}
