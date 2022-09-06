package com.carro1001.mhnw.datagen;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.BlockTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.ExistingFileHelper;

import static com.carro1001.mhnw.registration.ModBlocks.*;
import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class ModBlockTags extends BlockTagsProvider {

    public ModBlockTags(DataGenerator generator, ExistingFileHelper existingFileHelper) {
        super(generator, MODID,existingFileHelper);
    }

    @Override
    protected void addTags() {
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(CARBALITE_ORE_BLOCK.get())
                .add(DRAGONITE_ORE_BLOCK.get())
                .add(MACHALITE_ORE_BLOCK.get())
                .add(ICE_CRYSTAL_CLUSTER_BLOCK.get())
                .add(EARTH_CRYSTAL_CLUSTER_BLOCK.get());

        tag(BlockTags.NEEDS_IRON_TOOL)
                .add(CARBALITE_ORE_BLOCK.get())
                .add(DRAGONITE_ORE_BLOCK.get())
                .add(MACHALITE_ORE_BLOCK.get())
                .add(ICE_CRYSTAL_CLUSTER_BLOCK.get())
                .add(EARTH_CRYSTAL_CLUSTER_BLOCK.get());


        tag(Tags.Blocks.ORES)
                .add(CARBALITE_ORE_BLOCK.get())
                .add(DRAGONITE_ORE_BLOCK.get())
                .add(MACHALITE_ORE_BLOCK.get())
                .add(ICE_CRYSTAL_CLUSTER_BLOCK.get())
                .add(EARTH_CRYSTAL_CLUSTER_BLOCK.get());

    }

    @Override
    public String getName() {
        return "Monster Hunter: New World Tags";
    }
}
