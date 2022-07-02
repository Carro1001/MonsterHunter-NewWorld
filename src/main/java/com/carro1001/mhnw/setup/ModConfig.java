package com.carro1001.mhnw.setup;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.nio.file.Path;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

@Mod.EventBusSubscriber
public class ModConfig {

    public static final String CATEGORY_GENERAL = "general";
    public static final String WORLD_GEN = "world_gen";
    public static final String MOB = "mob";


    public static ForgeConfigSpec.IntValue MACHALITE_VEINSIZE;
    public static ForgeConfigSpec.IntValue MACHALITE_AMOUNT;
    public static ForgeConfigSpec.IntValue CARBALITE_VEINSIZE;
    public static ForgeConfigSpec.IntValue CARBALITE_AMOUNT;
    public static ForgeConfigSpec.IntValue DRAGONITE_VEINSIZE;
    public static ForgeConfigSpec.IntValue DRAGONITE_AMOUNT;
    public static ForgeConfigSpec.IntValue EARTH_CRYSTAL_VEINSIZE;
    public static ForgeConfigSpec.IntValue EARTH_CRYSTAL_AMOUNT;

    public static ForgeConfigSpec.IntValue ICE_CRYSTAL_VEINSIZE;
    public static ForgeConfigSpec.IntValue ICE_CRYSTAL_AMOUNT;
    public static ForgeConfigSpec.IntValue MACHALITE_MAXY;
    public static ForgeConfigSpec.IntValue CARBALITE_MAXY;
    public static ForgeConfigSpec.IntValue DRAGONITE_MAXY;


    public static ForgeConfigSpec.IntValue APTONOTH_WEIGHT;

    public static ForgeConfigSpec.IntValue TOADS_WEIGHT;
    public static ForgeConfigSpec.DoubleValue POISON_CHANCE;
    public static ForgeConfigSpec.DoubleValue BLAST_CHANCE;
    public static ForgeConfigSpec.DoubleValue PARA_CHANCE;
    public static ForgeConfigSpec.DoubleValue SLEEP_CHANCE;
    public static ForgeConfigSpec.IntValue BITTERBUG_WEIGHT;
    public static ForgeConfigSpec.DoubleValue GODBUG_CHANCE;
    public static ForgeConfigSpec.IntValue BLANGO_WEIGHT;
    public static ForgeConfigSpec.IntValue BLANGONGA_WEIGHT;

    public static ForgeConfigSpec.IntValue IZUCHI_WEIGHT;
    public static ForgeConfigSpec.IntValue GIZUCHI_WEIGHT;

    public static ForgeConfigSpec.IntValue RATHALOS_WEIGHT;
    public static ForgeConfigSpec.IntValue RATHIAN_WEIGHT;
    public static ForgeConfigSpec.IntValue ZINOGRE_WEIGHT;

    public static final ForgeConfigSpec.Builder SERVER_BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();

    public static ForgeConfigSpec SERVER_CONFIG;
    public static ForgeConfigSpec CLIENT_CONFIG;

    public static void register() {
        SERVER_BUILDER.comment("General Settings").push(CATEGORY_GENERAL);

        setupWorldGenConfig();
        setupMobInfo();

        SERVER_BUILDER.pop();

        SERVER_CONFIG = SERVER_BUILDER.build();
        CLIENT_CONFIG = CLIENT_BUILDER.build();

        ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.CLIENT, CLIENT_CONFIG);
        ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER, SERVER_CONFIG);
    }

    public static void loadConfig(ForgeConfigSpec spec, Path path) {

        final CommentedFileConfig configData = CommentedFileConfig.builder(path)
                .sync()
                .autosave()
                .writingMode(WritingMode.REPLACE)
                .build();

        configData.load();
        spec.setConfig(configData);
    }

    private static void setupMobInfo() {
        SERVER_BUILDER.comment("Mob Settings").push(MOB);
        APTONOTH_WEIGHT = SERVER_BUILDER.comment("Weight for Aptonoth to spawn.")
                .defineInRange("aptonoth_weight", 35, 0, 100);

        BITTERBUG_WEIGHT = SERVER_BUILDER.comment("Weight for Bitterbug to spawn.")
                .defineInRange("bitterbug_weight", 50, 0, 100);
        GODBUG_CHANCE = SERVER_BUILDER.comment("Chance for Bitterbug to spawn as Godbug.")
                .defineInRange("godbug_chance", 0.50f, 0, 100);

        TOADS_WEIGHT = SERVER_BUILDER.comment("Weight for Toads to spawn.")
                .defineInRange("toad_weight", 50, 0, 100);
        POISON_CHANCE = SERVER_BUILDER.comment("Chance for Toad to spawn as Poisontoad.")
                .defineInRange("poison_chance", 0.25f, 0.1, 1);
        PARA_CHANCE = SERVER_BUILDER.comment("Chance for Toad to spawn as Paratoad.")
                .defineInRange("para_chance", 0.25f, 0.1, 1);
        SLEEP_CHANCE = SERVER_BUILDER.comment("Chance for Toad to spawn as Sleeptoad.")
                .defineInRange("sleep_chance", 0.25f, 0.1, 1);
        BLAST_CHANCE = SERVER_BUILDER.comment("Chance for Toad to spawn as Blastoad.")
                .defineInRange("blast_chance", 0.25f, 0.1, 1);

        BLANGO_WEIGHT = SERVER_BUILDER.comment("Weight for Blango to spawn.")
                .defineInRange("blango_weight", 25, 0, 100);
        BLANGONGA_WEIGHT = SERVER_BUILDER.comment("Weight for Blangonga to spawn.")
                .defineInRange("blangonga_weight", 5, 0, 100);

        IZUCHI_WEIGHT = SERVER_BUILDER.comment("Weight for Izuchi to spawn.")
                .defineInRange("izuchi_weight", 25, 0, 100);
        GIZUCHI_WEIGHT = SERVER_BUILDER.comment("Weight for Great Izuchi to spawn.")
                .defineInRange("gizuchi_weight", 5, 0, 100);

        RATHALOS_WEIGHT = SERVER_BUILDER.comment("Weight for Rathalos to spawn.")
                .defineInRange("rathalos_weight", 5, 0, 100);
        RATHIAN_WEIGHT = SERVER_BUILDER.comment("Weight for Rathian to spawn.")
                .defineInRange("rathian_weight", 5, 0, 100);
        ZINOGRE_WEIGHT = SERVER_BUILDER.comment("Weight for Zinogre to spawn.")
                .defineInRange("zinogre_weight", 5, 0, 100);

        SERVER_BUILDER.pop();
    }

    private static void setupWorldGenConfig() {
        SERVER_BUILDER.comment("World Gen settings").push(WORLD_GEN);

        MACHALITE_VEINSIZE = SERVER_BUILDER.comment("Machalite Ore Vein Size")
                .defineInRange("mcore_size", 5, 0, 1000);
        MACHALITE_AMOUNT = SERVER_BUILDER.comment("Machalite Ore Spawn Amount")
                .defineInRange("mcore_amount", 12, 0, 1000);
        CARBALITE_VEINSIZE = SERVER_BUILDER.comment("Carbalite Ore Vein Size")
                .defineInRange("cbore_size", 3, 0, 1000);
        CARBALITE_AMOUNT = SERVER_BUILDER.comment("Carbalite Ore Spawn Amount")
                .defineInRange("cbore_amount", 7, 0, 1000);
        DRAGONITE_VEINSIZE = SERVER_BUILDER.comment("Dragonite Ore Vein Size")
                .defineInRange("dgore_size", 3, 0, 1000);
        DRAGONITE_AMOUNT = SERVER_BUILDER.comment("Dragonite Ore Spawn Amount")
                .defineInRange("dgore_amount", 5, 0, 1000);
        EARTH_CRYSTAL_VEINSIZE = SERVER_BUILDER.comment("Earth Crystal Vein Size")
                .defineInRange("ecore_size", 3, 0, 1000);
        EARTH_CRYSTAL_AMOUNT = SERVER_BUILDER.comment("Earth Crystal Spawn Amount")
                .defineInRange("ecore_amount", 20, 0, 1000);
        ICE_CRYSTAL_VEINSIZE = SERVER_BUILDER.comment("Ice Crystal Vein Size")
                .defineInRange("icore_size", 3, 0, 1000);
        ICE_CRYSTAL_AMOUNT = SERVER_BUILDER.comment("Ice Crystal Spawn Amount")
                .defineInRange("icore_amount", 20, 0, 1000);

        MACHALITE_MAXY = SERVER_BUILDER.comment("Machalite Ore Highest Y level to spawn")
                .defineInRange("mcore_maxy", 64, 0, 255);
        CARBALITE_MAXY = SERVER_BUILDER.comment("Carbalite Ore Highest Y level to spawn")
                .defineInRange("cbore_maxy", 64, 0, 255);
        DRAGONITE_MAXY = SERVER_BUILDER.comment("Dragonite Ore Highest Y level to spawn")
                .defineInRange("dgore_maxy", 64, 0, 255);


        SERVER_BUILDER.pop();
    }

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent.Loading event)
    {
        if (event.getConfig().getModId().equals(MODID))
        {
            CommentedConfig cfg = event.getConfig().getConfigData();

            if (cfg instanceof CommentedFileConfig)
                ((CommentedFileConfig) cfg).load();
        }

    }

    @SubscribeEvent
    public static void onReload(final ModConfigEvent.Reloading configEvent) {
        if (configEvent.getConfig().getModId().equals(MODID))
        {
            //reload my stuff
            CommentedConfig cfg = configEvent.getConfig().getConfigData();

            if (cfg instanceof CommentedFileConfig)
                ((CommentedFileConfig) cfg).load();
        }
    }

}
