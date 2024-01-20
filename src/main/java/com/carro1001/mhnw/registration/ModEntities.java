package com.carro1001.mhnw.registration;

import com.carro1001.mhnw.entities.*;
import com.carro1001.mhnw.items.ToadBucket;
import com.carro1001.mhnw.utils.MHNWReferences;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.carro1001.mhnw.registration.ModItems.ITEMS;
import static com.carro1001.mhnw.registration.ModItems.basicItem;
import static com.carro1001.mhnw.utils.MHNWReferences.GREAT;
import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);

    public static final RegistryObject<EntityType<AptonothEntity>> APTONOTH = ENTITIES.register(MHNWReferences.APTONOTH, () -> EntityType.Builder.of(AptonothEntity::new, MobCategory.CREATURE)
            .sized(1.4f, 3.2f)
            .clientTrackingRange(8)
            .setShouldReceiveVelocityUpdates(false)
            .build(MHNWReferences.APTONOTH));
    public static final RegistryObject<Item> APTONOTH_EGG_ITEM = ITEMS.register(MHNWReferences.APTONOTH, () -> new ForgeSpawnEggItem(APTONOTH, 0xB8B584, 0x664D29, new Item.Properties()));

    public static final RegistryObject<EntityType<RathianEntity>> RATHIAN = ENTITIES.register(MHNWReferences.RATHIAN, () -> EntityType.Builder.of(RathianEntity::new, MobCategory.CREATURE)
            .sized(2.5f, 2.5f)
            .clientTrackingRange(16)
            .setShouldReceiveVelocityUpdates(false)
            .build(MHNWReferences.RATHIAN));
    public static final RegistryObject<Item> RATHIAN_EGG_ITEM = ITEMS.register(MHNWReferences.RATHIAN, () -> new ForgeSpawnEggItem(RATHIAN, 0x263920, 0x000000, new Item.Properties()));

    public static final RegistryObject<EntityType<RathalosEntity>> RATHALOS = ENTITIES.register(MHNWReferences.RATHALOS, () -> EntityType.Builder.of(RathalosEntity::new, MobCategory.CREATURE)
            .sized(2.5f, 2.5f)
            .clientTrackingRange(128)
            .setShouldReceiveVelocityUpdates(true)
            .build(MHNWReferences.RATHALOS));

    public static final RegistryObject<EntityType<NewRathalosEntity>> NEW_RATHALOS = ENTITIES.register(MHNWReferences.NEW_RATHALOS, () -> EntityType.Builder.of(NewRathalosEntity::new, MobCategory.CREATURE)
            .sized(2.5f, 2.5f)
            .clientTrackingRange(128)
            .setShouldReceiveVelocityUpdates(true)
            .build(MHNWReferences.NEW_RATHALOS));
    public static final RegistryObject<Item> RATHALOS_EGG_ITEM = ITEMS.register(MHNWReferences.RATHALOS, () -> new ForgeSpawnEggItem(RATHALOS, 0x642628, 0x000000, new Item.Properties()));

    public static final RegistryObject<EntityType<NewRathalosLargeFireballEntity>> NEW_RATHALOS_FIREBALL = ENTITIES.register(MHNWReferences.NEW_RATHALOS_FIREBALL, () -> EntityType.Builder.<NewRathalosLargeFireballEntity>of(NewRathalosLargeFireballEntity::new, MobCategory.MISC).sized(1.0F, 1.0F).clientTrackingRange(4).updateInterval(10).build(MHNWReferences.NEW_RATHALOS_FIREBALL));

    public static final RegistryObject<EntityType<BitterbugEntity>> BITTERBUG = ENTITIES.register(MHNWReferences.BITTERBUG, () -> EntityType.Builder.of(BitterbugEntity::new, MobCategory.CREATURE)
            .sized(0.4f, 0.2f)
            .clientTrackingRange(8)
            .setShouldReceiveVelocityUpdates(false)
            .build(MHNWReferences.BITTERBUG));
    public static final RegistryObject<Item> BITTERBUG_ITEM = ITEMS.register(MHNWReferences.BITTERBUG_ITEM, () -> new ForgeSpawnEggItem(BITTERBUG, 0x351c75, 0x5b5b5b, new Item.Properties()));

    public static final RegistryObject<EntityType<BlangoEntity>> BLANGO = ENTITIES.register(MHNWReferences.BLANGO, () -> EntityType.Builder.of(BlangoEntity::new, MobCategory.CREATURE)
            .sized(0.4f, 0.2f)
            .clientTrackingRange(8)
            .setShouldReceiveVelocityUpdates(false)
            .build(MHNWReferences.BLANGO));
    public static final RegistryObject<Item> BLANGO_EGG_ITEM = ITEMS.register(MHNWReferences.BLANGO, () -> new ForgeSpawnEggItem(BLANGO, 0xA6423E, 0x7C89BA, new Item.Properties()));
    public static final RegistryObject<EntityType<BlangongaEntity>> BLANGONGA = ENTITIES.register(MHNWReferences.BLANGONGA, () -> EntityType.Builder.of(BlangongaEntity::new, MobCategory.CREATURE)
            .sized(0.4f, 0.2f)
            .clientTrackingRange(8)
            .setShouldReceiveVelocityUpdates(false)
            .build(MHNWReferences.BLANGONGA));
    public static final RegistryObject<Item> BLANGONGA_EGG_ITEM = ITEMS.register(MHNWReferences.BLANGONGA, () -> new ForgeSpawnEggItem(BLANGONGA, 0x7D94C9, 0x4CD7B8, new Item.Properties()));

    public static final RegistryObject<EntityType<IzuchiEntity>> IZUCHI = ENTITIES.register(MHNWReferences.IZUCHI, () -> EntityType.Builder.of(IzuchiEntity::new, MobCategory.CREATURE)
            .sized(0.4f, 0.2f)
            .clientTrackingRange(8)
            .setShouldReceiveVelocityUpdates(false)
            .build(MHNWReferences.IZUCHI));
    public static final RegistryObject<Item> IZUCHI_EGG_ITEM = ITEMS.register(MHNWReferences.IZUCHI, () -> new ForgeSpawnEggItem(IZUCHI, 0xCC6D5A, 0xD4D7D9, new Item.Properties()));
    public static final RegistryObject<EntityType<GreatIzuchiEntity>> GIZUCHI = ENTITIES.register(GREAT+MHNWReferences.IZUCHI, () -> EntityType.Builder.of(GreatIzuchiEntity::new, MobCategory.CREATURE)
            .sized(0.4f, 0.2f)
            .clientTrackingRange(8)
            .setShouldReceiveVelocityUpdates(false)
            .build(GREAT+MHNWReferences.IZUCHI));
    public static final RegistryObject<Item> GIZUCHI_EGG_ITEM = ITEMS.register(GREAT+MHNWReferences.IZUCHI, () -> new ForgeSpawnEggItem(GIZUCHI, 0xCC6D5A, 0x3D4F95, new Item.Properties()));

    public static final RegistryObject<EntityType<ToadEntity>> TOAD = ENTITIES.register(MHNWReferences.TOAD, () -> EntityType.Builder.of(ToadEntity::new, MobCategory.CREATURE)
            .sized(0.4f, 0.2f)
            .clientTrackingRange(8)
            .setShouldReceiveVelocityUpdates(false)
            .build(MHNWReferences.TOAD));
    public static final RegistryObject<Item> TOAD_EGG_ITEM = ITEMS.register(MHNWReferences.TOAD, () -> new ForgeSpawnEggItem(TOAD, 0x1E8262, 0xFFDE7D, new Item.Properties()));
    public static final RegistryObject<Item> BUCKET_NITROTOAD_ITEM = ITEMS.register(MHNWReferences.BUCKET_NITROTOAD_ITEM, () ->new ToadBucket(TOAD, Fluids.WATER,basicItem));
    public static final RegistryObject<Item> BUCKET_POISONTOAD_ITEM = ITEMS.register(MHNWReferences.BUCKET_POISONTOAD_ITEM, () -> new ToadBucket(TOAD,Fluids.WATER,basicItem));
    public static final RegistryObject<Item> BUCKET_PARATOAD_ITEM = ITEMS.register(MHNWReferences.BUCKET_PARATOAD_ITEM, () ->new ToadBucket(TOAD,Fluids.WATER,basicItem));
    public static final RegistryObject<Item> BUCKET_SLEEPTOAD_ITEM = ITEMS.register(MHNWReferences.BUCKET_SLEEPTOAD_ITEM, () ->new ToadBucket(TOAD,Fluids.WATER,basicItem));

    public static final RegistryObject<EntityType<ZinogreEntity>> ZINOGRE = ENTITIES.register(MHNWReferences.ZINOGRE, () -> EntityType.Builder.of(ZinogreEntity::new, MobCategory.CREATURE)
            .sized(0.4f, 0.2f)
            .clientTrackingRange(8)
            .setShouldReceiveVelocityUpdates(false)
            .build(MHNWReferences.ZINOGRE));
    public static final RegistryObject<Item> ZINOGRE_EGG_ITEM = ITEMS.register(MHNWReferences.ZINOGRE, () -> new ForgeSpawnEggItem(ZINOGRE, 0x207A8E, 0xD9B367, new Item.Properties()));

    public static final RegistryObject<EntityType<FlashBugEntity>> FLASHBUG = ENTITIES.register(MHNWReferences.FLASHBUG, () -> EntityType.Builder.of(FlashBugEntity::new, MobCategory.AMBIENT)
            .sized(0.4f, 0.2f)
            .clientTrackingRange(8)
            .setShouldReceiveVelocityUpdates(false)
            .build(MHNWReferences.FLASHBUG));
    public static final RegistryObject<Item> FLASHFLY_EGG_ITEM = ITEMS.register(MHNWReferences.FLASHBUG, () -> new ForgeSpawnEggItem(FLASHBUG, 0x636526, 0xFBEB1C, new Item.Properties()));

    public static final RegistryObject<EntityType<SpitFireball>> SPIT_FIREBALL = ENTITIES.register(MHNWReferences.SPIT_FIREBALL, () -> EntityType.Builder.<SpitFireball>of(SpitFireball::new, MobCategory.MISC)
            .sized(0.4F, 0.4f)
            .clientTrackingRange(4)
            .updateInterval(10)
            .build(MHNWReferences.SPIT_FIREBALL));

}
