package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.entities.NewWorldMonsterEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public abstract class DragonRenderer extends MonsterRenderer {

    public DragonRenderer(EntityRendererProvider.Context renderManager, DefaultedEntityGeoModel<NewWorldMonsterEntity> modelProvider, String name) {
        super(renderManager, modelProvider, name);
    }
}
