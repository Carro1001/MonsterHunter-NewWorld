package com.carro1001.mhnw.client.models.entities;

import com.carro1001.mhnw.entities.LargeMonster;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class MonsterModel extends DefaultedEntityGeoModel<LargeMonster> {
    public MonsterModel(String name) {
        super(new ResourceLocation(MODID, name), false);
    }
}
