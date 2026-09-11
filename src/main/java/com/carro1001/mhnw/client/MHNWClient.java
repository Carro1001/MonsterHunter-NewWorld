package com.carro1001.mhnw.client;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.client.models.BugModel;
import com.carro1001.mhnw.entity.Aptonoth;
import com.carro1001.mhnw.entity.Bug;
import com.carro1001.mhnw.entity.Flashbug;
import com.carro1001.mhnw.entity.GreatIzuchi;
import com.carro1001.mhnw.entity.Izuchi;
import com.carro1001.mhnw.entity.Lagiacrus;
import com.carro1001.mhnw.entity.Rathian;
import com.carro1001.mhnw.entity.Rathalos;
import com.carro1001.mhnw.entity.Toad;
import com.carro1001.mhnw.registry.ModEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
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
        event.registerEntityRenderer(ModEntities.BUG.get(), BugRenderer::new);
        event.registerEntityRenderer(ModEntities.IZUCHI.get(), IzuchiRenderer::new);
        event.registerEntityRenderer(ModEntities.RATHIAN.get(), RathianRenderer::new);
        event.registerEntityRenderer(ModEntities.RATHALOS.get(), RathalosRenderer::new);
        event.registerEntityRenderer(ModEntities.LAGIACRUS.get(), LagiacrusRenderer::new);
    }

    /**
     * The layer definition {@link BugModel} bakes its {@code ModelPart} tree from. Vanilla-style
     * hand-modeled entities register this separately from the renderer itself, unlike the GeckoLib
     * species above, which resolve their geometry straight from the preserved {@code .geo.json}.
     */
    @SubscribeEvent
    private static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BugModel.LAYER_LOCATION, BugModel::createBodyLayer);
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
            boolean attacking = entity.getAttackId() != GreatIzuchi.ATTACK_NONE;
            BoneProbe.maybeLog("great_izuchi", entity, getGeoModel(), BoneProbe.GREAT_IZUCHI_BONES, attacking);
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

    /** Reuses the preserved geometry/animations/texture; the bone probe hook is the only addition,
     * added once Aptonoth's own offline-solved part offsets needed the same "measure it" check its
     * multipart siblings get. */
    public static class AptonothRenderer extends GeoEntityRenderer<Aptonoth> {
        public AptonothRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<>(
                    ResourceLocation.fromNamespaceAndPath(MHNW.MOD_ID, "aptonoth")));
            this.shadowRadius = 0.6F;
        }

        @Override
        public void render(Aptonoth entity, float entityYaw, float partialTick,
                           com.mojang.blaze3d.vertex.PoseStack poseStack,
                           net.minecraft.client.renderer.MultiBufferSource bufferSource,
                           int packedLight) {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            BoneProbe.maybeLog("aptonoth", entity, getGeoModel(), BoneProbe.APTONOTH_BONES, false);
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

    /**
     * A bioluminescent creature, not a plain reuse: the legacy runtime's own renderer forced a
     * minimum block light level of 12 so its glowing yellow segments actually read as glowing
     * rather than going dark in normal ambient light, which this port had dropped (the reported
     * "kinda darker than intended, like a dark yellow" is exactly what ordinary diffuse lighting
     * does to this texture without that floor).
     */
    public static class FlashbugRenderer extends GeoEntityRenderer<Flashbug> {
        public FlashbugRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<>(
                    ResourceLocation.fromNamespaceAndPath(MHNW.MOD_ID, "flashbug")));
            this.shadowRadius = 0.15F;
        }

        @Override
        protected int getBlockLightLevel(Flashbug entity, net.minecraft.core.BlockPos pos) {
            return Math.max(12, super.getBlockLightLevel(entity, pos));
        }
    }

    /**
     * The one hand-modeled species. {@link BugModel} bakes from {@link BugModel#LAYER_LOCATION}
     * (registered above), and the texture is picked per entity from its {@link Bug.Variant} the
     * same way {@link ToadGeoModel} does for the toad, just through the vanilla renderer API
     * ({@code getTextureLocation}) instead of GeckoLib's.
     */
    public static class BugRenderer extends MobRenderer<Bug, BugModel<Bug>> {
        public BugRenderer(EntityRendererProvider.Context context) {
            super(context, new BugModel<>(context.bakeLayer(BugModel.LAYER_LOCATION)), 0.2F);
        }

        @Override
        public ResourceLocation getTextureLocation(Bug entity) {
            return ResourceLocation.fromNamespaceAndPath(MHNW.MOD_ID,
                    "textures/entity/" + entity.getVariant().textureName + ".png");
        }
    }

    /** Plain reuse of the preserved geometry/animations/texture; no death rotation override needed
     * since this species has no authored death clip for vanilla's own flop to fight. */
    public static class IzuchiRenderer extends GeoEntityRenderer<Izuchi> {
        public IzuchiRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<>(
                    ResourceLocation.fromNamespaceAndPath(MHNW.MOD_ID, "izuchi")));
            this.shadowRadius = 0.5F;
        }
    }

    /**
     * Plain reuse of the preserved geometry/animations/texture; no death rotation override since
     * {@code death} is an authored clip and vanilla's flop is disabled the same way Great Izuchi's
     * is, for the same reason (the clip already lays the body down).
     */
    public static class RathianRenderer extends GeoEntityRenderer<Rathian> {
        public RathianRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<>(
                    ResourceLocation.fromNamespaceAndPath(MHNW.MOD_ID, "rathian")));
            this.shadowRadius = 1.4F;
        }

        @Override
        public void render(Rathian entity, float entityYaw, float partialTick,
                           com.mojang.blaze3d.vertex.PoseStack poseStack,
                           net.minecraft.client.renderer.MultiBufferSource bufferSource,
                           int packedLight) {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            BoneProbe.maybeLog("rathian", entity, getGeoModel(), BoneProbe.WYVERN_BONES, false);
        }

        @Override
        protected float getDeathMaxRotation(Rathian entity) {
            return 0.0F;
        }
    }

    /** Same treatment as {@link RathianRenderer}: authored death clip, vanilla flop disabled. */
    public static class RathalosRenderer extends GeoEntityRenderer<Rathalos> {
        public RathalosRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<>(
                    ResourceLocation.fromNamespaceAndPath(MHNW.MOD_ID, "rathalos")));
            this.shadowRadius = 1.4F;
        }

        @Override
        public void render(Rathalos entity, float entityYaw, float partialTick,
                           com.mojang.blaze3d.vertex.PoseStack poseStack,
                           net.minecraft.client.renderer.MultiBufferSource bufferSource,
                           int packedLight) {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            BoneProbe.maybeLog("rathalos", entity, getGeoModel(), BoneProbe.WYVERN_BONES, false);
        }

        @Override
        protected float getDeathMaxRotation(Rathalos entity) {
            return 0.0F;
        }
    }

    /** Preserved assets and vanilla death rotation; no authored death clip exists. */
    public static class LagiacrusRenderer extends GeoEntityRenderer<Lagiacrus> {
        public LagiacrusRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<>(
                    ResourceLocation.fromNamespaceAndPath(MHNW.MOD_ID, "lagiacrus")));
            this.shadowRadius = 1.0F;
        }

        @Override
        public void render(Lagiacrus entity, float entityYaw, float partialTick,
                           com.mojang.blaze3d.vertex.PoseStack poseStack,
                           net.minecraft.client.renderer.MultiBufferSource bufferSource,
                           int packedLight) {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            BoneProbe.maybeLog("lagiacrus", entity, getGeoModel(), BoneProbe.LAGIACRUS_BONES, false);
        }
    }

    private MHNWClient() {}
}
