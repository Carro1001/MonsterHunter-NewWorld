package com.carro1001.mhnw;

import com.carro1001.mhnw.setup.ClientSetup;
import com.carro1001.mhnw.setup.CommonSetup;
import com.carro1001.mhnw.setup.ModConfig;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.NotNull;

import static com.carro1001.mhnw.registration.ModBlocks.BLOCKS;
import static com.carro1001.mhnw.registration.ModBlocks.BLOCK_ENTITIES;
import static com.carro1001.mhnw.registration.ModConfiguredFeature.CONFIGURED_FEATURES;
import static com.carro1001.mhnw.registration.ModEntities.ENTITIES;
import static com.carro1001.mhnw.registration.ModItems.ITEMS;
import static com.carro1001.mhnw.registration.ModItems.WELL_DONE_MEAT_ITEM;
import static com.carro1001.mhnw.registration.ModParticle.PARTICLES;
import static com.carro1001.mhnw.registration.ModPlacedFeature.PLACED_FEATURES;
import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

@Mod(MODID)
public class MHNW {

    public MHNW(){
        ModConfig.register();

        //Registration
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        PARTICLES.register(bus);
        CONFIGURED_FEATURES.register(bus);
        PLACED_FEATURES.register(bus);
        ENTITIES.register(bus);

        IEventBus event = FMLJavaModLoadingContext.get().getModEventBus();
        event.addListener(CommonSetup::init);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> event.addListener(ClientSetup::init));

        ModConfig.loadConfig(ModConfig.CLIENT_CONFIG, FMLPaths.CONFIGDIR.get().resolve(MODID+"-client.toml"));
        ModConfig.loadConfig(ModConfig.SERVER_CONFIG, FMLPaths.CONFIGDIR.get().resolve(MODID+"-common.toml"));
    }

    public static final CreativeModeTab GROUP = new CreativeModeTab(MODID) {
        @Override
        public @NotNull ItemStack makeIcon() {
            return new ItemStack(WELL_DONE_MEAT_ITEM.get());
        }
    };
}
