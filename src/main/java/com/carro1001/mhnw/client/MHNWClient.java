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
import com.carro1001.mhnw.registry.ModItems;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import com.carro1001.mhnw.item.BoneArmorItem;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
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
        // R2's thrown flash bomb. Vanilla's own item-projectile renderer draws it from the stack
        // the projectile carries, so it needs no model, no class of ours, and no texture.
        event.registerEntityRenderer(ModEntities.FLASH_BOMB.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
    }

    /**
     * Leans the Giant Jawblade back as its charge builds, the same way vanilla's bow swaps to its
     * pulling models.
     *
     * <p>Registers one {@code mhnw:charge} property whose value the weapon itself computes from
     * vanilla's use countdown ({@link com.carro1001.mhnw.item.GiantJawbladeItem#chargeProgress}),
     * and the item model's own {@code overrides} pick a per-tier model from it. No renderer class,
     * no animation controller and no packet: the client already knows how long the holder has been
     * using the item, so the pose needs nothing sent to it.
     */
    @SubscribeEvent
    private static void onClientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> net.minecraft.client.renderer.item.ItemProperties.register(
                ModItems.GIANT_JAWBLADE.get(),
                ResourceLocation.fromNamespaceAndPath(MHNW.MOD_ID, "charge"),
                (stack, level, holder, seed) ->
                        com.carro1001.mhnw.item.GiantJawbladeItem.chargeProgress(stack, holder)));
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
     * GeckoLib's default overlay red-tints anything with {@code deathTime > 0}. For a carvable
     * species that is the whole 12,000-tick corpse window (see {@code CarveState}), not vanilla's
     * 20 ticks, so the body would sit there glowing red until it expired. Keep the hurt flash,
     * drop the death tint.
     */
    private static int overlayWithoutDeathTint(net.minecraft.world.entity.LivingEntity entity, float u) {
        return net.minecraft.client.renderer.texture.OverlayTexture.pack(
                net.minecraft.client.renderer.texture.OverlayTexture.u(u),
                net.minecraft.client.renderer.texture.OverlayTexture.v(entity.hurtTime > 0));
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


        /** Corpses stay carvable for a long time; no red death tint. @see #overlayWithoutDeathTint */
        @Override
        public int getPackedOverlay(GreatIzuchi animatable, float u, float partialTick) {
            return overlayWithoutDeathTint(animatable, u);
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

        /**
         * Same reason Great Izuchi zeroes this, with a sharper symptom. Aptonoth's death clip rolls
         * the body -90 degrees about Z over its 20 ticks; vanilla's own flop adds +90 about the same
         * axis over its own ~20, so the two cancel and the corpse ends the clip standing upright
         * again (sunk into the floor by the clip's own -1.125 block body drop). The clip already
         * lays the body down -- vanilla must not also.
         */
        @Override
        protected float getDeathMaxRotation(Aptonoth entity) {
            return 0.0F;
        }

        /** Corpses stay carvable for a long time; no red death tint. @see #overlayWithoutDeathTint */
        @Override
        public int getPackedOverlay(Aptonoth animatable, float u, float partialTick) {
            return overlayWithoutDeathTint(animatable, u);
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

        /**
         * The wings. GeckoLib's default is {@code entityCutoutNoCull}, which has no partial alpha
         * at all -- a texel is either fully drawn or fully discarded -- so the wing texture's
         * semi-transparent pixels rendered solid. Translucent is the render type that keeps them.
         */
        @Override
        public net.minecraft.client.renderer.RenderType getRenderType(
                Flashbug animatable, ResourceLocation texture,
                net.minecraft.client.renderer.MultiBufferSource bufferSource, float partialTick) {
            return animatable.isInvisible()
                    ? super.getRenderType(animatable, texture, bufferSource, partialTick)
                    : net.minecraft.client.renderer.RenderType.entityTranslucent(texture);
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

    /** Preserved geometry/texture plus the recovered brain-branch tail swipe. */
    public static class IzuchiRenderer extends GeoEntityRenderer<Izuchi> {
        public IzuchiRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<>(
                    ResourceLocation.fromNamespaceAndPath(MHNW.MOD_ID, "izuchi")));
            this.shadowRadius = 0.5F;
        }

        @Override
        public void render(Izuchi entity, float entityYaw, float partialTick,
                           com.mojang.blaze3d.vertex.PoseStack poseStack,
                           net.minecraft.client.renderer.MultiBufferSource bufferSource,
                           int packedLight) {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            boolean attacking = entity.getAttackId() != Izuchi.ATTACK_NONE;
            BoneProbe.maybeLog("izuchi", entity, getGeoModel(), BoneProbe.IZUCHI_BONES, attacking);
            AttackVolumeOverlay.render(entity, poseStack, bufferSource, partialTick);
        }

        /** Corpses stay carvable for a long time; no red death tint. @see #overlayWithoutDeathTint */
        @Override
        public int getPackedOverlay(Izuchi animatable, float u, float partialTick) {
            return overlayWithoutDeathTint(animatable, u);
        }

        /**
         * The retargeted death clip rotates {@code root} 90 degrees about Z itself, so vanilla's
         * own corpse flop would add a second 90 on the same axis -- the exact fault that put
         * Aptonoth back on its feet. Zero hands the pose entirely to the clip.
         */
        @Override
        protected float getDeathMaxRotation(Izuchi entity) {
            return 0.0F;
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
            boolean attacking = entity.getAttackId() != Rathian.ATTACK_NONE;
            BoneProbe.maybeLog("rathian", entity, getGeoModel(), BoneProbe.WYVERN_BONES, attacking);
            AttackVolumeOverlay.render(entity, poseStack, bufferSource, partialTick);
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

    /**
     * The worn bone armor.
     *
     * <p>Points at {@code geo/entity/bone_armor.geo.json}, the original complete export, and maps
     * all eight of its real bone names. GeckoLib's defaults look for {@code armorHead} and friends;
     * this export predates that convention and uses {@code Head}, {@code Body}, {@code RightArm},
     * {@code LeftArm}, {@code RightLeg}, {@code LeftLeg}, {@code RightBoot}, {@code LeftBoot}. The
     * newer {@code geo/item/armor/} copy does use the default names but has no boot bones at all,
     * so pointing a stock renderer at it would silently render bare feet. See
     * {@link BoneArmorItem} for the full history.
     */
    public static class BoneArmorRenderer extends GeoArmorRenderer<BoneArmorItem> {
        public BoneArmorRenderer() {
            super(new BoneArmorModel());
        }

        @Override
        public GeoBone getHeadBone(GeoModel<BoneArmorItem> model) {
            return model.getBone("Head").orElse(null);
        }

        @Override
        public GeoBone getBodyBone(GeoModel<BoneArmorItem> model) {
            return model.getBone("Body").orElse(null);
        }

        @Override
        public GeoBone getRightArmBone(GeoModel<BoneArmorItem> model) {
            return model.getBone("RightArm").orElse(null);
        }

        @Override
        public GeoBone getLeftArmBone(GeoModel<BoneArmorItem> model) {
            return model.getBone("LeftArm").orElse(null);
        }

        @Override
        public GeoBone getRightLegBone(GeoModel<BoneArmorItem> model) {
            return model.getBone("RightLeg").orElse(null);
        }

        @Override
        public GeoBone getLeftLegBone(GeoModel<BoneArmorItem> model) {
            return model.getBone("LeftLeg").orElse(null);
        }

        @Override
        public GeoBone getRightBootBone(GeoModel<BoneArmorItem> model) {
            return model.getBone("RightBoot").orElse(null);
        }

        @Override
        public GeoBone getLeftBootBone(GeoModel<BoneArmorItem> model) {
            return model.getBone("LeftBoot").orElse(null);
        }
    }

    /**
     * Static geometry and one texture. {@code DefaultedItemGeoModel} would resolve the
     * {@code geo/item/} layout, which is the incomplete copy, so the three paths are stated
     * outright. The animation file is the preserved empty one -- GeckoLib requires the resource to
     * exist, and this armor has nothing to animate.
     */
    public static class BoneArmorModel extends GeoModel<BoneArmorItem> {
        private static final ResourceLocation MODEL =
                ResourceLocation.fromNamespaceAndPath(MHNW.MOD_ID, "geo/entity/bone_armor.geo.json");
        private static final ResourceLocation TEXTURE =
                ResourceLocation.fromNamespaceAndPath(MHNW.MOD_ID, "textures/item/armor/bone_armor.png");
        private static final ResourceLocation ANIMATION =
                ResourceLocation.fromNamespaceAndPath(MHNW.MOD_ID, "animations/item/armor/bone_armor.animation.json");

        @Override
        public ResourceLocation getModelResource(BoneArmorItem animatable) {
            return MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(BoneArmorItem animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(BoneArmorItem animatable) {
            return ANIMATION;
        }
    }

    private MHNWClient() {}
}
