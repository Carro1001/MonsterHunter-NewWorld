package com.carro1001.mhnw.client.models.entities;

import com.carro1001.mhnw.entities.LargeMonster;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class MonsterModel extends DefaultedEntityGeoModel<LargeMonster> {
    String name;
    public MonsterModel(String name) {
        super(new ResourceLocation(MODID, name), false);
        this.name = name;
    }

    @Override
    public ResourceLocation getModelResource(LargeMonster object)
    {
        return new ResourceLocation(MODID, "geo/"+ name +".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(LargeMonster object)
    {
        return new ResourceLocation(MODID, "textures/entity/"+ name +".png");
    }

    @Override
    public ResourceLocation getAnimationResource(LargeMonster object)
    {
        return new ResourceLocation(MODID, "animations/"+ name +".animation.json");
    }

}
