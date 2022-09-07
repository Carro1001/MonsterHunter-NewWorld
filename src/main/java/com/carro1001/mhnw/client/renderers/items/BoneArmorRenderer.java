package com.carro1001.mhnw.client.renderers.items;

import com.carro1001.mhnw.client.models.items.BoneArmorModel;
import com.carro1001.mhnw.items.BoneArmorItem;
import software.bernie.geckolib3.renderers.geo.GeoArmorRenderer;

public class BoneArmorRenderer extends GeoArmorRenderer<BoneArmorItem> {
    public BoneArmorRenderer() {
        super(new BoneArmorModel());

        // These values are what each bone name is in blockbench.
        // The default values are the ones that come with the default armor template in
        // the geckolib blockbench plugin.
        this.headBone = "Head";
        this.bodyBone = "Body";
        this.rightArmBone = "RightArm";
        this.leftArmBone = "LeftArm";
        this.rightLegBone = "RightLeg";
        this.leftLegBone = "LeftLeg";
        this.rightBootBone = "RightBoot";
        this.leftBootBone = "LeftBoot";
    }
}
