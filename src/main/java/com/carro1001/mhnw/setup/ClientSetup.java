package com.carro1001.mhnw.setup;

import com.carro1001.mhnw.entities.aptonoth.AptonothRenderer;
import com.carro1001.mhnw.entities.bitterbug.BitterbugModel;
import com.carro1001.mhnw.entities.bitterbug.BitterbugRenderer;
import com.carro1001.mhnw.entities.blango.BlangoRenderer;
import com.carro1001.mhnw.entities.blangonga.BlangongaRenderer;
import com.carro1001.mhnw.entities.greatIzuchi.GreatIzuchiRenderer;
import com.carro1001.mhnw.entities.izuchi.IzuchiRenderer;
import com.carro1001.mhnw.entities.rathalos.RathalasRenderer;
import com.carro1001.mhnw.entities.rathian.RathianRenderer;
import com.carro1001.mhnw.entities.toad.ToadModel;
import com.carro1001.mhnw.entities.toad.ToadRenderer;
import com.carro1001.mhnw.entities.zamite.ZamiteRenderer;
import com.carro1001.mhnw.entities.zinogre.ZinogreRenderer;
import com.carro1001.mhnw.items.bone_armor.BoneArmorItem;
import com.carro1001.mhnw.items.bone_armor.BoneArmorRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import software.bernie.geckolib3.renderers.geo.GeoArmorRenderer;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    public static void init(FMLClientSetupEvent event) {
        event.enqueueWork( () -> {
            ItemBlockRenderTypes.setRenderLayer(Registration.EARTH_CRYSTAL_CLUSTER_BLOCK.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(Registration.ICE_CRYSTAL_CLUSTER_BLOCK.get(), RenderType.cutout());
        });
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

        event.registerEntityRenderer(Registration.APTONOTH.get(), AptonothRenderer::new);
        event.registerEntityRenderer(Registration.RATHIAN.get(), RathianRenderer::new);
        event.registerEntityRenderer(Registration.RATHALOS.get(), RathalasRenderer::new);
        event.registerEntityRenderer(Registration.BITTERBUG.get(), BitterbugRenderer::new);
        event.registerEntityRenderer(Registration.TOAD.get(), ToadRenderer::new);
        event.registerEntityRenderer(Registration.ZINOGRE.get(), ZinogreRenderer::new);
        event.registerEntityRenderer(Registration.ZAMITE.get(), ZamiteRenderer::new);
        event.registerEntityRenderer(Registration.IZUCHI.get(), IzuchiRenderer::new);
        event.registerEntityRenderer(Registration.GIZUCHI.get(), GreatIzuchiRenderer::new);
        event.registerEntityRenderer(Registration.BLANGONGA.get(), BlangongaRenderer::new);
        event.registerEntityRenderer(Registration.BLANGO.get(), BlangoRenderer::new);

    }
}
