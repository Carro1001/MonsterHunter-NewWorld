package com.carro1001.mhnw.setup;

import com.carro1001.mhnw.entities.aptonoth.AptonothEntity;
import com.carro1001.mhnw.items.bone_armor.BoneArmorItem;
import com.carro1001.mhnw.utils.MHNWReferences;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.AmethystBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.OreBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
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

    public static  void init(){
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        ENTITIES.register(bus);
    }

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
    public static final RegistryObject<Item> WELL_DONE_MEAT_ITEM = ITEMS.register(MHNWReferences.WELL_DONE_MEAT_ITEM, () -> new Item(basicItem));
    public static final RegistryObject<Item> MONSTER_FECES_ITEM = ITEMS.register(MHNWReferences.MONSTER_FECES_ITEM, () -> new Item(basicItem));

    public static final RegistryObject<Item> BUCKET_NITROTOAD_ITEM = ITEMS.register(MHNWReferences.BUCKET_NITROTOAD_ITEM, () -> new Item(basicItem));
    public static final RegistryObject<Item> BUCKET_POISONTOAD_ITEM = ITEMS.register(MHNWReferences.BUCKET_POISONTOAD_ITEM, () -> new Item(basicItem));
    public static final RegistryObject<Item> BUCKET_PARATOAD_ITEM = ITEMS.register(MHNWReferences.BUCKET_PARATOAD_ITEM, () -> new Item(basicItem));
    public static final RegistryObject<Item> BUCKET_SLEEPTOAD_ITEM = ITEMS.register(MHNWReferences.BUCKET_SLEEPTOAD_ITEM, () -> new Item(basicItem));

    public static final RegistryObject<EntityType<AptonothEntity>> APTONOTH = ENTITIES.register(MHNWReferences.APTONOTH, () -> EntityType.Builder.of(AptonothEntity::new, MobCategory.CREATURE)
            .sized(0.7f, 1.5f)
            .clientTrackingRange(8)
            .setShouldReceiveVelocityUpdates(false)
            .build(MHNWReferences.APTONOTH));
    public static final RegistryObject<Item> APTONOTH_EGG_ITEM = ITEMS.register(MHNWReferences.APTONOTH, () -> new ForgeSpawnEggItem(APTONOTH, 0x351c75, 0x5b5b5b, new Item.Properties().tab(GROUP)));

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
