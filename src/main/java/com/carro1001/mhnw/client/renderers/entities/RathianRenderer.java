package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.client.models.entities.RathianModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class RathianRenderer extends DragonRenderer {
    public RathianRenderer(EntityRendererProvider.Context context) {
        super(context, new RathianModel());
    }
}
