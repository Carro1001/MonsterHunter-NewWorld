package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.client.models.entities.BitterbugModel;
import com.carro1001.mhnw.entities.BitterbugEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import static com.carro1001.mhnw.utils.MHNWReferences.*;

public class BitterbugRenderer extends MobRenderer<BitterbugEntity, BitterbugModel<BitterbugEntity>> {
    private static final ResourceLocation RESOURCE_LOCATION_BITTER =  new ResourceLocation(MODID, "textures/entity/"+ BITTERBUG +".png");
    private static final ResourceLocation RESOURCE_LOCATION_GOD =  new ResourceLocation(MODID, "textures/entity/"+ GODBUG +".png");


    public BitterbugRenderer(EntityRendererProvider.Context context) {
        super(context, new BitterbugModel<>(context.bakeLayer(BitterbugModel.LAYER_LOCATION)), 0.2F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(BitterbugEntity pEntity) {
        return pEntity.getTypeDir() == 0 ? RESOURCE_LOCATION_BITTER : RESOURCE_LOCATION_GOD;
    }
}
