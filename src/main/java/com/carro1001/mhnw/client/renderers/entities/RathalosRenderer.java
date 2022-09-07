package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.client.models.entities.RathalosModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class RathalosRenderer extends DragonRenderer {

    public RathalosRenderer(EntityRendererProvider.Context context) {
        super(context, new RathalosModel());
    }

}
