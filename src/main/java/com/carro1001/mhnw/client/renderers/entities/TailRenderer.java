package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.client.models.entities.TailModel;
import com.carro1001.mhnw.entities.NewWorldGrowingEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class TailRenderer extends NewWorldGrowingEntityRenderer<NewWorldGrowingEntity> {

    public TailRenderer(EntityRendererProvider.Context context, String name) {
        super(context, new TailModel(name), name + "_tail");
    }

}
