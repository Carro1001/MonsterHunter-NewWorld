package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.client.models.entities.RathalosTailModel;
import com.carro1001.mhnw.entities.NewWorldGrowingEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import static com.carro1001.mhnw.utils.MHNWReferences.RATHALOS;

public class RathalosTailRenderer extends NewWorldGrowingEntityRenderer<NewWorldGrowingEntity> {

    public RathalosTailRenderer(EntityRendererProvider.Context context) {
        super(context, new RathalosTailModel(), RATHALOS + "_tail");
    }

}
