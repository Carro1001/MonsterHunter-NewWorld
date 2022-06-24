package com.carro1001.mhnw.datagen;

import com.carro1001.mhnw.setup.Registration;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.BlockTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.ExistingFileHelper;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class ModBlockTags extends BlockTagsProvider {

    public ModBlockTags(DataGenerator generator, ExistingFileHelper existingFileHelper) {
        super(generator, MODID,existingFileHelper);
    }

    @Override
    protected void addTags() {
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(Registration.CARBALITE_ORE_BLOCK.get())
                .add(Registration.DRAGONITE_ORE_BLOCK.get())
                .add(Registration.MACHALITE_ORE_BLOCK.get())
                .add(Registration.ICE_CRYSTAL_CLUSTER_BLOCK.get())
                .add(Registration.EARTH_CRYSTAL_CLUSTER_BLOCK.get());

        tag(BlockTags.NEEDS_IRON_TOOL)
                .add(Registration.CARBALITE_ORE_BLOCK.get())
                .add(Registration.DRAGONITE_ORE_BLOCK.get())
                .add(Registration.MACHALITE_ORE_BLOCK.get())
                .add(Registration.ICE_CRYSTAL_CLUSTER_BLOCK.get())
                .add(Registration.EARTH_CRYSTAL_CLUSTER_BLOCK.get());


        tag(Tags.Blocks.ORES)
                .add(Registration.CARBALITE_ORE_BLOCK.get())
                .add(Registration.DRAGONITE_ORE_BLOCK.get())
                .add(Registration.MACHALITE_ORE_BLOCK.get())
                .add(Registration.ICE_CRYSTAL_CLUSTER_BLOCK.get())
                .add(Registration.EARTH_CRYSTAL_CLUSTER_BLOCK.get());
    }

    @Override
    public String getName() {
        return "Monster Hunter: New World Tags";
    }
}
