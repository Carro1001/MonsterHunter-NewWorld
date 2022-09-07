package com.carro1001.mhnw.setup;

import com.carro1001.mhnw.client.models.entities.BitterbugModel;
import com.carro1001.mhnw.client.models.entities.ToadModel;
import com.carro1001.mhnw.client.renderers.entities.*;
import com.carro1001.mhnw.client.renderers.items.BoneArmorRenderer;
import com.carro1001.mhnw.items.BoneArmorItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import software.bernie.geckolib3.renderers.geo.GeoArmorRenderer;

import static com.carro1001.mhnw.registration.ModEntities.*;
import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    public static void init(FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(ForgeHooksClient.ClientEvents.class);
    }

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BitterbugModel.LAYER_LOCATION, BitterbugModel::createBodyLayer);
        event.registerLayerDefinition(ToadModel.LAYER_LOCATION, ToadModel::createBodyLayer);
   }
    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.AddLayers event) {
        GeoArmorRenderer.registerArmorRenderer(BoneArmorItem.class, new BoneArmorRenderer());
    }
    @SubscribeEvent
    public static void onRegisterRenderer(EntityRenderersEvent.RegisterRenderers event) {

        event.registerEntityRenderer(APTONOTH.get(), AptonothRenderer::new);
        event.registerEntityRenderer(RATHIAN.get(), RathianRenderer::new);
        event.registerEntityRenderer(RATHALOS.get(), RathalosRenderer::new);
        event.registerEntityRenderer(BITTERBUG.get(), BitterbugRenderer::new);
        event.registerEntityRenderer(TOAD.get(), ToadRenderer::new);
        event.registerEntityRenderer(ZINOGRE.get(), ZinogreRenderer::new);
        event.registerEntityRenderer(IZUCHI.get(), IzuchiRenderer::new);
        event.registerEntityRenderer(GIZUCHI.get(), GreatIzuchiRenderer::new);
        event.registerEntityRenderer(BLANGONGA.get(), BlangongaRenderer::new);
        event.registerEntityRenderer(BLANGO.get(), BlangoRenderer::new);

    }
}
