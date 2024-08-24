package com.carro1001.mhnw.registration;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.entities.*;
import com.carro1001.mhnw.items.ToadBucket;
import com.carro1001.mhnw.utils.MHNWReferences;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
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

    public static final RegistryObject<EntityType<AptonothEntity>> APTONOTH = registerEntity(MHNWReferences.APTONOTH,AptonothEntity::new, MobCategory.CREATURE,1.32F, 2.7F,8,true);
    public static final RegistryObject<Item> APTONOTH_EGG_ITEM = registerEntityEgg(MHNWReferences.APTONOTH, APTONOTH, 0xB8B584, 0x664D29);

    public static final RegistryObject<EntityType<RathianEntity>> RATHIAN = registerEntity(MHNWReferences.RATHIAN,RathianEntity::new, MobCategory.CREATURE,5.7f, 3.4f,16,true);
    public static final RegistryObject<Item> RATHIAN_EGG_ITEM = registerEntityEgg(MHNWReferences.RATHIAN, RATHIAN, 0x263920, 0x000000);

    public static final RegistryObject<EntityType<RathalosEntity>> RATHALOS = registerEntity(MHNWReferences.RATHALOS,RathalosEntity::new, MobCategory.CREATURE,5.9f, 3.6f,16,true);
    public static final RegistryObject<Item> RATHALOS_EGG_ITEM = registerEntityEgg(MHNWReferences.RATHALOS, RATHALOS, 0x642628, 0x000000);
    public static final RegistryObject<EntityType<RathalosLargeFireballEntity>> RATHALOS_FIREBALL = ENTITIES.register(MHNWReferences.RATHALOS_FIREBALL, () -> EntityType.Builder.<RathalosLargeFireballEntity>of(RathalosLargeFireballEntity::new, MobCategory.MISC).sized(1.0F, 1.0F).clientTrackingRange(4).updateInterval(10).build(MHNWReferences.RATHALOS_FIREBALL));
    public static final RegistryObject<EntityType<NewWorldGrowingEntity>> RATH_TAIL = registerEntity(MHNWReferences.RATHALOS  + "_tail",NewWorldGrowingEntity::new, MobCategory.CREATURE,3f, 1.2f,16,true);

    public static final RegistryObject<EntityType<BugEntity>> BITTERBUG = registerEntity(MHNWReferences.BITTERBUG, BugEntity::new, MobCategory.CREATURE,0.4f, 0.2f,8,true);
    public static final RegistryObject<Item> BITTERBUG_ITEM = registerEntityEgg(MHNWReferences.BITTERBUG_ITEM, BITTERBUG, 0x351c75, 0x5b5b5b);

    public static final RegistryObject<EntityType<BlangoEntity>> BLANGO = registerEntity(MHNWReferences.BLANGO,BlangoEntity::new, MobCategory.CREATURE,1.2f, 2.2f,8,true);
    public static final RegistryObject<Item> BLANGO_EGG_ITEM = registerEntityEgg(MHNWReferences.BLANGO, BLANGO, 0xCC6D5A, 0x3D4F95);

    public static final RegistryObject<EntityType<BlangongaEntity>> BLANGONGA = registerEntity(MHNWReferences.BLANGONGA,BlangongaEntity::new, MobCategory.CREATURE,1.4f, 2.2f,8,true);
    public static final RegistryObject<Item> BLANGONGA_EGG_ITEM = registerEntityEgg(MHNWReferences.BLANGONGA, BLANGONGA, 0x7D94C9, 0x4CD7B8);

    public static final RegistryObject<EntityType<IzuchiEntity>> IZUCHI = registerEntity(MHNWReferences.IZUCHI,IzuchiEntity::new, MobCategory.CREATURE,1.4f, 2.2f,8,true);
    public static final RegistryObject<Item> IZUCHI_EGG_ITEM = registerEntityEgg(MHNWReferences.IZUCHI, IZUCHI, 0xCC6D5A, 0xD4D7D9);

    public static final RegistryObject<EntityType<GreatIzuchiEntity>> GIZUCHI = registerEntity(GREAT+"_"+MHNWReferences.IZUCHI,GreatIzuchiEntity::new, MobCategory.CREATURE,1.4f, 2.8f,8,true);
    public static final RegistryObject<Item> GIZUCHI_EGG_ITEM = registerEntityEgg(GREAT+"_"+MHNWReferences.IZUCHI, GIZUCHI, 0xCC6D5A, 0x3D4F95);

    public static final RegistryObject<EntityType<ToadEntity>> TOAD = registerEntity(MHNWReferences.TOAD,ToadEntity::new, MobCategory.CREATURE,0.4f, 0.2f,8,true);
    public static final RegistryObject<Item> TOAD_EGG_ITEM = registerEntityEgg(MHNWReferences.TOAD, TOAD, 0x1E8262, 0xFFDE7D);
    public static final RegistryObject<Item> BUCKET_NITROTOAD_ITEM = ITEMS.register(MHNWReferences.BUCKET_NITROTOAD_ITEM, () ->new ToadBucket(TOAD, Fluids.WATER,basicItem));
    public static final RegistryObject<Item> BUCKET_POISONTOAD_ITEM = ITEMS.register(MHNWReferences.BUCKET_POISONTOAD_ITEM, () -> new ToadBucket(TOAD,Fluids.WATER,basicItem));
    public static final RegistryObject<Item> BUCKET_PARATOAD_ITEM = ITEMS.register(MHNWReferences.BUCKET_PARATOAD_ITEM, () ->new ToadBucket(TOAD,Fluids.WATER,basicItem));
    public static final RegistryObject<Item> BUCKET_SLEEPTOAD_ITEM = ITEMS.register(MHNWReferences.BUCKET_SLEEPTOAD_ITEM, () ->new ToadBucket(TOAD,Fluids.WATER,basicItem));

    public static final RegistryObject<EntityType<ZinogreEntity>> ZINOGRE = registerEntity(MHNWReferences.ZINOGRE,ZinogreEntity::new, MobCategory.CREATURE,4.4f, 4.2f,8,true);
    public static final RegistryObject<Item> ZINOGRE_EGG_ITEM = registerEntityEgg(MHNWReferences.ZINOGRE, ZINOGRE, 0x207A8E, 0xD9B367);

    public static final RegistryObject<EntityType<LagiacrusEntity>> LAGIACRUS = registerEntity(MHNWReferences.LAGIACRUZ,LagiacrusEntity::new, MobCategory.CREATURE,3.5f, 3.2f,8,true);
    public static final RegistryObject<Item> LAGIACRUS_EGG_ITEM = registerEntityEgg(MHNWReferences.LAGIACRUZ, LAGIACRUS, 0x2A6A7D, 0xB35D47);

    public static final RegistryObject<EntityType<DeviljhoEntity>> DEVILJHO = registerEntity(MHNWReferences.DEVILJHO,DeviljhoEntity::new, MobCategory.CREATURE,1.4f, 3.2f,8,true);
    public static final RegistryObject<Item> DEVILJOE_EGG_ITEM = registerEntityEgg(MHNWReferences.DEVILJHO, DEVILJHO, 0x575528, 0xD7CF70);

    public static final RegistryObject<EntityType<FlashBugEntity>> FLASHBUG = registerEntity(MHNWReferences.FLASHBUG,FlashBugEntity::new, MobCategory.AMBIENT,0.4f, 0.2f,8,true);
    public static final RegistryObject<Item> FLASHFLY_EGG_ITEM = registerEntityEgg(MHNWReferences.FLASHBUG, FLASHBUG, 0x636526, 0xFBEB1C);

    public static <T extends Entity> RegistryObject<EntityType<T>> registerEntity(String name,EntityType.EntityFactory<T> pFactory, MobCategory pCategory, float width,float height, int trackingRange, Boolean shouldReceiveVelocityUpdates){
        MHNW.debugLog("registerEntity: " + name);
        return ENTITIES.register(name, () -> EntityType.Builder.of(pFactory,pCategory)
                .sized(width, height)
                .clientTrackingRange(trackingRange)
                .setShouldReceiveVelocityUpdates(shouldReceiveVelocityUpdates)
                .build(name));
    }

    public static <T extends Mob> RegistryObject<Item> registerEntityEgg(String name, RegistryObject<EntityType<T>> entityTypeRegistryObject, int backgroundColor, int highlightColor){
        MHNW.debugLog("registerEntityEgg: " + name);
        return ITEMS.register(name, () -> new ForgeSpawnEggItem(entityTypeRegistryObject, backgroundColor, highlightColor, new Item.Properties()));
    }

}
