package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.client.models.entities.DeviljhoModel;
import com.carro1001.mhnw.entities.NewWorldGrowingEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import static com.carro1001.mhnw.utils.MHNWReferences.DEVILJHO;

public class DeviljhoRenderer extends NewWorldGrowingEntityRenderer<NewWorldGrowingEntity> {

    public DeviljhoRenderer(EntityRendererProvider.Context context) {
        super(context, new DeviljhoModel(), DEVILJHO);
    }

}
