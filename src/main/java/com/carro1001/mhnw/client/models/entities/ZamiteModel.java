package com.carro1001.mhnw.client.models.entities;

import com.carro1001.mhnw.entities.ZamiteEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;
import static com.carro1001.mhnw.utils.MHNWReferences.ZAMITE;

public class ZamiteModel extends AnimatedGeoModel<ZamiteEntity> {
    @Override
    public ResourceLocation getModelLocation(ZamiteEntity object)
    {
        return new ResourceLocation(MODID, "geo/"+ ZAMITE +".geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(ZamiteEntity object)
    {
        return new ResourceLocation(MODID, "textures/entity/"+ ZAMITE +".png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(ZamiteEntity object)
    {
        return new ResourceLocation(MODID, "animations/"+ ZAMITE +".animation.json");
    }

}
