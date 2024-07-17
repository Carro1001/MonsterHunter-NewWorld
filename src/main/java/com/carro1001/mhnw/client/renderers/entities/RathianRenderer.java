package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.client.models.entities.RathianModel;
import com.carro1001.mhnw.entities.DragonEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;
import static com.carro1001.mhnw.utils.MHNWReferences.RATHIAN;

public class RathianRenderer extends DragonRenderer {
    private static final ResourceLocation RESOURCE_LOCATION =  new ResourceLocation(MODID, "textures/entity/"+ RATHIAN +".png");

    public RathianRenderer(EntityRendererProvider.Context context) {
        super(context, new RathianModel());
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull DragonEntity pEntity) {
        return RESOURCE_LOCATION;
    }
}
