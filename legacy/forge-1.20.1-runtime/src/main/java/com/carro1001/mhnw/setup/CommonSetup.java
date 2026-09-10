package com.carro1001.mhnw.setup;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommonSetup {

    @SubscribeEvent
    public static void init(FMLCommonSetupEvent event) {

    }
}
