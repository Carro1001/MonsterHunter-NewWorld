package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.entities.NewWorldMonsterEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public abstract class DragonRenderer extends NewWorldGrowingEntityRenderer<NewWorldMonsterEntity> {
    private String tailName = "tailclub";

    public DragonRenderer(EntityRendererProvider.Context renderManager, DefaultedEntityGeoModel<NewWorldMonsterEntity> modelProvider, String name) {
        super(renderManager, modelProvider, name);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, NewWorldMonsterEntity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if(bone.getName().equals(tailName) && animatable.isTailCut()){
            bone.setHidden(true);
            bone.setChildrenHidden(true);
        }
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
