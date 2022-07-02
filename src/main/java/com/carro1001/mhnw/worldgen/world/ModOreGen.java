package com.carro1001.mhnw.worldgen.world;

import com.carro1001.mhnw.setup.Registration;
import com.carro1001.mhnw.utils.MHNWReferences;
import net.minecraft.core.Holder;
import net.minecraft.data.worldgen.features.OreFeatures;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import static com.carro1001.mhnw.setup.ModConfig.*;

public class ModOreGen {



    public static Holder<PlacedFeature> OVERWORLD_MACHALITE_ORE;
    public static Holder<PlacedFeature> OVERWORLD_CARBALITE_ORE;
    public static Holder<PlacedFeature> OVERWORLD_DRAGONITE_ORE;
    public static Holder<PlacedFeature> OVERWORLD_EARTH_CRYSTAL;
    public static Holder<PlacedFeature> OVERWORLD_ICE_CRYSTAL;

    public static final RuleTest AMETHYST_REPLACEABLES = new TagMatchTest(BlockTags.CRYSTAL_SOUND_BLOCKS);

    public ModOreGen(){
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, ModOreGen::onBiomeLoadingEvent);
    }

    public static void registerConfiguredFeatures() {
        OreConfiguration overworldCarbaliteConfig = new OreConfiguration(OreFeatures.DEEPSLATE_ORE_REPLACEABLES, Registration.CARBALITE_ORE_BLOCK.get().defaultBlockState(),
                CARBALITE_VEINSIZE.get());
        OVERWORLD_CARBALITE_ORE = registerPlacedFeature(MHNWReferences.CARBALITE_ORE, new ConfiguredFeature<>(Feature.ORE, overworldCarbaliteConfig),
                CountPlacement.of(CARBALITE_AMOUNT.get()),
                InSquarePlacement.spread(),
                BiomeFilter.biome(),
                HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(CARBALITE_MAXY.get())));

        OreConfiguration overworldMachaliteConfig = new OreConfiguration(OreFeatures.DEEPSLATE_ORE_REPLACEABLES, Registration.MACHALITE_ORE_BLOCK.get().defaultBlockState(),
                MACHALITE_VEINSIZE.get());
        OVERWORLD_MACHALITE_ORE = registerPlacedFeature(MHNWReferences.MACHALITE_ORE, new ConfiguredFeature<>(Feature.ORE, overworldMachaliteConfig),
                CountPlacement.of(MACHALITE_AMOUNT.get()),
                InSquarePlacement.spread(),
                BiomeFilter.biome(),
                HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(MACHALITE_MAXY.get())));

        OreConfiguration overworldDragoniteConfig = new OreConfiguration(OreFeatures.DEEPSLATE_ORE_REPLACEABLES, Registration.DRAGONITE_ORE_BLOCK.get().defaultBlockState(),
                DRAGONITE_VEINSIZE.get());
        OVERWORLD_DRAGONITE_ORE = registerPlacedFeature(MHNWReferences.DRAGONITE_ORE, new ConfiguredFeature<>(Feature.ORE, overworldDragoniteConfig),
                CountPlacement.of(DRAGONITE_AMOUNT.get()),
                InSquarePlacement.spread(),
                BiomeFilter.biome(),
                HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(DRAGONITE_MAXY.get())));

        OreConfiguration overworldIceCrystalConfig = new OreConfiguration(AMETHYST_REPLACEABLES, Registration.ICE_CRYSTAL_CLUSTER_BLOCK.get().defaultBlockState(),
                ICE_CRYSTAL_VEINSIZE.get());
        OVERWORLD_ICE_CRYSTAL = registerPlacedFeature(MHNWReferences.ICE_CRYSTAL_CLUSTER, new ConfiguredFeature<>(Feature.ORE, overworldIceCrystalConfig),
                CountPlacement.of(ICE_CRYSTAL_AMOUNT.get()),
                InSquarePlacement.spread(),
                BiomeFilter.biome(),
                HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.top()));

        OreConfiguration overworldEarthCrystalConfig = new OreConfiguration(AMETHYST_REPLACEABLES, Registration.EARTH_CRYSTAL_CLUSTER_BLOCK.get().defaultBlockState(),
                EARTH_CRYSTAL_VEINSIZE.get());
        OVERWORLD_EARTH_CRYSTAL = registerPlacedFeature(MHNWReferences.EARTH_CRYSTAL_CLUSTER, new ConfiguredFeature<>(Feature.ORE, overworldEarthCrystalConfig),
                CountPlacement.of(EARTH_CRYSTAL_AMOUNT.get()),
                InSquarePlacement.spread(),
                BiomeFilter.biome(),
                HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.top()));

    }

    private static <C extends FeatureConfiguration, F extends Feature<C>> Holder<PlacedFeature> registerPlacedFeature(String registryName, ConfiguredFeature<C, F> feature, PlacementModifier... placementModifiers) {
        return PlacementUtils.register(registryName, Holder.direct(feature), placementModifiers);
    }

    @SubscribeEvent
    public static void onBiomeLoadingEvent(BiomeLoadingEvent event) {
        if (event.getCategory() != Biome.BiomeCategory.NETHER && event.getCategory() != Biome.BiomeCategory.THEEND) {
            event.getGeneration().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OVERWORLD_CARBALITE_ORE);
            event.getGeneration().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OVERWORLD_MACHALITE_ORE);
            event.getGeneration().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OVERWORLD_DRAGONITE_ORE);
            event.getGeneration().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OVERWORLD_ICE_CRYSTAL);
            event.getGeneration().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OVERWORLD_EARTH_CRYSTAL);
        }
    }

}
