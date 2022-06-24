package com.carro1001.mhnw.entities.aptonoth;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

public class AptonothModel<T extends AptonothEntity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("mhnw", "aptonoth"), "main");
    private final ModelPart root;

    public AptonothModel(ModelPart root){
        this.root = root.getChild("root");
    }
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-10.0F, -13.0F, -30.0F, 20.0F, 26.0F, 53.0F, new CubeDeformation(0.0F))
                .texOffs(0, 80).addBox(-3.5F, -17.0F, -23.0F, 7.0F, 4.0F, 46.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -29.7F, 0.0F, 0.2182F, 0.0F, 0.0F));

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

        PartDefinition leftLeg1 = hips.addOrReplaceChild("leftLeg1", CubeListBuilder.create().texOffs(88, 125).addBox(-5.0F, -4.0F, -10.0F, 9.0F, 19.0F, 19.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(8.0F, -2.3F, -1.0F, -0.2182F, 0.0F, 0.0F));

        PartDefinition leftLeg2 = leftLeg1.addOrReplaceChild("leftLeg2", CubeListBuilder.create().texOffs(194, 204).addBox(-3.5F, -3.0F, -2.0F, 8.0F, 13.0F, 13.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, 15.0F, -4.0F, 0.3491F, 0.0F, 0.0F));

        PartDefinition leftLeg3 = leftLeg2.addOrReplaceChild("leftLeg3", CubeListBuilder.create().texOffs(208, 86).addBox(-3.5F, -2.0F, -7.0F, 7.0F, 14.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, 11.0F, 7.0F, -0.3491F, 0.0F, 0.0F));

        PartDefinition leftLeg4 = leftLeg3.addOrReplaceChild("leftLeg4", CubeListBuilder.create().texOffs(215, 124).addBox(-3.5F, 0.0F, -7.0F, 8.0F, 4.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offset(-0.5F, 10.0F, -1.2F));

        PartDefinition rightLeg1 = hips.addOrReplaceChild("rightLeg1", CubeListBuilder.create().texOffs(88, 125).mirror().addBox(-4.0F, -4.0F, -10.0F, 9.0F, 19.0F, 19.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-8.0F, -2.3F, -1.0F, -0.2182F, 0.0F, 0.0F));

        PartDefinition rightLeg2 = rightLeg1.addOrReplaceChild("rightLeg2", CubeListBuilder.create().texOffs(194, 204).mirror().addBox(-4.5F, -3.0F, -2.0F, 8.0F, 13.0F, 13.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(1.0F, 15.0F, -4.0F, 0.3491F, 0.0F, 0.0F));

        PartDefinition rightLeg3 = rightLeg2.addOrReplaceChild("rightLeg3", CubeListBuilder.create().texOffs(208, 86).mirror().addBox(-3.5F, -2.0F, -7.0F, 7.0F, 14.0F, 8.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-0.5F, 11.0F, 7.0F, -0.3491F, 0.0F, 0.0F));

        PartDefinition rightLeg4 = rightLeg3.addOrReplaceChild("rightLeg4", CubeListBuilder.create().texOffs(215, 124).mirror().addBox(-4.5F, 0.0F, -7.0F, 8.0F, 4.0F, 9.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.5F, 10.0F, -1.2F));

        PartDefinition leftArm1 = body.addOrReplaceChild("leftArm1", CubeListBuilder.create().texOffs(0, 131).addBox(-4.0F, -3.0F, -5.0F, 9.0F, 15.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offset(6.0F, 2.0F, -17.0F));

        PartDefinition leftArm2 = leftArm1.addOrReplaceChild("leftArm2", CubeListBuilder.create().texOffs(71, 192).addBox(-4.0F, -1.0F, -6.0F, 8.0F, 13.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, 9.5F, 0.3F, -0.3491F, 0.0F, 0.0F));

        PartDefinition leftArm3 = leftArm2.addOrReplaceChild("leftArm3", CubeListBuilder.create().texOffs(129, 217).addBox(-4.0F, 0.0F, -4.0F, 9.0F, 5.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, 10.3F, -2.4F, 0.1309F, 0.0F, 0.0F));

        PartDefinition rightArm1 = body.addOrReplaceChild("rightArm1", CubeListBuilder.create().texOffs(0, 131).mirror().addBox(-5.0F, -3.0F, -5.0F, 9.0F, 15.0F, 9.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-6.0F, 2.0F, -17.0F));

        PartDefinition rightArm2 = rightArm1.addOrReplaceChild("rightArm2", CubeListBuilder.create().texOffs(71, 192).mirror().addBox(-4.0F, -1.0F, -6.0F, 8.0F, 13.0F, 9.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-0.5F, 9.5F, 0.3F, -0.3491F, 0.0F, 0.0F));

        PartDefinition rightArm3 = rightArm2.addOrReplaceChild("rightArm3", CubeListBuilder.create().texOffs(129, 217).mirror().addBox(-5.0F, 0.0F, -4.0F, 9.0F, 5.0F, 9.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.5F, 10.3F, -2.4F, 0.1309F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 256, 256);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        root.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
