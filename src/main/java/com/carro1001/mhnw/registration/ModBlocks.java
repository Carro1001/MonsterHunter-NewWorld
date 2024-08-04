package com.carro1001.mhnw.registration;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.blocks.CrystalCluster;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.carro1001.mhnw.registration.ModItems.ITEMS;
import static com.carro1001.mhnw.utils.MHNWReferences.*;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);

    private static final BlockBehaviour.Properties ORE_PROPERTIES = BlockBehaviour.Properties.copy(Blocks.STONE).strength(2f);
    private static final BlockBehaviour.Properties CRYSTAL_PROPERTIES = BlockBehaviour.Properties.copy(Blocks.AMETHYST_BLOCK).noOcclusion().forceSolidOn().strength(2f).dynamicShape();

    public static final RegistryObject<Block> CARBALITE_ORE_BLOCK = BLOCKS.register(CARBALITE_ORE , () -> new DropExperienceBlock(ORE_PROPERTIES));
    public static final RegistryObject<Item> CARBALITE_ORE_ITEM = fromBlock(CARBALITE_ORE_BLOCK);

    public static final RegistryObject<Block> DRAGONITE_ORE_BLOCK = BLOCKS.register(DRAGONITE_ORE , () -> new DropExperienceBlock(ORE_PROPERTIES));
    public static final RegistryObject<Item> DRAGONITE_ORE_ITEM = fromBlock(DRAGONITE_ORE_BLOCK);

    public static final RegistryObject<Block> MACHALITE_ORE_BLOCK = BLOCKS.register(MACHALITE_ORE , () -> new DropExperienceBlock(ORE_PROPERTIES));
    public static final RegistryObject<Item> MACHALITE_ORE_ITEM = fromBlock(MACHALITE_ORE_BLOCK);

    public static final RegistryObject<Block> EARTH_CRYSTAL_CLUSTER_BLOCK = BLOCKS.register(EARTH_CRYSTAL_CLUSTER , () -> new CrystalCluster(CRYSTAL_PROPERTIES));
    public static final RegistryObject<Item> EARTH_CRYSTAL_CLUSTER_ITEM = fromBlock(EARTH_CRYSTAL_CLUSTER_BLOCK);

    public static final RegistryObject<Block> ICE_CRYSTAL_CLUSTER_BLOCK = BLOCKS.register(ICE_CRYSTAL_CLUSTER , () -> new CrystalCluster(CRYSTAL_PROPERTIES));
    public static final RegistryObject<Item> ICE_CRYSTAL_CLUSTER_ITEM = fromBlock(ICE_CRYSTAL_CLUSTER_BLOCK);

    public static <B extends  Block>RegistryObject<Item> fromBlock(RegistryObject<B> block) {
        MHNW.debugLog("registerEntityEgg: ");
        return ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(),(new Item.Properties())));
    }
}
