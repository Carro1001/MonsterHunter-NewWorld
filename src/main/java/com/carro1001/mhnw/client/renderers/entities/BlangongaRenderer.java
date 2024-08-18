package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.client.models.entities.BlangongaModel;
import com.carro1001.mhnw.entities.NewWorldGrowingEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import static com.carro1001.mhnw.utils.MHNWReferences.BLANGONGA;

public class BlangongaRenderer extends NewWorldGrowingEntityRenderer<NewWorldGrowingEntity> {

    public BlangongaRenderer(EntityRendererProvider.Context context) {
        super(context, new BlangongaModel(),BLANGONGA);
    }

}
