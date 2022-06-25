package com.carro1001.mhnw.entities.aptonoth;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class AptonothModel extends AnimatedGeoModel<AptonothEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("mhnw", "aptonoth"), "main");
    private final ModelPart body;

    @Override
    public ResourceLocation getModelLocation(AptonothEntity object)
    {
        return new ResourceLocation(MODID, "geo/aptonoth.geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(AptonothEntity object)
    {
        return new ResourceLocation(MODID, "textures/entity/aptonoth.png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(AptonothEntity object)
    {
        return new ResourceLocation(MODID, "animations/aptonoth.animation.json");
    }

    public AptonothModel(ModelPart root){
        this.body = root.getChild("body");
    }
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-10.0F, -13.0F, -30.0F, 20.0F, 26.0F, 53.0F, new CubeDeformation(0.0F))
                .texOffs(0, 80).addBox(-3.5F, -17.0F, -23.0F, 7.0F, 4.0F, 46.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -5.7F, 0.0F, 0.2182F, 0.0F, 0.0F));

        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(0, 80).addBox(-4.0F, -21.0F, -7.0F, 8.0F, 26.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.0F, -28.0F, 0.6109F, 0.0F, 0.0F));

        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(128, 105).addBox(-4.5F, -7.0F, -13.0F, 9.0F, 7.0F, 20.0F, new CubeDeformation(0.0F))
                .texOffs(0, 7).addBox(4.5F, -6.0F, -2.0F, 4.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(0, 7).mirror().addBox(-8.5F, -6.0F, -2.0F, 4.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(11, 10).addBox(4.5F, -3.0F, -3.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(11, 10).mirror().addBox(-6.5F, -3.0F, -3.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, -20.0F, -5.0F, -0.4363F, 0.0F, 0.0F));

        PartDefinition horn = head.addOrReplaceChild("horn", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -2.0F, -4.0F, 6.0F, 4.0F, 20.0F, new CubeDeformation(0.0F))
                .texOffs(61, 80).addBox(-5.0F, -2.0F, 16.0F, 10.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(0, 12).addBox(5.0F, -1.0F, 17.0F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(0, 12).mirror().addBox(-8.0F, -1.0F, 17.0F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(0, 0).addBox(2.0F, -1.0F, 20.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0).mirror().addBox(-4.0F, -1.0F, 20.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, -6.0F, 2.0F, 0.1745F, 0.0F, 0.0F));

        PartDefinition hips = body.addOrReplaceChild("hips", CubeListBuilder.create(), PartPose.offset(0.0F, -2.3F, 14.0F));

        PartDefinition tail1 = hips.addOrReplaceChild("tail1", CubeListBuilder.create().texOffs(61, 80).addBox(-6.0F, -4.0F, 0.0F, 12.0F, 13.0F, 31.0F, new CubeDeformation(0.0F))
                .texOffs(117, 49).addBox(-2.5F, -8.0F, 0.0F, 5.0F, 4.0F, 31.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -4.7F, 9.0F, -0.1309F, 0.0F, 0.0F));

        PartDefinition tail2 = tail1.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(94, 0).addBox(-4.0F, -2.0F, 0.0F, 8.0F, 10.0F, 32.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.0F, 31.0F, -0.2618F, 0.0F, 0.0F));

        PartDefinition thagomizer_end_left = tail2.addOrReplaceChild("thagomizer_end_left", CubeListBuilder.create().texOffs(0, 25).addBox(-3.0F, -1.0F, 0.0F, 4.0F, 2.0F, 19.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, 2.0F, 31.0F, -0.1309F, 0.2618F, 0.0F));

        PartDefinition thagomizer_end_right = tail2.addOrReplaceChild("thagomizer_end_right", CubeListBuilder.create().texOffs(0, 25).mirror().addBox(-1.0F, -1.0F, 0.0F, 4.0F, 2.0F, 19.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-4.0F, 2.0F, 31.0F, -0.1309F, -0.2618F, 0.0F));

        PartDefinition thagomizer_top_left = tail2.addOrReplaceChild("thagomizer_top_left", CubeListBuilder.create().texOffs(94, 43).addBox(0.0F, -2.0F, -3.0F, 16.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.6F, 1.0F, 27.0F, 0.0F, -0.0873F, -0.2182F));

        PartDefinition thagomizer_top_right = tail2.addOrReplaceChild("thagomizer_top_right", CubeListBuilder.create().texOffs(94, 43).mirror().addBox(-16.0F, -2.0F, -3.0F, 16.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-3.6F, 1.0F, 27.0F, 0.0F, 0.0873F, 0.2182F));

        PartDefinition thagomizer_bottom_left = tail2.addOrReplaceChild("thagomizer_bottom_left", CubeListBuilder.create().texOffs(0, 47).addBox(0.0F, -1.0F, -3.0F, 13.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.0F, 5.0F, 27.0F, 0.0F, 0.2618F, 0.4363F));

        PartDefinition thagomizer_bottom_right = tail2.addOrReplaceChild("thagomizer_bottom_right", CubeListBuilder.create().texOffs(0, 47).mirror().addBox(-13.0F, -1.0F, -3.0F, 13.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-3.0F, 5.0F, 27.0F, 0.0F, -0.2618F, -0.4363F));

        PartDefinition leg_right = hips.addOrReplaceChild("leg_right", CubeListBuilder.create().texOffs(88, 125).mirror().addBox(-4.0F, -4.0F, -10.0F, 9.0F, 19.0F, 19.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-8.0F, -2.3F, -1.0F, -0.2182F, 0.0F, 0.0F));

        PartDefinition knee_right = leg_right.addOrReplaceChild("knee_right", CubeListBuilder.create().texOffs(37, 131).mirror().addBox(-4.5F, 0.0F, -4.0F, 8.0F, 22.0F, 9.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(1.0F, 15.0F, 3.0F));

        PartDefinition leg_left = hips.addOrReplaceChild("leg_left", CubeListBuilder.create().texOffs(88, 125).addBox(-5.0F, -4.0F, -10.0F, 9.0F, 19.0F, 19.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(8.0F, -2.3F, -1.0F, -0.2182F, 0.0F, 0.0F));

        PartDefinition knee_left = leg_left.addOrReplaceChild("knee_left", CubeListBuilder.create().texOffs(37, 131).addBox(-3.5F, 0.0F, -4.0F, 8.0F, 22.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.0F, 15.0F, 3.0F));

        PartDefinition left_arm = body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 131).addBox(-4.0F, -8.0F, -5.0F, 9.0F, 28.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.0F, 6.0F, -17.0F, -0.2182F, 0.0F, 0.0F));

        PartDefinition right_arm = body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 131).mirror().addBox(-5.0F, -8.0F, -5.0F, 9.0F, 28.0F, 9.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-6.0F, 6.0F, -17.0F, -0.2182F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 256, 256);
    }
}
