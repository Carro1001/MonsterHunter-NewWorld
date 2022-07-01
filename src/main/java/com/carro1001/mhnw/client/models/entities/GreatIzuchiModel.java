package com.carro1001.mhnw.client.models.entities;

import com.carro1001.mhnw.entities.GreatIzuchiEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

import static com.carro1001.mhnw.utils.MHNWReferences.*;

public class GreatIzuchiModel extends AnimatedGeoModel<GreatIzuchiEntity> {
    @Override
    public ResourceLocation getModelLocation(GreatIzuchiEntity object)
    {
        return new ResourceLocation(MODID, "geo/"+ GREAT+"_"+IZUCHI +".geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(GreatIzuchiEntity object)
    {
        return new ResourceLocation(MODID, "textures/entity/"+ GREAT+"_"+IZUCHI +".png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(GreatIzuchiEntity object)
    {
        return new ResourceLocation(MODID, "animations/"+ GREAT+"_"+IZUCHI +".animation.json");
    }

}
