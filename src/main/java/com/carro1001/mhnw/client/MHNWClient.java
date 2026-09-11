package com.carro1001.mhnw.client;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.entity.Aptonoth;
import com.carro1001.mhnw.entity.Flashbug;
import com.carro1001.mhnw.entity.GreatIzuchi;
import com.carro1001.mhnw.entity.Toad;
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
        event.registerEntityRenderer(ModEntities.APTONOTH.get(), AptonothRenderer::new);
        event.registerEntityRenderer(ModEntities.TOAD.get(), ToadRenderer::new);
        event.registerEntityRenderer(ModEntities.FLASHBUG.get(), FlashbugRenderer::new);
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

    /** Plain reuse of the preserved geometry/animations/texture; nothing species-specific to add. */
    public static class AptonothRenderer extends GeoEntityRenderer<Aptonoth> {
        public AptonothRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<>(
                    ResourceLocation.fromNamespaceAndPath(MHNW.MOD_ID, "aptonoth")));
            this.shadowRadius = 0.6F;
        }
    }

    /**
     * Toad has no single base texture: only the four variant skins were preserved
     * ({@code poisontoad.png}, {@code sleeptoad.png}, {@code paratoad.png}, {@code blastoad.png}).
     * {@link ToadGeoModel} picks the right one per entity; the geometry and animation are shared.
     */
    public static class ToadRenderer extends GeoEntityRenderer<Toad> {
        public ToadRenderer(EntityRendererProvider.Context context) {
            super(context, new ToadGeoModel());
            this.shadowRadius = 0.3F;
        }
    }

    private static class ToadGeoModel extends DefaultedEntityGeoModel<Toad> {
        ToadGeoModel() {
            super(ResourceLocation.fromNamespaceAndPath(MHNW.MOD_ID, "toad"));
        }

        @Override
        public ResourceLocation getTextureResource(Toad animatable) {
            return ResourceLocation.fromNamespaceAndPath(MHNW.MOD_ID,
                    "textures/entity/" + animatable.getVariant().textureName + ".png");
        }
    }

    /** Plain reuse of the preserved geometry/animations/texture; nothing species-specific to add. */
    public static class FlashbugRenderer extends GeoEntityRenderer<Flashbug> {
        public FlashbugRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<>(
                    ResourceLocation.fromNamespaceAndPath(MHNW.MOD_ID, "flashbug")));
            this.shadowRadius = 0.15F;
        }
    }

    private MHNWClient() {}
}
