package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.client.models.entities.GreatIzuchiModel;
import com.carro1001.mhnw.entities.NewWorldGrowingEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import static com.carro1001.mhnw.utils.MHNWReferences.GREAT;
import static com.carro1001.mhnw.utils.MHNWReferences.IZUCHI;

public class GreatIzuchiRenderer extends NewWorldGrowingEntityRenderer<NewWorldGrowingEntity> {

    public GreatIzuchiRenderer(EntityRendererProvider.Context context) {
        super(context, new GreatIzuchiModel(), GREAT+"_"+IZUCHI);
    }

}
