package com.carro1001.mhnw.datagen;

import com.carro1001.mhnw.registration.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class ModBlockTags extends BlockTagsProvider{

    public ModBlockTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider pProvider) {
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.CARBALITE_ORE_BLOCK.get(),
                        ModBlocks.MACHALITE_ORE_BLOCK.get(),
                        ModBlocks.DRAGONITE_ORE_BLOCK.get(),
                        ModBlocks.EARTH_CRYSTAL_CLUSTER_BLOCK.get(),
                        ModBlocks.ICE_CRYSTAL_CLUSTER_BLOCK.get());


        this.tag(BlockTags.NEEDS_IRON_TOOL)
                .add(ModBlocks.CARBALITE_ORE_BLOCK.get(),
                ModBlocks.EARTH_CRYSTAL_CLUSTER_BLOCK.get(),
                ModBlocks.ICE_CRYSTAL_CLUSTER_BLOCK.get());

        this.tag(BlockTags.NEEDS_DIAMOND_TOOL)
                .add(ModBlocks.MACHALITE_ORE_BLOCK.get());

        this.tag(Tags.Blocks.NEEDS_NETHERITE_TOOL)
                .add(ModBlocks.MACHALITE_ORE_BLOCK.get());

    }
}
