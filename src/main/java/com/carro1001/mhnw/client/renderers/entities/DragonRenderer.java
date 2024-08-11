package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.entities.NewWorldGrowingEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public abstract class DragonRenderer extends NewWorldGrowingEntityRenderer {

    public DragonRenderer(EntityRendererProvider.Context renderManager, DefaultedEntityGeoModel<NewWorldGrowingEntity> modelProvider, String name) {
        super(renderManager, modelProvider, name);
    }
}
