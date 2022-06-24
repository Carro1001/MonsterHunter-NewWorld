package com.carro1001.mhnw.setup;

import com.carro1001.mhnw.entities.aptonoth.AptonothModel;
import com.carro1001.mhnw.entities.aptonoth.AptonothRenderer;
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
        event.registerLayerDefinition(AptonothModel.LAYER_LOCATION, AptonothModel::createBodyLayer);
   }
    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.AddLayers event) {
        GeoArmorRenderer.registerArmorRenderer(BoneArmorItem.class, new BoneArmorRenderer());
    }
    @SubscribeEvent
    public static void onRegisterRenderer(EntityRenderersEvent.RegisterRenderers event) {

        event.registerEntityRenderer(Registration.APTONOTH.get(), AptonothRenderer::new);

    }
}
