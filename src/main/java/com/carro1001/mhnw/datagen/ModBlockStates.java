package com.carro1001.mhnw.datagen;

import com.carro1001.mhnw.registration.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;


public class ModBlockStates extends BlockStateProvider  {
    public ModBlockStates(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        blockWithItem(ModBlocks.CARBALITE_ORE_BLOCK);
        blockWithItem(ModBlocks.DRAGONITE_ORE_BLOCK);

        blockWithItem(ModBlocks.MACHALITE_ORE_BLOCK);
        cross(ModBlocks.EARTH_CRYSTAL_CLUSTER_BLOCK);
        cross(ModBlocks.ICE_CRYSTAL_CLUSTER_BLOCK);
    }
    private void cross(RegistryObject<Block> blockRegistryObject){
        simpleBlockWithItem(blockRegistryObject.get(), models().cross(name(blockRegistryObject.get()),blockTexture(blockRegistryObject.get())));
    }

    private String name(Block block) {
        return key(block).getPath();
    }

    private ResourceLocation key(Block block) {
        return ForgeRegistries.BLOCKS.getKey(block);
    }

    private void blockWithItem(RegistryObject<Block> blockRegistryObject) {
        simpleBlockWithItem(blockRegistryObject.get(), cubeAll(blockRegistryObject.get()));

    }
}
