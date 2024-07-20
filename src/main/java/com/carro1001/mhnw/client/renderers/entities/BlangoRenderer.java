package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.client.models.entities.BlangoModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import static com.carro1001.mhnw.utils.MHNWReferences.BLANGO;

public class BlangoRenderer extends MonsterRenderer {

    public BlangoRenderer(EntityRendererProvider.Context context) {
        super(context, new BlangoModel(),BLANGO);
    }

}
