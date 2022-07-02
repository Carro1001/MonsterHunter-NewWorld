package com.carro1001.mhnw;

import com.carro1001.mhnw.setup.ClientSetup;
import com.carro1001.mhnw.setup.ModConfig;
import com.carro1001.mhnw.setup.ModSetup;
import com.carro1001.mhnw.setup.Registration;
import com.carro1001.mhnw.worldgen.world.ModOreGen;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

@Mod(MODID)
public class MHNW {

    public MHNW(){
        ModConfig.register();
        Registration.init();
        IEventBus event = FMLJavaModLoadingContext.get().getModEventBus();
        event.addListener(ModSetup::init);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> event.addListener(ClientSetup::init));
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;
        forgeBus.register(new ModOreGen());
        ModConfig.loadConfig(ModConfig.CLIENT_CONFIG, FMLPaths.CONFIGDIR.get().resolve(MODID+"-client.toml"));
        ModConfig.loadConfig(ModConfig.SERVER_CONFIG, FMLPaths.CONFIGDIR.get().resolve(MODID+"-common.toml"));
    }

    public static final CreativeModeTab GROUP = new CreativeModeTab(MODID) {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(Registration.WELL_DONE_MEAT_ITEM.get());
        }
    };
}
