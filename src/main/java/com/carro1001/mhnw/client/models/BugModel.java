package com.carro1001.mhnw.client.models;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.entity.Bug;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * Ported unchanged (mesh, cube values, procedural animation formulas) from the archived runtime at
 * {@code legacy/forge-1.20.1-runtime/src/main/java/com/carro1001/mhnw/client/models/entities/BugModel.java},
 * only the package and the mod-id/entity-package references updated for the new build. Preserving
 * this Java-authored mesh exactly is the whole point of bringing it forward (handoff section 2.1:
 * "This is also a model asset; a resources-only copy would lose it").
 *
 * <p>This is a vanilla-style hand-modeled {@link EntityModel}, not a GeckoLib asset: {@link Bug}
 * does not implement {@code GeoEntity}, and the leg/antennae motion below is evaluated every frame
 * from limb-swing and age, the same mechanism any vanilla mob's walk cycle uses.
 */
public class BugModel<T extends Bug> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(MHNW.MOD_ID, "bitterbug"), "main");

    private final ModelPart body;
    private final ModelPart frontleftleg;
    private final ModelPart frontrightleg;
    private final ModelPart midrightleg;
    private final ModelPart midleftleg;
    private final ModelPart backleftleg;
    private final ModelPart backrightleg;
    private final ModelPart antennaeleft;
    private final ModelPart antennaeright;

    public BugModel(ModelPart root) {
        this.body = root.getChild("body");
        ModelPart head = body.getChild("head");
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

        PartDefinition body = partdefinition.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-2.0F, -2.0F, -1.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.1F)),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        body.addOrReplaceChild("frontleftleg",
                CubeListBuilder.create().texOffs(1, 12)
                        .addBox(0.5702F, -0.7032F, -0.766F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(1.0F, 0.0F, 1.0F, -0.3696F, 0.6067F, -0.5969F));

        body.addOrReplaceChild("frontrightleg",
                CubeListBuilder.create().texOffs(1, 12).mirror()
                        .addBox(-3.5702F, -0.7032F, -0.766F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F))
                        .mirror(false),
                PartPose.offsetAndRotation(-1.0F, 0.0F, 1.0F, -0.3696F, -0.6067F, 0.5969F));

        body.addOrReplaceChild("midrightleg",
                CubeListBuilder.create().texOffs(1, 12).mirror()
                        .addBox(-3.0F, -1.0F, -1.0F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F))
                        .mirror(false),
                PartPose.offsetAndRotation(-1.0F, 0.0F, 2.0F, 0.0F, 0.0F, 0.3927F));

        body.addOrReplaceChild("midleftleg",
                CubeListBuilder.create().texOffs(1, 12)
                        .addBox(0.0F, -1.0F, -1.0F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(1.0F, 0.0F, 2.0F, 0.0F, 0.0F, -0.3927F));

        body.addOrReplaceChild("backleftleg",
                CubeListBuilder.create().texOffs(1, 12)
                        .addBox(-0.4532F, -1.2113F, -0.866F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(1.0F, 0.0F, 3.0F, 0.2393F, -0.4703F, -0.4939F));

        body.addOrReplaceChild("backrightleg",
                CubeListBuilder.create().texOffs(1, 12).mirror()
                        .addBox(-2.5468F, -1.2113F, -0.866F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F))
                        .mirror(false),
                PartPose.offsetAndRotation(-1.0F, 0.0F, 3.0F, 0.2393F, 0.4703F, 0.4939F));

        PartDefinition head = body.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 6)
                        .addBox(-1.5F, -1.0F, -2.0F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -1.0F, -1.0F));

        PartDefinition antennaeleft = head.addOrReplaceChild("antennaeleft",
                CubeListBuilder.create(), PartPose.offset(1.0F, -1.0F, -2.0F));

        antennaeleft.addOrReplaceChild("cube_r1",
                CubeListBuilder.create().texOffs(7, 6)
                        .addBox(0.0F, -3.0F, 0.0F, 0.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.6109F, 0.0F, 0.3054F));

        PartDefinition antennaeright = head.addOrReplaceChild("antennaeright",
                CubeListBuilder.create(), PartPose.offset(-1.0F, -1.0F, -2.0F));

        antennaeright.addOrReplaceChild("cube_r2",
                CubeListBuilder.create().texOffs(7, 6).mirror()
                        .addBox(0.0F, -3.0F, 0.0F, 0.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
                        .mirror(false),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.6109F, 0.0F, -0.3054F));

        return LayerDefinition.create(meshdefinition, 16, 16);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        float legSwingAngle = (float) (Math.cos(limbSwing * 7F) * 1.8F * limbSwingAmount);

        frontleftleg.xRot = legSwingAngle;
        midleftleg.xRot = -legSwingAngle;
        backleftleg.xRot = legSwingAngle;

        frontrightleg.xRot = -legSwingAngle;
        midrightleg.xRot = legSwingAngle;
        backrightleg.xRot = -legSwingAngle;

        float animationTime = ageInTicks / 20.0F;
        float wiggleFrequency = 0.5F;
        float wiggleAmplitude = 0.35F;
        float antennaeRotation = (float) (wiggleAmplitude
                * Math.sin(animationTime * (2 * (float) Math.PI) * wiggleFrequency));

        antennaeleft.xRot = antennaeRotation;
        antennaeright.xRot = -antennaeRotation;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight,
                               int packedOverlay, int color) {
        body.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
