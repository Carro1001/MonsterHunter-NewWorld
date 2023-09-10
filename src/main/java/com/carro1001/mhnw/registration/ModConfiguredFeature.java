package com.carro1001.mhnw.registration;

import com.carro1001.mhnw.utils.MHNWReferences;
import com.google.common.base.Suppliers;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.features.OreFeatures;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GeodeBlockSettings;
import net.minecraft.world.level.levelgen.GeodeCrackSettings;
import net.minecraft.world.level.levelgen.GeodeLayerSettings;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.GeodeConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.function.Supplier;

import static com.carro1001.mhnw.registration.ModBlocks.*;
import static com.carro1001.mhnw.setup.ModConfig.*;

public class ModConfiguredFeature {

    /*public static final DeferredRegister<ConfiguredFeature<?, ?>> CONFIGURED_FEATURES = DeferredRegister.create(Registry.CONFIGURED_FEATURE_REGISTRY, MHNWReferences.MODID);

    private static final Supplier<List<OreConfiguration.TargetBlockState>> CARBALITE_REPLACEMENT = Suppliers.memoize(() ->
            List.of(
                    OreConfiguration.target(OreFeatures.STONE_ORE_REPLACEABLES, CARBALITE_ORE_BLOCK.get().defaultBlockState()),
                    OreConfiguration.target(OreFeatures.DEEPSLATE_ORE_REPLACEABLES, CARBALITE_ORE_BLOCK.get().defaultBlockState())
            )
    );

    private static final Supplier<List<OreConfiguration.TargetBlockState>> DRAGONITE_REPLACEMENT = Suppliers.memoize(() ->
            List.of(
                    OreConfiguration.target(OreFeatures.STONE_ORE_REPLACEABLES, DRAGONITE_ORE_BLOCK.get().defaultBlockState()),
                    OreConfiguration.target(OreFeatures.DEEPSLATE_ORE_REPLACEABLES, DRAGONITE_ORE_BLOCK.get().defaultBlockState())
            )
    );

    private static final Supplier<List<OreConfiguration.TargetBlockState>> MACHALITE_REPLACEMENT = Suppliers.memoize(() ->
            List.of(
                    OreConfiguration.target(OreFeatures.STONE_ORE_REPLACEABLES, MACHALITE_ORE_BLOCK.get().defaultBlockState()),
                    OreConfiguration.target(OreFeatures.DEEPSLATE_ORE_REPLACEABLES, MACHALITE_ORE_BLOCK.get().defaultBlockState())
            )
    );

    public static final RegistryObject<ConfiguredFeature<?, ?>> CARBALITE_ORE = CONFIGURED_FEATURES.register(MHNWReferences.CARBALITE_ORE,
            () -> new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(CARBALITE_REPLACEMENT.get(), CARBALITE_VEINSIZE.get())));
    public static final RegistryObject<ConfiguredFeature<?, ?>> DRAGONITE_ORE = CONFIGURED_FEATURES.register(MHNWReferences.DRAGONITE_ORE,
            () -> new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(DRAGONITE_REPLACEMENT.get(), DRAGONITE_VEINSIZE.get())));
    public static final RegistryObject<ConfiguredFeature<?, ?>> MACHALITE_ORE = CONFIGURED_FEATURES.register(MHNWReferences.MACHALITE_ORE,
            () -> new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(MACHALITE_REPLACEMENT.get(), MACHALITE_VEINSIZE.get())));

    public static final RegistryObject<ConfiguredFeature<?, ?>> CRYSTAL_CLUSTER_GEODE = CONFIGURED_FEATURES.register(MHNWReferences.CRYSTAL_CLUSTER_GEODES,
            () -> new ConfiguredFeature<>(Feature.GEODE, new GeodeConfiguration(new GeodeBlockSettings(BlockStateProvider.simple(Blocks.AIR), BlockStateProvider.simple(Blocks.AMETHYST_BLOCK), BlockStateProvider.simple(Blocks.BUDDING_AMETHYST), BlockStateProvider.simple(Blocks.CALCITE), BlockStateProvider.simple(Blocks.SMOOTH_BASALT), List.of(EARTH_CRYSTAL_CLUSTER_BLOCK.get().defaultBlockState(), ICE_CRYSTAL_CLUSTER_BLOCK.get().defaultBlockState(), Blocks.LARGE_AMETHYST_BUD.defaultBlockState(), Blocks.MEDIUM_AMETHYST_BUD.defaultBlockState(), Blocks.SMALL_AMETHYST_BUD.defaultBlockState(), Blocks.AMETHYST_CLUSTER.defaultBlockState()), BlockTags.FEATURES_CANNOT_REPLACE, BlockTags.GEODE_INVALID_BLOCKS), new GeodeLayerSettings(1.7D, 2.2D, 3.2D, 4.2D), new GeodeCrackSettings(0.95D, 2.0D, 2), 0.35D, 0.083D, true, UniformInt.of(4, 6), UniformInt.of(3, 4), UniformInt.of(1, 2), -16, 16, 0.05D, 1)));
*/
}
