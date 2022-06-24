package com.carro1001.mhnw.datagen;

import com.carro1001.mhnw.setup.Registration;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.ExistingFileHelper;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class ModItemTags extends ItemTagsProvider {
    public ModItemTags(DataGenerator generator, ModBlockTags blocktags, ExistingFileHelper existingFileHelper) {
        super(generator,blocktags, MODID,existingFileHelper);
    }

    @Override
    protected void addTags() {
        tag(Tags.Items.ORES)
                .add(Registration.CARBALITE_ORE_ITEM.get())
                .add(Registration.DRAGONITE_ORE_ITEM.get())
                .add(Registration.MACHALITE_ORE_ITEM.get())
                .add(Registration.ICE_CRYSTAL_CLUSTER_ITEM.get())
                .add(Registration.EARTH_CRYSTAL_CLUSTER_ITEM.get());
    }

    @Override
    public String getName() {
        return "Monster Hunter: New World  Tags";
    }
}
