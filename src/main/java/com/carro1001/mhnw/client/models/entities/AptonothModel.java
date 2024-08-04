package com.carro1001.mhnw.client.models.entities;

import com.carro1001.mhnw.entities.AptonothEntity;
import com.carro1001.mhnw.utils.MHNWReferences;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class AptonothModel extends DefaultedEntityGeoModel<AptonothEntity> {

    public AptonothModel() {
        super(new ResourceLocation(MODID, MHNWReferences.APTONOTH), true);
    }

}
