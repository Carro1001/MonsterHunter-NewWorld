package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.client.models.entities.ToadModel;
import com.carro1001.mhnw.entities.ToadEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

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
    public ResourceLocation getTextureLocation(ToadEntity pEntity) {
        return switch (pEntity.getTypeDir()) {
            case 0 -> RESOURCE_LOCATION_POISON;
            case 1 -> RESOURCE_LOCATION_SLEEP;
            case 2 -> RESOURCE_LOCATION_PARAS;
            default -> RESOURCE_LOCATION_BLAST;
        };
    }
}
