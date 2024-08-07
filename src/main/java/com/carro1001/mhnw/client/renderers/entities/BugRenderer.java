package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.client.models.entities.BugModel;
import com.carro1001.mhnw.entities.BugEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import static com.carro1001.mhnw.utils.MHNWReferences.*;

public class BugRenderer extends MobRenderer<BugEntity, BugModel<BugEntity>> {
    private static final ResourceLocation RESOURCE_LOCATION_BITTER =  new ResourceLocation(MODID, "textures/entity/"+ BITTERBUG +".png");
    private static final ResourceLocation RESOURCE_LOCATION_GOD =  new ResourceLocation(MODID, "textures/entity/"+ GODBUG +".png");


    public BugRenderer(EntityRendererProvider.Context context) {
        super(context, new BugModel<>(context.bakeLayer(BugModel.LAYER_LOCATION)), 0.2F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(BugEntity pEntity) {
        return pEntity.getTypeDir() == 0 ? RESOURCE_LOCATION_BITTER : RESOURCE_LOCATION_GOD;
    }
}
