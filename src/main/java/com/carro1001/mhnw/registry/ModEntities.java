package com.carro1001.mhnw.registry;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.entity.Aptonoth;
import com.carro1001.mhnw.entity.GreatIzuchi;
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

    /** Where natural spawn placement is anchored. Referenced by the spawn placement registration. */
    public static final Heightmap.Types SPAWN_HEIGHTMAP = Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        ITEMS.register(modBus);
    }

    private ModEntities() {}
}
