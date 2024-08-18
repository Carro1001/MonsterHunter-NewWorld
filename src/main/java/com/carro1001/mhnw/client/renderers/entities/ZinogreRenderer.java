package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.client.models.entities.ZinogreModel;
import com.carro1001.mhnw.entities.NewWorldGrowingEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import static com.carro1001.mhnw.utils.MHNWReferences.ZINOGRE;

public class ZinogreRenderer extends NewWorldGrowingEntityRenderer<NewWorldGrowingEntity> {

    public ZinogreRenderer(EntityRendererProvider.Context context) {
        super(context, new ZinogreModel(),ZINOGRE);
    }

}
