package com.carro1001.mhnw.worldgen;

import com.carro1001.mhnw.registry.ModBiomes;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;

import java.util.function.Consumer;

/**
 * The one thing that makes {@code mhnw:verdant_hunting_grounds} actually generate.
 *
 * <p>Registering a biome JSON and adding spawn entries to it does nothing on its own -- the vanilla
 * Overworld biome source never picks a key it was not told about. TerraBlender's region system is
 * the compatible way in: each registered region gets a weighted share of the world, and within its
 * own share it decides which biome answers a given climate point. Weight {@code 2} against
 * vanilla's own region means roughly a small minority of the Overworld routes through us.
 *
 * <p>What this deliberately is not: a global replacement. {@code addModifiedVanillaOverworldBiomes}
 * starts from vanilla's own parameter list and we swap exactly two entries inside <em>our</em>
 * region's copy of it. Plains and forest still generate normally everywhere else, including inside
 * chunks other regions own, and the world's noise settings / Overworld preset are untouched. All
 * other climate mappings pass through unchanged, so terrain, oceans, caves and biome transitions are
 * ordinary vanilla.
 */
public class HuntingGroundsRegion extends Region {

    public static final ResourceLocation NAME =
            ResourceLocation.fromNamespaceAndPath("mhnw", "overworld_hunting_grounds");

    /** Region weight, not a mob spawn weight: how much of the Overworld this region is asked about. */
    public static final int WEIGHT = 2;

    public HuntingGroundsRegion() {
        super(NAME, RegionType.OVERWORLD, WEIGHT);
    }

    @Override
    public void addBiomes(Registry<Biome> registry,
                          Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        addModifiedVanillaOverworldBiomes(mapper, builder -> {
            // Temperate and lightly wooded: the two vanilla biomes whose climate slots the hunting
            // grounds should read as. Everything else the builder produces is left alone.
            builder.replaceBiome(Biomes.PLAINS, ModBiomes.VERDANT_HUNTING_GROUNDS);
            builder.replaceBiome(Biomes.FOREST, ModBiomes.VERDANT_HUNTING_GROUNDS);
        });
    }
}
