package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.client.models.entities.IzuchiModel;
import com.carro1001.mhnw.entities.NewWorldGrowingEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import static com.carro1001.mhnw.utils.MHNWReferences.IZUCHI;

public class IzuchiRenderer extends NewWorldGrowingEntityRenderer<NewWorldGrowingEntity> {

    public IzuchiRenderer(EntityRendererProvider.Context context) {
        super(context, new IzuchiModel(),IZUCHI);
    }

}
