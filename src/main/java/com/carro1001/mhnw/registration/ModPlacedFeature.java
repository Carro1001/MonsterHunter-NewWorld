package com.carro1001.mhnw.registration;

import com.carro1001.mhnw.utils.MHNWReferences;
import net.minecraft.core.Registry;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.placement.*;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

import static com.carro1001.mhnw.setup.ModConfig.*;

public class ModPlacedFeature {
    /*public static final DeferredRegister<PlacedFeature> PLACED_FEATURES = DeferredRegister.create(Registry.PLACED_FEATURE_REGISTRY, MHNWReferences.MODID);


    public static final RegistryObject<PlacedFeature> MACHALITE_ORE = PLACED_FEATURES.register(MHNWReferences.MACHALITE_ORE,
            () -> new PlacedFeature(ModConfiguredFeature.MACHALITE_ORE.getHolder().get(),
                    commonOrePlacement(MACHALITE_AMOUNT.get(), HeightRangePlacement.triangle(
                            VerticalAnchor.bottom(),
                            VerticalAnchor.absolute(MACHALITE_MAXY.get())
                    ))));
    public static final RegistryObject<PlacedFeature> DRAGONITE_ORE = PLACED_FEATURES.register(MHNWReferences.DRAGONITE_ORE,
            () -> new PlacedFeature(ModConfiguredFeature.DRAGONITE_ORE.getHolder().get(),
                    commonOrePlacement(DRAGONITE_AMOUNT.get(), HeightRangePlacement.triangle(
                            VerticalAnchor.bottom(),
                            VerticalAnchor.absolute(DRAGONITE_MAXY.get())
                    ))));
    public static final RegistryObject<PlacedFeature> CARBALITE_ORE = PLACED_FEATURES.register(MHNWReferences.CARBALITE_ORE,
            () -> new PlacedFeature(ModConfiguredFeature.CARBALITE_ORE.getHolder().get(),
                    commonOrePlacement(CARBALITE_AMOUNT.get(), HeightRangePlacement.triangle(
                            VerticalAnchor.bottom(),
                            VerticalAnchor.absolute(CARBALITE_MAXY.get())
                    ))));

    public static final RegistryObject<PlacedFeature> CRYSTAL_CLUSTER_GEODE = PLACED_FEATURES.register(MHNWReferences.CRYSTAL_CLUSTER_GEODES,
            () -> new PlacedFeature(ModConfiguredFeature.CRYSTAL_CLUSTER_GEODE.getHolder().get(),
                    orePlacement(RarityFilter.onAverageOnceEvery(24),
                            HeightRangePlacement.uniform(VerticalAnchor.aboveBottom(6), VerticalAnchor.absolute(30)))));

    private static List<PlacementModifier> commonOrePlacement(int countPerChunk, PlacementModifier height) {
        return orePlacement(CountPlacement.of(countPerChunk), height);
    }

    private static List<PlacementModifier> orePlacement(PlacementModifier count, PlacementModifier height) {
        return List.of(count, InSquarePlacement.spread(), height, BiomeFilter.biome());
    }*/
}
