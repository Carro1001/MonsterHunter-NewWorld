package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.client.models.entities.LagiacrusModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import static com.carro1001.mhnw.utils.MHNWReferences.LAGIACRUZ;

public class LagiacrusRenderer extends NewWorldGrowingEntityRenderer {

    public LagiacrusRenderer(EntityRendererProvider.Context context) {
        super(context, new LagiacrusModel(), LAGIACRUZ);
    }


}
