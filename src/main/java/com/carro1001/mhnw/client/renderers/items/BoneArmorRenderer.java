package com.carro1001.mhnw.client.renderers.items;

import com.carro1001.mhnw.items.BoneArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class BoneArmorRenderer extends GeoArmorRenderer<BoneArmorItem> {
    public BoneArmorRenderer() {
        super(new DefaultedItemGeoModel<>(new ResourceLocation(MODID, "armor/bone_armor")));
    }
}
