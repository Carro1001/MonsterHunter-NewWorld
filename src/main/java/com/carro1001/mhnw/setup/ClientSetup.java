package com.carro1001.mhnw.setup;

import com.carro1001.mhnw.client.models.entities.BugModel;
import com.carro1001.mhnw.client.renderers.entities.*;
import com.carro1001.mhnw.registration.ModRenderTypes;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import static com.carro1001.mhnw.registration.ModEntities.*;
import static com.carro1001.mhnw.registration.RegistrationHelper.register;
import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {
    public static ModelLayerLocation CUBE_MODEL = register("cube_fireball");

    public static void init(FMLClientSetupEvent event) {
        ModRenderTypes.init();
    }

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BugModel.LAYER_LOCATION, BugModel::createBodyLayer);
        //event.registerLayerDefinition(CubeFireballModel.LAYER_LOCATION, CubeFireballModel::createMobHeadLayer);
        //event.registerLayerDefinition(CUBE_MODEL, CubeFireballRenderer::createSkullLayer);
    }

    @SubscribeEvent
    public static void onRegisterRenderer(EntityRenderersEvent.RegisterRenderers event) {

        event.registerEntityRenderer(APTONOTH.get(), AptonothRenderer::new);
        event.registerEntityRenderer(RATHIAN.get(), RathianRenderer::new);
        event.registerEntityRenderer(RATHALOS.get(), RathalosRenderer::new);
        event.registerEntityRenderer(RATH_TAIL.get(), RathalosTailRenderer::new);
        event.registerEntityRenderer(RATHALOS_FIREBALL.get(), pContext ->  new ThrownItemRenderer<>(pContext, 3.0F, true));
        event.registerEntityRenderer(BITTERBUG.get(), BugRenderer::new);
        event.registerEntityRenderer(TOAD.get(), ToadRenderer::new);
        event.registerEntityRenderer(ZINOGRE.get(), ZinogreRenderer::new);
        event.registerEntityRenderer(IZUCHI.get(), IzuchiRenderer::new);
        event.registerEntityRenderer(GIZUCHI.get(), GreatIzuchiRenderer::new);
        event.registerEntityRenderer(BLANGONGA.get(), BlangongaRenderer::new);
        event.registerEntityRenderer(BLANGO.get(), BlangoRenderer::new);
        event.registerEntityRenderer(FLASHBUG.get(), FlashBugRenderer::new);
        event.registerEntityRenderer(DEVILJHO.get(), DeviljhoRenderer::new);
        event.registerEntityRenderer(LAGIACRUS.get(), LagiacrusRenderer::new);

    }
    @SubscribeEvent
    public static void onTextureStitch(TextureStitchEvent.Post event) {
        if (!event.getAtlas().location().equals(TextureAtlas.LOCATION_BLOCKS)) {
        }
        //event.getAtlas().(CUBE_FIREBALL_SPRITE);
    }
}
