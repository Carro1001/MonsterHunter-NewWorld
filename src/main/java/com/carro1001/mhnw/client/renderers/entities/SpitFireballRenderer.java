package com.carro1001.mhnw.client.renderers.entities;

import com.carro1001.mhnw.entities.SpitFireball;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;
import static com.carro1001.mhnw.utils.MHNWReferences.SPIT_FIREBALL;
@OnlyIn(Dist.CLIENT)
public class SpitFireballRenderer extends EntityRenderer<SpitFireball> {
    public static final ResourceLocation CUBE_FIREBALL_LOCATION = new ResourceLocation(MODID, "textures/entity/"+ SPIT_FIREBALL +".png");
    public static final ResourceLocation CUBE_FIREBALL_SPRITE = new ResourceLocation(MODID, "entity/"+ SPIT_FIREBALL);

    public SpitFireballRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }



    protected int getBlockLightLevel(SpitFireball pEntity, BlockPos pPos) {
        return 15;
    }

    public void render(SpitFireball pEntity, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight) {
        int brightness = LightTexture.FULL_BRIGHT;

        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(CUBE_FIREBALL_SPRITE);
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        pMatrixStack.pushPose();

        pMatrixStack.translate(0.0D, 0.0D, 0.0D);

        pMatrixStack.mulPose(Vector3f.YP.rotationDegrees(Mth.lerp(pPartialTicks, pEntity.yRotO, pEntity.getYRot()) - 90.0F));
        pMatrixStack.mulPose(Vector3f.ZP.rotationDegrees(Mth.lerp(pPartialTicks, pEntity.xRotO, pEntity.getXRot())));
        // Actually render the quad in our own custom render type
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutoutMipped());
        Matrix4f matrix = pMatrixStack.last().pose();
        // Vertex data has to appear in a specific order:
        buffer.vertex(matrix, -0.8f, -0.8f, 0.0f).color(1.0f, 1.0f, 1.0f, 1f).uv(sprite.getU0(), sprite.getV0()).uv2(brightness).normal(0,0,1).endVertex();
        buffer.vertex(matrix, -0.8f, 0.8f, 0.0f).color(1.0f, 1.0f, 1.0f, 1f).uv(sprite.getU0(), sprite.getV1()).uv2(brightness).normal(0,0,1).endVertex();
        buffer.vertex(matrix, 0.8f, 0.8f, 0.0f).color(1.0f, 1.0f, 1.0f, 1f).uv(sprite.getU1(), sprite.getV1()).uv2(brightness).normal(0,0,1).endVertex();
        buffer.vertex(matrix, 0.8f, -0.8f, 0.0f).color(1.0f, 1.0f, 1.0f, 1f).uv(sprite.getU1(), sprite.getV0()).uv2(brightness).normal(0,0,1).endVertex();

        pMatrixStack.popPose();
        RenderSystem.disableDepthTest();
        super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
    }

    public void vertex(Matrix4f pMatrix, Matrix3f pNormals, VertexConsumer pVertexBuilder, int pOffsetX, int pOffsetY, int pOffsetZ, float pTextureX, float pTextureY, int pNormalX, int p_113835_, int p_113836_, int pPackedLight) {
        pVertexBuilder.vertex(pMatrix, (float)pOffsetX, (float)pOffsetY, (float)pOffsetZ).color(255, 255, 255, 255).uv(pTextureX, pTextureY).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(pPackedLight).normal(pNormals, (float)pNormalX, (float)p_113836_, (float)p_113835_).endVertex();
    }
    /**
     * Returns the location of an entity's texture.
     */
    public ResourceLocation getTextureLocation(SpitFireball pEntity) {
        return CUBE_FIREBALL_LOCATION;
    }
}