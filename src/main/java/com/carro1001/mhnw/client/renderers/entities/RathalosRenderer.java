package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.client.models.entities.RathalosModel;
import com.carro1001.mhnw.entities.DragonEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;
import static com.carro1001.mhnw.utils.MHNWReferences.RATHALOS;

public class RathalosRenderer extends DragonRenderer {
    private static final ResourceLocation RESOURCE_LOCATION =  new ResourceLocation(MODID, "textures/entity/"+ RATHALOS +".png");

    public RathalosRenderer(EntityRendererProvider.Context context) {
        super(context, new RathalosModel());
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull DragonEntity pEntity) {
        return RESOURCE_LOCATION;
    }
}
