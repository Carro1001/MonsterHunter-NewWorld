package com.carro1001.mhnw.datagen;

import com.carro1001.mhnw.setup.Registration;
import com.carro1001.mhnw.utils.MHNWReferences;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class ModItemModels extends ItemModelProvider {
    public ModItemModels(DataGenerator generator, ExistingFileHelper existingFileHelper) {
        super(generator, MODID,existingFileHelper);

    }

    @Override
    protected void registerModels() {
        withExistingParent(Registration.CARBALITE_ORE_ITEM.get().getRegistryName().getPath(),modLoc("block/"+ MHNWReferences.CARBALITE_ORE));
        withExistingParent(Registration.MACHALITE_ORE_ITEM.get().getRegistryName().getPath(),modLoc("block/"+ MHNWReferences.MACHALITE_ORE));
        withExistingParent(Registration.DRAGONITE_ORE_ITEM.get().getRegistryName().getPath(),modLoc("block/"+ MHNWReferences.DRAGONITE_ORE));
        withExistingParent(Registration.EARTH_CRYSTAL_CLUSTER_BLOCK.get().getRegistryName().getPath(),modLoc("block/" + MHNWReferences.EARTH_CRYSTAL_CLUSTER));
        withExistingParent(Registration.ICE_CRYSTAL_CLUSTER_BLOCK.get().getRegistryName().getPath(),modLoc("block/"+ MHNWReferences.ICE_CRYSTAL_CLUSTER));

        singleTexture(Registration.CARBALITE_ITEM.get().getRegistryName().getPath(),
                mcLoc("item/generated"),
                "layer0",modLoc("item/"+ MHNWReferences.CARBALITE_ITEM));
        singleTexture(Registration.DRAGONITE_ITEM.get().getRegistryName().getPath(),
                mcLoc("item/generated"),
                "layer0",modLoc("item/"+ MHNWReferences.DRAGONITE_ITEM));
        singleTexture(Registration.MACHALITE_ITEM.get().getRegistryName().getPath(),
                mcLoc("item/generated"),
                "layer0",modLoc("item/"+ MHNWReferences.MACHALITE_ITEM));
        singleTexture(Registration.RAW_CARBALITE_ITEM.get().getRegistryName().getPath(),
                mcLoc("item/generated"),
                "layer0",modLoc("item/"+ MHNWReferences.RAW_CARBALITE_ITEM));
        singleTexture(Registration.RAW_DRAGONITE_ITEM.get().getRegistryName().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.RAW_DRAGONITE_ITEM));
        singleTexture(Registration.RAW_MACHALITE_ITEM.get().getRegistryName().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.RAW_MACHALITE_ITEM));
        singleTexture(Registration.EARTH_CRYSTAL_ITEM.get().getRegistryName().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.EARTH_CRYSTAL_ITEM));
        singleTexture(Registration.ICE_CRYSTAL_ITEM.get().getRegistryName().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.ICE_CRYSTAL_ITEM));
        singleTexture(Registration.WELL_DONE_MEAT_ITEM.get().getRegistryName().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.WELL_DONE_MEAT_ITEM));
        singleTexture(Registration.MONSTER_FECES_ITEM.get().getRegistryName().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.MONSTER_FECES_ITEM));

        singleTexture(Registration.BUCKET_SLEEPTOAD_ITEM.get().getRegistryName().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.BUCKET_SLEEPTOAD_ITEM));
        singleTexture(Registration.BUCKET_PARATOAD_ITEM.get().getRegistryName().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.BUCKET_PARATOAD_ITEM));
        singleTexture(Registration.BUCKET_POISONTOAD_ITEM.get().getRegistryName().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.BUCKET_POISONTOAD_ITEM));
        singleTexture(Registration.BUCKET_NITROTOAD_ITEM.get().getRegistryName().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.BUCKET_NITROTOAD_ITEM));

        withExistingParent(Registration.APTONOTH_EGG_ITEM.get().getRegistryName().getPath(), mcLoc("item/template_spawn_egg"));

    }
}
