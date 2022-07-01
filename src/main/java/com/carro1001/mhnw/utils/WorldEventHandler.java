package com.carro1001.mhnw.utils;

import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

@Mod.EventBusSubscriber(modid = MODID)
public class WorldEventHandler {

    @SubscribeEvent
    public static void onEntitySpawn(LivingSpawnEvent event) {


    }
}
