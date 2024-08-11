package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.client.models.entities.DeviljhoModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import static com.carro1001.mhnw.utils.MHNWReferences.DEVILJHO;

public class DeviljhoRenderer extends NewWorldGrowingEntityRenderer {

    public DeviljhoRenderer(EntityRendererProvider.Context context) {
        super(context, new DeviljhoModel(), DEVILJHO);
    }

}
