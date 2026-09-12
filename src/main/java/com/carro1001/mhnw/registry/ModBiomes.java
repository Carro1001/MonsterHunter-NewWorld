package com.carro1001.mhnw.registry;

import com.carro1001.mhnw.MHNW;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/**
 * R1a's two worldgen identifiers. Deliberately not a {@code DeferredRegister<Biome>}: biomes live in
 * the dynamic registry and are loaded from datapack JSON
 * ({@code data/mhnw/worldgen/biome/verdant_hunting_grounds.json}), so all the code needs is the key
 * to point at that file and the tag key that decides what counts as habitat.
 */
public final class ModBiomes {

    /** The one R1a habitat. Its data lives in datapack JSON, not code. */
    public static final ResourceKey<Biome> VERDANT_HUNTING_GROUNDS = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(MHNW.MOD_ID, "verdant_hunting_grounds"));

    /**
     * The single selector for "MHNW wildlife belongs here", used by both the spawn-entry biome
     * modifiers and the automatic-spawn guard so the two can never disagree. Contains only our
     * biome by default; a pack author can extend it, which is the point of it being a tag.
     */
    public static final TagKey<Biome> SPAWNS_HUNTING_WILDLIFE = TagKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(MHNW.MOD_ID, "spawns_hunting_wildlife"));

    private ModBiomes() {}
}
