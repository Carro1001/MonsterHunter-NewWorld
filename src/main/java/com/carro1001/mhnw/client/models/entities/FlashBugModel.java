package com.carro1001.mhnw.client.models.entities;

import com.carro1001.mhnw.entities.FlashBugEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

import static com.carro1001.mhnw.utils.MHNWReferences.FLASHBUG;
import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class FlashBugModel extends DefaultedEntityGeoModel<FlashBugEntity> {

    public FlashBugModel() {
        super(new ResourceLocation(MODID, FLASHBUG), true);
    }
}
