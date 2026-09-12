package com.carro1001.mhnw.registry;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.entity.Aptonoth;
import com.carro1001.mhnw.entity.Bug;
import com.carro1001.mhnw.entity.FlashBombProjectile;
import com.carro1001.mhnw.entity.Flashbug;
import com.carro1001.mhnw.entity.GreatIzuchi;
import com.carro1001.mhnw.entity.Izuchi;
import com.carro1001.mhnw.entity.Lagiacrus;
import com.carro1001.mhnw.entity.Rathian;
import com.carro1001.mhnw.entity.Rathalos;
import com.carro1001.mhnw.entity.Toad;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MHNW.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, MHNW.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<GreatIzuchi>> GREAT_IZUCHI =
            ENTITY_TYPES.register("great_izuchi", () -> EntityType.Builder
                    .of(GreatIzuchi::new, MobCategory.MONSTER)
                    // Conservative body-sized root envelope with a measured ground footprint: the
                    // feet solve to +/- 0.47 laterally and the head to 3.0 blocks. The torso, head
                    // and 4.9-block tail sit outside this box and are covered by hurtbox parts.
                    .sized(GreatIzuchi.BODY_WIDTH, GreatIzuchi.BODY_HEIGHT)
                    .eyeHeight(3.0F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .build("great_izuchi"));

    public static final DeferredHolder<Item, Item> GREAT_IZUCHI_SPAWN_EGG =
            ITEMS.register("great_izuchi_spawn_egg", () -> new DeferredSpawnEggItem(
                    GREAT_IZUCHI, 0x6B5B45, 0xB03A2E, new Item.Properties()));

    /**
     * The P3 proof of reuse: a passive herbivore, not another combat monster (see {@link Aptonoth}).
     * A single, whole-entity hurtbox; no multipart machinery.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<Aptonoth>> APTONOTH =
            ENTITY_TYPES.register("aptonoth", () -> EntityType.Builder
                    .of(Aptonoth::new, MobCategory.CREATURE)
                    .sized(Aptonoth.BODY_WIDTH, Aptonoth.BODY_HEIGHT)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .build("aptonoth"));

    public static final DeferredHolder<Item, Item> APTONOTH_SPAWN_EGG =
            ITEMS.register("aptonoth_spawn_egg", () -> new DeferredSpawnEggItem(
                    APTONOTH, 0xC9A876, 0x6E5842, new Item.Properties()));

    /**
     * Endemic life, not a monster: one entity type, four preserved variant textures chosen
     * randomly at spawn (see {@link Toad}). No multipart, no attack timeline.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<Toad>> TOAD =
            ENTITY_TYPES.register("toad", () -> EntityType.Builder
                    .of(Toad::new, MobCategory.CREATURE)
                    .sized(Toad.BODY_WIDTH, Toad.BODY_HEIGHT)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("toad"));

    public static final DeferredHolder<Item, Item> TOAD_SPAWN_EGG =
            ITEMS.register("toad_spawn_egg", () -> new DeferredSpawnEggItem(
                    TOAD, 0x4C7A3D, 0xD8C77A, new Item.Properties()));

    /**
     * Endemic life, flying: the second caller of {@code EndemicAreaEffectGoal}. See
     * {@link Flashbug}.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<Flashbug>> FLASHBUG =
            ENTITY_TYPES.register("flashbug", () -> EntityType.Builder
                    .of(Flashbug::new, MobCategory.CREATURE)
                    .sized(Flashbug.BODY_WIDTH, Flashbug.BODY_HEIGHT)
                    .clientTrackingRange(6)
                    .updateInterval(2)
                    .build("flashbug"));

    public static final DeferredHolder<Item, Item> FLASHBUG_SPAWN_EGG =
            ITEMS.register("flashbug_spawn_egg", () -> new DeferredSpawnEggItem(
                    FLASHBUG, 0x2E2A1F, 0xE8E13A, new Item.Properties()));

    /**
     * The last P3 species, and the odd one out: a hand-modeled Java mesh, not a GeckoLib asset.
     * See {@link Bug}.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<Bug>> BUG =
            ENTITY_TYPES.register("bug", () -> EntityType.Builder
                    .of(Bug::new, MobCategory.AMBIENT)
                    .sized(Bug.BODY_WIDTH, Bug.BODY_HEIGHT)
                    .eyeHeight(Bug.EYE_HEIGHT)
                    .clientTrackingRange(6)
                    .updateInterval(3)
                    .build("bug"));

    public static final DeferredHolder<Item, Item> BUG_SPAWN_EGG =
            ITEMS.register("bug_spawn_egg", () -> new DeferredSpawnEggItem(
                    BUG, 0x8A6B3D, 0xC9A54A, new Item.Properties()));

    /**
     * The P4 small monster: genuinely hostile, single fitted hurtbox, no multipart. See
     * {@link Izuchi}.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<Izuchi>> IZUCHI =
            ENTITY_TYPES.register("izuchi", () -> EntityType.Builder
                    .of(Izuchi::new, MobCategory.MONSTER)
                    .sized(Izuchi.BODY_WIDTH, Izuchi.BODY_HEIGHT)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build("izuchi"));

    public static final DeferredHolder<Item, Item> IZUCHI_SPAWN_EGG =
            ITEMS.register("izuchi_spawn_egg", () -> new DeferredSpawnEggItem(
                    IZUCHI, 0x8B7355, 0x4A3B2A, new Item.Properties()));

    /** P4's first large wyvern, ground-only for now. See {@link Rathian}. */
    public static final DeferredHolder<EntityType<?>, EntityType<Rathian>> RATHIAN =
            ENTITY_TYPES.register("rathian", () -> EntityType.Builder
                    .of(Rathian::new, MobCategory.MONSTER)
                    .sized(Rathian.BODY_WIDTH, Rathian.BODY_HEIGHT)
                    .eyeHeight(3.2F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .build("rathian"));

    public static final DeferredHolder<Item, Item> RATHIAN_SPAWN_EGG =
            ITEMS.register("rathian_spawn_egg", () -> new DeferredSpawnEggItem(
                    RATHIAN, 0x4A7A2E, 0xD4C13A, new Item.Properties()));

    /** P4's second large wyvern, ground-only, with a worse-blocked attack set than Rathian's. See
     * {@link Rathalos}. */
    public static final DeferredHolder<EntityType<?>, EntityType<Rathalos>> RATHALOS =
            ENTITY_TYPES.register("rathalos", () -> EntityType.Builder
                    .of(Rathalos::new, MobCategory.MONSTER)
                    .sized(Rathalos.BODY_WIDTH, Rathalos.BODY_HEIGHT)
                    .eyeHeight(3.2F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .build("rathalos"));

    public static final DeferredHolder<Item, Item> RATHALOS_SPAWN_EGG =
            ITEMS.register("rathalos_spawn_egg", () -> new DeferredSpawnEggItem(
                    RATHALOS, 0xB03A2E, 0x2E2A6B, new Item.Properties()));

    /** P5a amphibious movement baseline; no natural spawning or outgoing attacks. */
    public static final DeferredHolder<EntityType<?>, EntityType<Lagiacrus>> LAGIACRUS =
            ENTITY_TYPES.register("lagiacrus", () -> EntityType.Builder
                    .of(Lagiacrus::new, MobCategory.MONSTER)
                    .sized(Lagiacrus.BODY_WIDTH, Lagiacrus.BODY_HEIGHT)
                    .eyeHeight(1.2F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .build("lagiacrus"));

    public static final DeferredHolder<Item, Item> LAGIACRUS_SPAWN_EGG =
            ITEMS.register("lagiacrus_spawn_egg", () -> new DeferredSpawnEggItem(
                    LAGIACRUS, 0x4A91A6, 0xC9B077, new Item.Properties()));

    /**
     * R2's thrown flash bomb. {@code MISC} because it is a projectile, not life: it has no
     * attributes, no spawn placement and no spawn egg, and it is rendered from its own item stack
     * (see {@code MHNWClient.FlashBombRenderer}).
     */
    public static final DeferredHolder<EntityType<?>, EntityType<FlashBombProjectile>> FLASH_BOMB =
            ENTITY_TYPES.register("flash_bomb", () -> EntityType.Builder
                    .<FlashBombProjectile>of(FlashBombProjectile::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("flash_bomb"));

    /** Where natural spawn placement is anchored. Referenced by the spawn placement registration. */
    public static final Heightmap.Types SPAWN_HEIGHTMAP = Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        ITEMS.register(modBus);
    }

    private ModEntities() {}
}
