package com.carro1001.mhnw.setup;

import com.carro1001.mhnw.client.particles.iceParticle.IceParticleType;
import com.carro1001.mhnw.client.particles.poisonParticle.PoisonParticleType;
import com.carro1001.mhnw.client.particles.sleepParticle.SleepParticleType;
import com.carro1001.mhnw.client.particles.thunderParticle.ThunderParticleType;
import com.carro1001.mhnw.entities.*;
import com.carro1001.mhnw.items.BoneArmorItem;
import com.carro1001.mhnw.items.ToadBucket;
import com.carro1001.mhnw.utils.MHNWReferences;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.AmethystBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.OreBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.carro1001.mhnw.MHNW.GROUP;
import static com.carro1001.mhnw.utils.MHNWReferences.*;

public class Registration {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, MODID);

    public static  void init(){
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        PARTICLES.register(bus);

        ENTITIES.register(bus);
    }
    public static final RegistryObject<SleepParticleType> SLEEP_PARTICLE_TYPE = PARTICLES.register(MHNWReferences.SLEEP_PARTICLE_REGNAME, SleepParticleType::new);
    public static final RegistryObject<ThunderParticleType> THUNDER_PARTICLE_TYPE = PARTICLES.register(THUNDER_PARTICLE_REGNAME, ThunderParticleType::new);
    public static final RegistryObject<PoisonParticleType> POISON_PARTICLE_TYPE = PARTICLES.register(POISON_PARTICLE_REGNAME, PoisonParticleType::new);
    public static final RegistryObject<IceParticleType> ICE_PARTICLE_TYPE = PARTICLES.register(ICE_PARTICLE_REGNAME, IceParticleType::new);

    private static final BlockBehaviour.Properties ORE_PROPERTIES = BlockBehaviour.Properties.of(Material.STONE).strength(2f);
    private static final BlockBehaviour.Properties CRYSTAL_PROPERTIES = BlockBehaviour.Properties.of(Material.AMETHYST).noOcclusion().strength(2f).dynamicShape();

    public static final RegistryObject<Block> CARBALITE_ORE_BLOCK = BLOCKS.register(CARBALITE_ORE , () -> new OreBlock(ORE_PROPERTIES));
    public static final RegistryObject<Item> CARBALITE_ORE_ITEM = fromBlock(CARBALITE_ORE_BLOCK);

    public static final RegistryObject<Block> DRAGONITE_ORE_BLOCK = BLOCKS.register(DRAGONITE_ORE , () -> new OreBlock(ORE_PROPERTIES));
    public static final RegistryObject<Item> DRAGONITE_ORE_ITEM = fromBlock(DRAGONITE_ORE_BLOCK);

    public static final RegistryObject<Block> MACHALITE_ORE_BLOCK = BLOCKS.register(MACHALITE_ORE , () -> new OreBlock(ORE_PROPERTIES));
    public static final RegistryObject<Item> MACHALITE_ORE_ITEM = fromBlock(MACHALITE_ORE_BLOCK);

    public static final RegistryObject<Block> EARTH_CRYSTAL_CLUSTER_BLOCK = BLOCKS.register(EARTH_CRYSTAL_CLUSTER , () -> new AmethystBlock(CRYSTAL_PROPERTIES));
    public static final RegistryObject<Item> EARTH_CRYSTAL_CLUSTER_ITEM = fromBlock(EARTH_CRYSTAL_CLUSTER_BLOCK);

    public static final RegistryObject<Block> ICE_CRYSTAL_CLUSTER_BLOCK = BLOCKS.register(ICE_CRYSTAL_CLUSTER , () -> new AmethystBlock(CRYSTAL_PROPERTIES));
    public static final RegistryObject<Item> ICE_CRYSTAL_CLUSTER_ITEM = fromBlock(ICE_CRYSTAL_CLUSTER_BLOCK);


    public static final Item.Properties basicItem = (new Item.Properties()).tab(GROUP);
    public static final RegistryObject<Item> CARBALITE_ITEM = ITEMS.register(MHNWReferences.CARBALITE_ITEM, () -> new Item(basicItem));
    public static final RegistryObject<Item> DRAGONITE_ITEM = ITEMS.register(MHNWReferences.DRAGONITE_ITEM, () -> new Item(basicItem));
    public static final RegistryObject<Item> MACHALITE_ITEM = ITEMS.register(MHNWReferences.MACHALITE_ITEM, () -> new Item(basicItem));
    public static final RegistryObject<Item> RAW_CARBALITE_ITEM = ITEMS.register(MHNWReferences.RAW_CARBALITE_ITEM, () -> new Item(basicItem));
    public static final RegistryObject<Item> RAW_DRAGONITE_ITEM = ITEMS.register(MHNWReferences.RAW_DRAGONITE_ITEM, () -> new Item(basicItem));
    public static final RegistryObject<Item> RAW_MACHALITE_ITEM = ITEMS.register(MHNWReferences.RAW_MACHALITE_ITEM, () -> new Item(basicItem));
    public static final RegistryObject<Item> EARTH_CRYSTAL_ITEM = ITEMS.register(MHNWReferences.EARTH_CRYSTAL_ITEM, () -> new Item(basicItem));
    public static final RegistryObject<Item> ICE_CRYSTAL_ITEM = ITEMS.register(MHNWReferences.ICE_CRYSTAL_ITEM, () -> new Item(basicItem));

    public static final FoodProperties RAW_MEAT = (new FoodProperties.Builder()).nutrition(4).saturationMod(0.1F).effect(new MobEffectInstance(MobEffects.HUNGER, 600, 0), 0.8F).meat().build();
    public static final FoodProperties RARE_MEAT = (new FoodProperties.Builder()).nutrition(6).saturationMod(0.6F).meat().build();
    public static final FoodProperties WELL_DONE_MEAT = (new FoodProperties.Builder()).nutrition(8).saturationMod(0.8F).meat().build();

    public static final RegistryObject<Item> RAW_MEAT_ITEM = ITEMS.register(MHNWReferences.RAW_MEAT_ITEM, () -> new Item(basicItem.food(RAW_MEAT)));
    public static final RegistryObject<Item> RARE_MEAT_ITEM = ITEMS.register(MHNWReferences.RARE_MEAT_ITEM, () -> new Item(basicItem.food(RARE_MEAT)));
    public static final RegistryObject<Item> WELL_DONE_MEAT_ITEM = ITEMS.register(MHNWReferences.WELL_DONE_MEAT_ITEM, () -> new Item(basicItem.food(WELL_DONE_MEAT)));
    public static final RegistryObject<Item> MONSTER_FECES_ITEM = ITEMS.register(MHNWReferences.MONSTER_FECES_ITEM, () -> new Item(basicItem));

    public static final RegistryObject<Item> RATHIAN_PLATE_ITEM = ITEMS.register(MHNWReferences.RATHIAN+"_"+ PLATE_ITEM, () -> new Item(basicItem));
    public static final RegistryObject<Item> RATHIAN_SCALE_ITEM = ITEMS.register(MHNWReferences.RATHIAN+"_"+ SCALE_ITEM, () -> new Item(basicItem));
    public static final RegistryObject<Item> RATHIAN_TAIL_ITEM = ITEMS.register(MHNWReferences.RATHIAN+"_"+ TAIL_ITEM, () -> new Item(basicItem));
    public static final RegistryObject<Item> RATHIAN_WEBBING_ITEM = ITEMS.register(MHNWReferences.RATHIAN+"_"+ WEBBING_ITEM, () -> new Item(basicItem));

    public static final RegistryObject<Item> RATHALOS_PLATE_ITEM = ITEMS.register(MHNWReferences.RATHALOS+"_"+ PLATE_ITEM, () -> new Item(basicItem));
    public static final RegistryObject<Item> RATHALOS_SCALE_ITEM = ITEMS.register(MHNWReferences.RATHALOS+"_"+ SCALE_ITEM, () -> new Item(basicItem));
    public static final RegistryObject<Item> RATHALOS_TAIL_ITEM = ITEMS.register(MHNWReferences.RATHALOS+"_"+ TAIL_ITEM, () -> new Item(basicItem));
    public static final RegistryObject<Item> RATHALOS_WEBBING_ITEM = ITEMS.register(MHNWReferences.RATHALOS+"_"+ WEBBING_ITEM, () -> new Item(basicItem));

    public static final RegistryObject<Item> FLAME_SACK_ITEM = ITEMS.register(SACKS[0]+"_"+ SACK, () -> new Item(basicItem));
    public static final RegistryObject<Item> FREEZER_SACK_ITEM = ITEMS.register(SACKS[1]+"_"+ SACK, () -> new Item(basicItem));
    public static final RegistryObject<Item> SCREAMER_SACK_ITEM = ITEMS.register(SACKS[2]+"_"+ SACK, () -> new Item(basicItem));
    public static final RegistryObject<Item> SLEEP_SACK_ITEM = ITEMS.register(SACKS[3]+"_"+ SACK, () -> new Item(basicItem));

    public static final RegistryObject<Item> BUCKET_NITROTOAD_ITEM = ITEMS.register(MHNWReferences.BUCKET_NITROTOAD_ITEM, () ->new ToadBucket(Registration.TOAD,Fluids.WATER,basicItem));
    public static final RegistryObject<Item> BUCKET_POISONTOAD_ITEM = ITEMS.register(MHNWReferences.BUCKET_POISONTOAD_ITEM, () -> new ToadBucket(Registration.TOAD,Fluids.WATER,basicItem));
    public static final RegistryObject<Item> BUCKET_PARATOAD_ITEM = ITEMS.register(MHNWReferences.BUCKET_PARATOAD_ITEM, () ->new ToadBucket(Registration.TOAD,Fluids.WATER,basicItem));
    public static final RegistryObject<Item> BUCKET_SLEEPTOAD_ITEM = ITEMS.register(MHNWReferences.BUCKET_SLEEPTOAD_ITEM, () ->new ToadBucket(Registration.TOAD,Fluids.WATER,basicItem));

    public static final RegistryObject<EntityType<AptonothEntity>> APTONOTH = ENTITIES.register(MHNWReferences.APTONOTH, () -> EntityType.Builder.of(AptonothEntity::new, MobCategory.CREATURE)
            .sized(1.4f, 3.2f)
            .clientTrackingRange(8)
            .setShouldReceiveVelocityUpdates(false)
            .build(MHNWReferences.APTONOTH));
    public static final RegistryObject<Item> APTONOTH_EGG_ITEM = ITEMS.register(MHNWReferences.APTONOTH, () -> new ForgeSpawnEggItem(APTONOTH, 0xB8B584, 0x664D29, new Item.Properties().tab(GROUP)));

    public static final RegistryObject<EntityType<RathianEntity>> RATHIAN = ENTITIES.register(MHNWReferences.RATHIAN, () -> EntityType.Builder.of(RathianEntity::new, MobCategory.CREATURE)
            .sized(5.5f, 5.5f)
            .clientTrackingRange(8)
            .setShouldReceiveVelocityUpdates(false)
            .build(MHNWReferences.RATHIAN));
    public static final RegistryObject<Item> RATHIAN_EGG_ITEM = ITEMS.register(MHNWReferences.RATHIAN, () -> new ForgeSpawnEggItem(RATHIAN, 0x263920, 0x000000, new Item.Properties().tab(GROUP)));

    public static final RegistryObject<EntityType<RathalosEntity>> RATHALOS = ENTITIES.register(MHNWReferences.RATHALOS, () -> EntityType.Builder.of(RathalosEntity::new, MobCategory.CREATURE)
            .sized(5.5f, 4.5f)
            .clientTrackingRange(8)
            .setShouldReceiveVelocityUpdates(false)
            .build(MHNWReferences.RATHALOS));
    public static final RegistryObject<Item> RATHALOS_EGG_ITEM = ITEMS.register(MHNWReferences.RATHALOS, () -> new ForgeSpawnEggItem(RATHALOS, 0x642628, 0x000000, new Item.Properties().tab(GROUP)));

    public static final RegistryObject<EntityType<BitterbugEntity>> BITTERBUG = ENTITIES.register(MHNWReferences.BITTERBUG, () -> EntityType.Builder.of(BitterbugEntity::new, MobCategory.CREATURE)
            .sized(0.4f, 0.2f)
            .clientTrackingRange(8)
            .setShouldReceiveVelocityUpdates(false)
            .build(MHNWReferences.BITTERBUG));
    public static final RegistryObject<Item> BITTERBUG_ITEM = ITEMS.register(MHNWReferences.BITTERBUG_ITEM, () -> new ForgeSpawnEggItem(BITTERBUG, 0x351c75, 0x5b5b5b, new Item.Properties().tab(GROUP)));

    public static final RegistryObject<EntityType<BlangoEntity>> BLANGO = ENTITIES.register(MHNWReferences.BLANGO, () -> EntityType.Builder.of(BlangoEntity::new, MobCategory.CREATURE)
            .sized(0.4f, 0.2f)
            .clientTrackingRange(8)
            .setShouldReceiveVelocityUpdates(false)
            .build(MHNWReferences.BLANGO));
    public static final RegistryObject<Item> BLANGO_EGG_ITEM = ITEMS.register(MHNWReferences.BLANGO, () -> new ForgeSpawnEggItem(BLANGO, 0xA6423E, 0x7C89BA, new Item.Properties().tab(GROUP)));
    public static final RegistryObject<EntityType<BlangongaEntity>> BLANGONGA = ENTITIES.register(MHNWReferences.BLANGONGA, () -> EntityType.Builder.of(BlangongaEntity::new, MobCategory.CREATURE)
            .sized(0.4f, 0.2f)
            .clientTrackingRange(8)
            .setShouldReceiveVelocityUpdates(false)
            .build(MHNWReferences.BLANGONGA));
    public static final RegistryObject<Item> BLANGONGA_EGG_ITEM = ITEMS.register(MHNWReferences.BLANGONGA, () -> new ForgeSpawnEggItem(BLANGONGA, 0x7D94C9, 0x4CD7B8, new Item.Properties().tab(GROUP)));

    public static final RegistryObject<EntityType<IzuchiEntity>> IZUCHI = ENTITIES.register(MHNWReferences.IZUCHI, () -> EntityType.Builder.of(IzuchiEntity::new, MobCategory.CREATURE)
            .sized(0.4f, 0.2f)
            .clientTrackingRange(8)
            .setShouldReceiveVelocityUpdates(false)
            .build(MHNWReferences.IZUCHI));
    public static final RegistryObject<Item> IZUCHI_EGG_ITEM = ITEMS.register(MHNWReferences.IZUCHI, () -> new ForgeSpawnEggItem(IZUCHI, 0xCC6D5A, 0xD4D7D9, new Item.Properties().tab(GROUP)));
    public static final RegistryObject<EntityType<GreatIzuchiEntity>> GIZUCHI = ENTITIES.register(GREAT+MHNWReferences.IZUCHI, () -> EntityType.Builder.of(GreatIzuchiEntity::new, MobCategory.CREATURE)
            .sized(0.4f, 0.2f)
            .clientTrackingRange(8)
            .setShouldReceiveVelocityUpdates(false)
            .build(GREAT+MHNWReferences.IZUCHI));
    public static final RegistryObject<Item> GIZUCHI_EGG_ITEM = ITEMS.register(GREAT+MHNWReferences.IZUCHI, () -> new ForgeSpawnEggItem(GIZUCHI, 0xCC6D5A, 0x3D4F95, new Item.Properties().tab(GROUP)));

    public static final RegistryObject<EntityType<ToadEntity>> TOAD = ENTITIES.register(MHNWReferences.TOAD, () -> EntityType.Builder.of(ToadEntity::new, MobCategory.CREATURE)
            .sized(0.4f, 0.2f)
            .clientTrackingRange(8)
            .setShouldReceiveVelocityUpdates(false)
            .build(MHNWReferences.TOAD));
    public static final RegistryObject<Item> TOAD_EGG_ITEM = ITEMS.register(MHNWReferences.TOAD, () -> new ForgeSpawnEggItem(TOAD, 0x1E8262, 0xFFDE7D, new Item.Properties().tab(GROUP)));

    public static final RegistryObject<EntityType<ZinogreEntity>> ZINOGRE = ENTITIES.register(MHNWReferences.ZINOGRE, () -> EntityType.Builder.of(ZinogreEntity::new, MobCategory.CREATURE)
            .sized(0.4f, 0.2f)
            .clientTrackingRange(8)
            .setShouldReceiveVelocityUpdates(false)
            .build(MHNWReferences.ZINOGRE));
    public static final RegistryObject<Item> ZINOGRE_EGG_ITEM = ITEMS.register(MHNWReferences.ZINOGRE, () -> new ForgeSpawnEggItem(ZINOGRE, 0x207A8E, 0xD9B367, new Item.Properties().tab(GROUP)));


    public static final RegistryObject<BoneArmorItem> BONE_HEAD = ITEMS.register(BONE_ARMOR_HEAD_ITEM,
            () -> new BoneArmorItem(ArmorMaterials.CHAIN, EquipmentSlot.HEAD, new Item.Properties()));
    public static final RegistryObject<BoneArmorItem> BONE_CHEST = ITEMS.register(BONE_ARMOR_CHESTPLATE_ITEM,
            () -> new BoneArmorItem(ArmorMaterials.CHAIN, EquipmentSlot.CHEST, new Item.Properties()));
    public static final RegistryObject<BoneArmorItem> BONE_LEGGINGS = ITEMS.register(BONE_ARMOR_LEGGING_ITEM,
            () -> new BoneArmorItem(ArmorMaterials.CHAIN, EquipmentSlot.LEGS, new Item.Properties()));
    public static final RegistryObject<BoneArmorItem> BONE_BOOTS = ITEMS.register(BONE_ARMOR_BOOT_ITEM,
            () -> new BoneArmorItem(ArmorMaterials.CHAIN, EquipmentSlot.FEET, new Item.Properties()));

    public static <B extends  Block>RegistryObject<Item> fromBlock(RegistryObject<B> block) {
        return ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(),(new Item.Properties()).tab(GROUP)));
    }
}
