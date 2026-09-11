package com.carro1001.mhnw.client;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.entity.GreatIzuchi;
import com.carro1001.mhnw.registry.ModEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Client-only wiring. Nothing in common or server code may reference this class or anything it
 * imports: a dedicated server with no rendering clients must make identical damage decisions
 * (handoff section 4.4).
 */
@EventBusSubscriber(modid = MHNW.MOD_ID, value = Dist.CLIENT)
public final class MHNWClient {

    @SubscribeEvent
    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GREAT_IZUCHI.get(), GreatIzuchiRenderer::new);
    }

    /**
     * Renders the preserved geometry, animations and texture unchanged.
     * {@link DefaultedEntityGeoModel} resolves exactly the layout the original artists used:
     * {@code geo/entity/great_izuchi.geo.json}, {@code animations/entity/great_izuchi.animation.json}
     * and {@code textures/entity/great_izuchi.png}.
     */
    public static class GreatIzuchiRenderer extends GeoEntityRenderer<GreatIzuchi> {
        public GreatIzuchiRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<>(
                    ResourceLocation.fromNamespaceAndPath(MHNW.MOD_ID, "great_izuchi")));
            this.shadowRadius = 1.0F;
        }

        @Override
        public void render(GreatIzuchi entity, float entityYaw, float partialTick,
                           com.mojang.blaze3d.vertex.PoseStack poseStack,
                           net.minecraft.client.renderer.MultiBufferSource bufferSource,
                           int packedLight) {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            BoneProbe.maybeLog(entity, getGeoModel());
            AttackVolumeOverlay.render(entity, poseStack, bufferSource, partialTick);
        }

        /**
         * Vanilla flops a corpse 90 degrees onto its side while it dies. This creature has an
         * authored death clip that already lays it down, so the vanilla rotation fought it and
         * drove the body through the floor face-first. Zero hands the pose entirely to the clip.
         */
        @Override
        protected float getDeathMaxRotation(GreatIzuchi entity) {
            return 0.0F;
        }
    }

    private MHNWClient() {}
}
