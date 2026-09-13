package com.carro1001.mhnw;

import com.carro1001.mhnw.entity.Aptonoth;
import com.carro1001.mhnw.entity.Bug;
import com.carro1001.mhnw.entity.Flashbug;
import com.carro1001.mhnw.entity.GreatIzuchi;
import com.carro1001.mhnw.entity.HuntingSpawnRules;
import com.carro1001.mhnw.entity.Izuchi;
import com.carro1001.mhnw.entity.Lagiacrus;
import com.carro1001.mhnw.entity.Rathian;
import com.carro1001.mhnw.entity.Rathalos;
import com.carro1001.mhnw.entity.Toad;
import com.carro1001.mhnw.item.BoneArmorItem;
import com.carro1001.mhnw.registry.ModCreativeTabs;
import com.carro1001.mhnw.registry.ModEntities;
import com.carro1001.mhnw.registry.ModItems;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import com.carro1001.mhnw.worldgen.HuntingGroundsRegion;
import terrablender.api.Regions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(MHNW.MOD_ID)
public class MHNW {
    public static final String MOD_ID = "mhnw";
    public static final Logger LOG = LoggerFactory.getLogger("mhnw");

    public MHNW(IEventBus modBus, ModContainer container) {
        ModEntities.register(modBus);
        ModItems.register(modBus);
        ModCreativeTabs.register(modBus);
        modBus.addListener(MHNW::onAttributeCreation);
        modBus.addListener(MHNW::onRegisterSpawnPlacements);
        modBus.addListener(MHNW::onBuildCreativeTabs);
        modBus.addListener(MHNW::onRegisterGameTests);
        modBus.addListener(MHNW::onCommonSetup);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (RegisterCommandsEvent event) -> MHNWCommands.register(event.getDispatcher()));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(MHNW::onEquipmentChange);
        container.registerConfig(ModConfig.Type.SERVER, MHNWConfig.SERVER_SPEC);
        container.registerConfig(ModConfig.Type.COMMON, MHNWConfig.COMMON_SPEC);
    }

    @SubscribeEvent
    private static void onAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.GREAT_IZUCHI.get(), GreatIzuchi.createAttributes().build());
        event.put(ModEntities.APTONOTH.get(), Aptonoth.createAttributes().build());
        event.put(ModEntities.TOAD.get(), Toad.createAttributes().build());
        event.put(ModEntities.FLASHBUG.get(), Flashbug.createAttributes().build());
        event.put(ModEntities.BUG.get(), Bug.createAttributes().build());
        event.put(ModEntities.IZUCHI.get(), Izuchi.createAttributes().build());
        event.put(ModEntities.RATHIAN.get(), Rathian.createAttributes().build());
        event.put(ModEntities.RATHALOS.get(), Rathalos.createAttributes().build());
        event.put(ModEntities.LAGIACRUS.get(), Lagiacrus.createAttributes().build());
    }

    @SubscribeEvent
    private static void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        // R1a: the six species that can appear in the Verdant Hunting Grounds. Small Izuchi is here
        // for placement validation only -- it has no independent spawn entry and arrives solely as a
        // Great Izuchi escort. All six anchor to MOTION_BLOCKING_NO_LEAVES so a position under a tree
        // canopy resolves to the ground, not to the leaves.
        event.register(ModEntities.GREAT_IZUCHI.get(),
                SpawnPlacementTypes.ON_GROUND,
                ModEntities.SPAWN_HEIGHTMAP,
                GreatIzuchi::checkSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.IZUCHI.get(),
                SpawnPlacementTypes.ON_GROUND,
                ModEntities.SPAWN_HEIGHTMAP,
                HuntingSpawnRules::checkMonster,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.APTONOTH.get(),
                SpawnPlacementTypes.ON_GROUND,
                ModEntities.SPAWN_HEIGHTMAP,
                HuntingSpawnRules::checkAnimal,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.TOAD.get(),
                SpawnPlacementTypes.ON_GROUND,
                ModEntities.SPAWN_HEIGHTMAP,
                HuntingSpawnRules::checkSurfaceWildlife,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.FLASHBUG.get(),
                SpawnPlacementTypes.ON_GROUND,
                ModEntities.SPAWN_HEIGHTMAP,
                HuntingSpawnRules::checkSurfaceWildlife,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.BUG.get(),
                SpawnPlacementTypes.ON_GROUND,
                ModEntities.SPAWN_HEIGHTMAP,
                HuntingSpawnRules::checkSurfaceWildlife,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // Rathian, Rathalos and Lagiacrus stay registered and egg-only: R1a gives them no natural
        // spawn entry, so they need no placement rule yet (docs/DEFERRED.md).
    }

    /**
     * Where {@code mhnw:verdant_hunting_grounds} becomes a biome the Overworld can actually pick.
     * TerraBlender's region list is plain static state, so registration has to be enqueued onto the
     * main thread rather than run on the parallel mod-loading thread.
     */
    @SubscribeEvent
    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> Regions.register(new HuntingGroundsRegion()));
    }

    @SubscribeEvent
    private static void onRegisterGameTests(RegisterGameTestsEvent event) {
        event.register(MHNWGameTests.class);
    }

    /**
     * The only hook the bone-armor set bonus needs.
     *
     * <p>NeoForge fires this per changed slot on the server, which covers equipping, unequipping,
     * dying (every slot empties) and rejoining (every slot fills from nothing). Recomputing the
     * whole set from what is worn right now, rather than adding on equip and subtracting on
     * unequip, is what makes it impossible to leave a stale or doubled modifier behind. See
     * {@link BoneArmorItem#refreshSetBonus}.
     */
    private static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getSlot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            BoneArmorItem.refreshSetBonus(event.getEntity());
        }
    }

    /**
     * Everything MHNW adds, in one tab of its own ({@link ModCreativeTabs#MAIN}) rather than spread
     * across five vanilla ones. Roughly the old grouping, kept as section order within the one tab
     * rather than as separate {@code CreativeModeTabs} keys: materials, food/preparation, armor and
     * the weapon, then buckets, then spawn eggs last -- the same shelf order a player would actually
     * use the items in.
     */
    @SubscribeEvent
    private static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != ModCreativeTabs.MAIN.getKey()) {
            return;
        }
        event.accept(ModItems.MONSTER_HIDE.get());
        event.accept(ModItems.MONSTER_CLAW.get());
        event.accept(ModItems.BOTTLED_FLASHBUG.get());

        event.accept(ModItems.RAW_MEAT.get());
        event.accept(ModItems.COOKED_MEAT.get());
        event.accept(ModItems.BBQ_SPIT.get());

        event.accept(ModItems.BONE_HEAD.get());
        event.accept(ModItems.BONE_CHESTPLATE.get());
        event.accept(ModItems.BONE_LEGGING.get());
        event.accept(ModItems.BONE_BOOTS.get());
        event.accept(ModItems.GIANT_JAWBLADE.get());
        event.accept(ModItems.GIANT_JAWBLADE_GECKO.get());
        event.accept(ModItems.FLASH_BOMB.get());

        event.accept(ModItems.POISONTOAD_BUCKET.get());
        event.accept(ModItems.SLEEPTOAD_BUCKET.get());
        event.accept(ModItems.PARATOAD_BUCKET.get());
        event.accept(ModItems.NITROTOAD_BUCKET.get());

        event.accept(ModEntities.GREAT_IZUCHI_SPAWN_EGG.get());
        event.accept(ModEntities.APTONOTH_SPAWN_EGG.get());
        event.accept(ModEntities.TOAD_SPAWN_EGG.get());
        event.accept(ModEntities.FLASHBUG_SPAWN_EGG.get());
        event.accept(ModEntities.BUG_SPAWN_EGG.get());
        event.accept(ModEntities.IZUCHI_SPAWN_EGG.get());
        event.accept(ModEntities.RATHIAN_SPAWN_EGG.get());
        event.accept(ModEntities.RATHALOS_SPAWN_EGG.get());
        event.accept(ModEntities.LAGIACRUS_SPAWN_EGG.get());
    }
}
