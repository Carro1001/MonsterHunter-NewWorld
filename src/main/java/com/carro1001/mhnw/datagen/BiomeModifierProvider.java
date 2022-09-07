package com.carro1001.mhnw.datagen;

import com.carro1001.mhnw.utils.MHNWReferences;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ForgeBiomeModifiers;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class BiomeModifierProvider implements DataProvider {
    private final DataGenerator gen;
    private static final Logger LOGGER = LogManager.getLogger();
    public BiomeModifierProvider(DataGenerator gen) {
        this.gen   = gen;
    }

    @Override
    public void run(@NotNull CachedOutput cache) throws IOException {
        final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, RegistryAccess.BUILTIN.get());
        Path path = this.gen.getOutputFolder();
        ResourceLocation registryLoc = ForgeRegistries.Keys.BIOME_MODIFIERS.location();
        Map<ResourceLocation, BiomeModifier> map = new HashMap<>();
        this.registerModifiers(ops, (loc, modifier) -> {
            if (map.put(loc, modifier) != null) {
                LOGGER.debug("Duplicate biome modifier in data generation: {}", loc.toString());
            }
        });
        for (Map.Entry<ResourceLocation, BiomeModifier> entry : map.entrySet()) {
            Path modPath = path.resolve(String.join("/", PackType.SERVER_DATA.getDirectory(), entry.getKey().getNamespace(), registryLoc.getNamespace(), registryLoc.getPath(), entry.getKey().getPath() + ".json"));
            this.saveModifier(cache, ops, entry.getKey(), entry.getValue(), modPath);
        }
    }

    private void saveModifier(CachedOutput cache, RegistryOps<JsonElement> ops, ResourceLocation loc, BiomeModifier modifier, Path path) {
        BiomeModifier.DIRECT_CODEC.encodeStart(ops, modifier)
                .resultOrPartial(msg -> LOGGER.error("Failed to encode {}: {}", path, msg))
                .ifPresent(json -> {
                    try {
                        DataProvider.saveStable(cache, json, path);
                    } catch (IOException e) {
                        LOGGER.error("Failed to save {}", path, e);
                    }
                });
    }
    protected void registerModifiers(RegistryOps<JsonElement> ops, BiConsumer<ResourceLocation, BiomeModifier> consumer) {
        consumer.accept(new ResourceLocation(MHNWReferences.MODID, "add_ore_carbalite"), new ForgeBiomeModifiers.AddFeaturesBiomeModifier(
                new HolderSet.Named<>(ops.registry(Registry.BIOME_REGISTRY).get(), BiomeTags.IS_OVERWORLD),
                HolderSet.direct(ops.registry(Registry.PLACED_FEATURE_REGISTRY).get().getOrCreateHolderOrThrow(ResourceKey.create(Registry.PLACED_FEATURE_REGISTRY, new ResourceLocation(MHNWReferences.MODID, "carbalite_ore")))),
                GenerationStep.Decoration.UNDERGROUND_ORES
        ));
        consumer.accept(new ResourceLocation(MHNWReferences.MODID, "add_ore_machalite"), new ForgeBiomeModifiers.AddFeaturesBiomeModifier(
                new HolderSet.Named<>(ops.registry(Registry.BIOME_REGISTRY).get(), BiomeTags.IS_OVERWORLD),
                HolderSet.direct(ops.registry(Registry.PLACED_FEATURE_REGISTRY).get().getOrCreateHolderOrThrow(ResourceKey.create(Registry.PLACED_FEATURE_REGISTRY, new ResourceLocation(MHNWReferences.MODID, "machalite_ore")))),
                GenerationStep.Decoration.UNDERGROUND_ORES
        ));
        consumer.accept(new ResourceLocation(MHNWReferences.MODID, "add_ore_dragonite"), new ForgeBiomeModifiers.AddFeaturesBiomeModifier(
                new HolderSet.Named<>(ops.registry(Registry.BIOME_REGISTRY).get(), BiomeTags.IS_OVERWORLD),
                HolderSet.direct(ops.registry(Registry.PLACED_FEATURE_REGISTRY).get().getOrCreateHolderOrThrow(ResourceKey.create(Registry.PLACED_FEATURE_REGISTRY, new ResourceLocation(MHNWReferences.MODID, "dragonite_ore")))),
                GenerationStep.Decoration.UNDERGROUND_ORES
        ));

        consumer.accept(new ResourceLocation(MHNWReferences.MODID, "add_cluster_geode"), new ForgeBiomeModifiers.AddFeaturesBiomeModifier(
                new HolderSet.Named<>(ops.registry(Registry.BIOME_REGISTRY).get(), BiomeTags.IS_OVERWORLD),
                HolderSet.direct(ops.registry(Registry.PLACED_FEATURE_REGISTRY).get().getOrCreateHolderOrThrow(ResourceKey.create(Registry.PLACED_FEATURE_REGISTRY, new ResourceLocation(MHNWReferences.MODID, "crystal_cluster_geode")))),
                GenerationStep.Decoration.UNDERGROUND_DECORATION
        ));
    }

    @Override
    public @NotNull String getName() {
        return "Monster Hunter: New World Biome Modifiers";
    }
}
