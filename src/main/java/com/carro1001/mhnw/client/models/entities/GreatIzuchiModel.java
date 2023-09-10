package com.carro1001.mhnw.client.models.entities;

import com.carro1001.mhnw.entities.GreatIzuchiEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

import static com.carro1001.mhnw.utils.MHNWReferences.*;

public class GreatIzuchiModel extends DefaultedEntityGeoModel<GreatIzuchiEntity> {

    public GreatIzuchiModel() {
        super(new ResourceLocation(MODID, GREAT+"_"+IZUCHI), true);
    }

    @Override
    public ResourceLocation getModelResource(GreatIzuchiEntity object)
    {
        return new ResourceLocation(MODID, "geo/"+ GREAT+"_"+IZUCHI +".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GreatIzuchiEntity object)
    {
        return new ResourceLocation(MODID, "textures/entity/"+ GREAT+"_"+IZUCHI +".png");
    }

    @Override
    public ResourceLocation getAnimationResource(GreatIzuchiEntity object)
    {
        return new ResourceLocation(MODID, "animations/"+ GREAT+"_"+IZUCHI +".animation.json");
    }

}
