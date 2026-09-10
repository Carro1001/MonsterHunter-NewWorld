package com.carro1001.mhnw.client.models.entities;

import com.carro1001.mhnw.entities.BugEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class BugModel<T extends BugEntity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(MODID, "bitterbug"), "main");
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart frontleftleg;
    private final ModelPart frontrightleg;
    private final ModelPart midrightleg;
    private final ModelPart midleftleg;
    private final ModelPart backleftleg;
    private final ModelPart backrightleg;
    private final ModelPart antennaeleft;
    private final ModelPart antennaeright;

    public BugModel(ModelPart root){
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.frontleftleg = body.getChild("frontleftleg");
        this.frontrightleg = body.getChild("frontrightleg");
        this.midrightleg = body.getChild("midrightleg");
        this.midleftleg = body.getChild("midleftleg");
        this.backleftleg = body.getChild("backleftleg");
        this.backrightleg = body.getChild("backrightleg");
        this.antennaeleft = head.getChild("antennaeleft");
        this.antennaeright = head.getChild("antennaeright");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, -1.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.1F)), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition frontleftleg = body.addOrReplaceChild("frontleftleg", CubeListBuilder.create().texOffs(1, 12).addBox(0.5702F, -0.7032F, -0.766F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.0F, 0.0F, 1.0F, -0.3696F, 0.6067F, -0.5969F));

        PartDefinition frontrightleg = body.addOrReplaceChild("frontrightleg", CubeListBuilder.create().texOffs(1, 12).mirror().addBox(-3.5702F, -0.7032F, -0.766F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-1.0F, 0.0F, 1.0F, -0.3696F, -0.6067F, 0.5969F));

        PartDefinition midrightleg = body.addOrReplaceChild("midrightleg", CubeListBuilder.create().texOffs(1, 12).mirror().addBox(-3.0F, -1.0F, -1.0F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-1.0F, 0.0F, 2.0F, 0.0F, 0.0F, 0.3927F));

        PartDefinition midleftleg = body.addOrReplaceChild("midleftleg", CubeListBuilder.create().texOffs(1, 12).addBox(0.0F, -1.0F, -1.0F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.0F, 0.0F, 2.0F, 0.0F, 0.0F, -0.3927F));

        PartDefinition backleftleg = body.addOrReplaceChild("backleftleg", CubeListBuilder.create().texOffs(1, 12).addBox(-0.4532F, -1.2113F, -0.866F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.0F, 0.0F, 3.0F, 0.2393F, -0.4703F, -0.4939F));

        PartDefinition backrightleg = body.addOrReplaceChild("backrightleg", CubeListBuilder.create().texOffs(1, 12).mirror().addBox(-2.5468F, -1.2113F, -0.866F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-1.0F, 0.0F, 3.0F, 0.2393F, 0.4703F, 0.4939F));

        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 6).addBox(-1.5F, -1.0F, -2.0F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -1.0F, -1.0F));

        PartDefinition antennaeleft = head.addOrReplaceChild("antennaeleft", CubeListBuilder.create(), PartPose.offset(1.0F, -1.0F, -2.0F));

        PartDefinition cube_r1 = antennaeleft.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(7, 6).addBox(0.0F, -3.0F, 0.0F, 0.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.6109F, 0.0F, 0.3054F));

        PartDefinition antennaeright = head.addOrReplaceChild("antennaeright", CubeListBuilder.create(), PartPose.offset(-1.0F, -1.0F, -2.0F));

        PartDefinition cube_r2 = antennaeright.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(7, 6).mirror().addBox(0.0F, -3.0F, 0.0F, 0.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.6109F, 0.0F, -0.3054F));

        return LayerDefinition.create(meshdefinition, 16, 16);
    }

    @Override
    public void setupAnim(@NotNull T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float legSwingAngle = (float)  (Math.cos(limbSwing * 7F) * 1.8F * limbSwingAmount);

        // Apply rotation to each leg part
        frontleftleg.xRot = legSwingAngle;
        midleftleg.xRot = -legSwingAngle;
        backleftleg.xRot = legSwingAngle;

        frontrightleg.xRot = -legSwingAngle;
        midrightleg.xRot = legSwingAngle;
        backrightleg.xRot = -legSwingAngle;

        // Calculate animation parameters
        float animationTime = ageInTicks / 20.0F; // Convert ticks to seconds
        float wiggleFrequency = 0.5F; // Antennae wiggle every 2 seconds
        float wiggleAmplitude = 0.35F; // Small rotation angle

        // Calculate rotation angle using sine function
        float antennaeRotation = (float) (wiggleAmplitude * Math.sin(animationTime * (2 * (float) Math.PI) * wiggleFrequency));

        // Apply rotation to antennae parts
        antennaeleft.xRot = antennaeRotation;
        antennaeright.xRot = -antennaeRotation;
    }

    @Override
    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
