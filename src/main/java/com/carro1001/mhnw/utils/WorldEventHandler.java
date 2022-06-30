package com.carro1001.mhnw.utils;

import com.carro1001.mhnw.entities.bitterbug.BitterbugEntity;
import com.carro1001.mhnw.entities.toad.ToadEntity;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

@Mod.EventBusSubscriber(modid = MODID)
public class WorldEventHandler {

    @SubscribeEvent
    public static void onEntitySpawn(LivingSpawnEvent event) {
        if (event.getEntity() instanceof BitterbugEntity bitterbugEntity && !bitterbugEntity.getTypeAssignedDir()){
            bitterbugEntity.setType();
        }
        if (event.getEntity() instanceof ToadEntity toadEntity && !toadEntity.getTypeAssignedDir()){
            toadEntity.setType();
        }
    }
}
