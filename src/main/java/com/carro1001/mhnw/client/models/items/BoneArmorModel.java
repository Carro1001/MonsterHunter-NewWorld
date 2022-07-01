package com.carro1001.mhnw.client.models.items;

import com.carro1001.mhnw.items.BoneArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class BoneArmorModel extends AnimatedGeoModel<BoneArmorItem> {

    @Override
    public ResourceLocation getModelLocation(BoneArmorItem object) {
        return new ResourceLocation(MODID, "geo/bone_armor.geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(BoneArmorItem object) {
        return new ResourceLocation(MODID, "textures/item/bone_armor.png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(BoneArmorItem animatable) {
        return new ResourceLocation(MODID, "animations/bone_armor.animation.json");
    }
}
