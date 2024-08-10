package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.client.models.entities.AptonothModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import static com.carro1001.mhnw.utils.MHNWReferences.APTONOTH;

public class AptonothRenderer extends MonsterRenderer {

    public AptonothRenderer(EntityRendererProvider.Context context) {
        super(context, new AptonothModel(), APTONOTH);
    }

}
