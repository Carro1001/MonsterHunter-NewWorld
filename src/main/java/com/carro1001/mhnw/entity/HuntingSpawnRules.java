package com.carro1001.mhnw.entity;

import com.carro1001.mhnw.MHNWConfig;
import com.carro1001.mhnw.registry.ModBiomes;
import com.carro1001.mhnw.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/**
 * R1a's one gate on automatic MHNW spawning: habitat, then the off switch, then physical fitness.
 *
 * <h2>Why one place</h2>
 * The habitat selector tag {@code #mhnw:spawns_hunting_wildlife} is the single answer to "does MHNW
 * wildlife belong here", and it is consulted from two directions that must never disagree: the biome
 * modifiers put spawn <em>entries</em> on biomes in that tag, and this guard rejects a spawn
 * <em>attempt</em> outside it. The tag is the reason a pack author can extend the habitat without
 * touching code, and the reason the default does not leak MHNW mobs into vanilla forests.
 *
 * <h2>Automatic means NATURAL and CHUNK_GENERATION</h2>
 * Before R1a only {@code NATURAL} consulted the config, which quietly let worldgen-time passive
 * spawning bypass the off switch entirely -- {@code CHUNK_GENERATION} is how vanilla seeds animals
 * into fresh chunks, and a {@code CREATURE}-category mob gets most of its population that way. Both
 * sources are automatic and both are gated here.
 *
 * <p>Everything else is deliberately <em>not</em> gated: spawn eggs, {@code /summon}, mob spawners,
 * dispensers, breeding and entities already saved in a world keep working regardless of biome or
 * config. {@code naturalSpawning=false} stops new automatic MH spawns; it is not a content switch,
 * it does not touch vanilla wildlife, it does not affect biome generation, and it cannot change a
 * saved world's biome keys.
 *
 * <p>The config is read at spawn time, never cached into a static or baked into data, so toggling it
 * on a running server takes effect on the next spawn attempt.
 */
public final class HuntingSpawnRules {

    /** Spawn sources the game initiates on its own, as opposed to something a player or block did. */
    private static boolean isAutomatic(MobSpawnType spawnType) {
        return spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION;
    }

    /** Habitat membership alone, with no config or physical component. Used by the escort placement. */
    public static boolean inHabitat(ServerLevelAccessor level, BlockPos pos) {
        return level.getBiome(pos).is(ModBiomes.SPAWNS_HUNTING_WILDLIFE);
    }

    /**
     * The gate itself: an automatic spawn needs the off switch on and the habitat tag; anything else
     * passes straight through.
     */
    public static boolean habitatAllows(ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos) {
        if (!isAutomatic(spawnType)) {
            return true;
        }
        return MHNWConfig.NATURAL_SPAWNING.get() && inHabitat(level, pos);
    }

    /**
     * Great Izuchi and small Izuchi: habitat plus vanilla's own monster rules, so darkness, peaceful
     * difficulty and the rest still apply exactly as they did before R1a.
     */
    public static boolean checkMonster(EntityType<? extends Monster> type, ServerLevelAccessor level,
                                       MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return habitatAllows(level, spawnType, pos)
                && Monster.checkMonsterSpawnRules(type, level, spawnType, pos, random);
    }

    /**
     * Aptonoth: a real {@link Animal}, so it gets vanilla's animal rules -- spawnable ground block and
     * light above 8 -- rather than a hand-rolled imitation of them.
     */
    public static boolean checkAnimal(EntityType<? extends Animal> type, ServerLevelAccessor level,
                                      MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return habitatAllows(level, spawnType, pos)
                && Animal.checkAnimalSpawnRules(type, level, spawnType, pos, random);
    }

    /**
     * Toad, Flashbug and Bug: {@link net.minecraft.world.entity.PathfinderMob}s, not {@link Animal}s,
     * so {@code checkAnimalSpawnRules} is not available to them without a cast that would throw. They
     * get the same shape of check written out: solid spawnable ground, a body-sized volume actually
     * free of blocks and fluid, and a position at the surface rather than in a cave.
     *
     * <p>Surface is decided against {@code MOTION_BLOCKING_NO_LEAVES}, the same heightmap the spawn
     * placement anchors to, which is why standing under a tree canopy still counts as the surface
     * while a cave twenty blocks down does not. Flashbugs fly, but they are not created floating in
     * unsupported air -- they take off from a real spawn position once their own flight control runs.
     */
    public static boolean checkSurfaceWildlife(EntityType<? extends Mob> type, ServerLevelAccessor level,
                                               MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (!habitatAllows(level, spawnType, pos)) {
            return false;
        }
        return isSurface(level, pos) && hasSolidGround(level, type, pos) && isFree(level, type, pos);
    }

    /** At or above the ground the spawn heightmap reports for this column, so not underground. */
    public static boolean isSurface(ServerLevelAccessor level, BlockPos pos) {
        return pos.getY() >= level.getHeight(ModEntities.SPAWN_HEIGHTMAP, pos.getX(), pos.getZ()) - 1;
    }

    /** The block underfoot is something a mob of this type may stand on. */
    public static boolean hasSolidGround(ServerLevelAccessor level, EntityType<?> type, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState state = level.getBlockState(below);
        return state.isValidSpawn(level, below, type);
    }

    /** The body-sized volume at this position is clear of blocks and of fluid. */
    public static boolean isFree(ServerLevelAccessor level, EntityType<?> type, BlockPos pos) {
        if (!level.getFluidState(pos).isEmpty()) {
            return false;
        }
        return level.noCollision(type.getSpawnAABB(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D));
    }

    private HuntingSpawnRules() {}
}
