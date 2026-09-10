package com.carro1001.mhnw;

import com.carro1001.mhnw.entity.GreatIzuchi;
import com.carro1001.mhnw.registry.ModEntities;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(MHNW.MOD_ID)
public class MHNW {
    public static final String MOD_ID = "mhnw";
    public static final Logger LOG = LoggerFactory.getLogger("mhnw");

    public MHNW(IEventBus modBus, ModContainer container) {
        ModEntities.register(modBus);
        modBus.addListener(MHNW::onAttributeCreation);
        modBus.addListener(MHNW::onRegisterSpawnPlacements);
        modBus.addListener(MHNW::onBuildCreativeTabs);
        container.registerConfig(ModConfig.Type.SERVER, MHNWConfig.SPEC);
    }

    @SubscribeEvent
    private static void onAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.GREAT_IZUCHI.get(), GreatIzuchi.createAttributes().build());
    }

    @SubscribeEvent
    private static void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(ModEntities.GREAT_IZUCHI.get(),
                SpawnPlacementTypes.ON_GROUND,
                ModEntities.SPAWN_HEIGHTMAP,
                GreatIzuchi::checkSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @SubscribeEvent
    private static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModEntities.GREAT_IZUCHI_SPAWN_EGG.get());
        }
    }
}
