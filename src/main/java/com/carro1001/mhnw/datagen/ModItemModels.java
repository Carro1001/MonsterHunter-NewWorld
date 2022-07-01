package com.carro1001.mhnw.datagen;

import com.carro1001.mhnw.setup.Registration;
import com.carro1001.mhnw.utils.MHNWReferences;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

import static com.carro1001.mhnw.utils.MHNWReferences.*;

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

        singleTexture(Registration.RAW_MEAT_ITEM.get().getRegistryName().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.RAW_MEAT_ITEM));
        singleTexture(Registration.RARE_MEAT_ITEM.get().getRegistryName().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.RARE_MEAT_ITEM));
        singleTexture(Registration.WELL_DONE_MEAT_ITEM.get().getRegistryName().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.WELL_DONE_MEAT_ITEM));

        singleTexture(Registration.MONSTER_FECES_ITEM.get().getRegistryName().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.MONSTER_FECES_ITEM));

        singleTexture(Registration.RATHALOS_PLATE_ITEM.get().getRegistryName().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ RATHALOS+"_"+PLATE_ITEM));
        singleTexture(Registration.RATHALOS_SCALE_ITEM.get().getRegistryName().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ RATHALOS+"_"+SCALE_ITEM));
        singleTexture(Registration.RATHALOS_TAIL_ITEM.get().getRegistryName().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ RATHALOS+"_"+TAIL_ITEM));
        singleTexture(Registration.RATHALOS_WEBBING_ITEM.get().getRegistryName().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ RATHALOS+"_"+WEBBING_ITEM));

        singleTexture(Registration.RATHIAN_PLATE_ITEM.get().getRegistryName().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ RATHIAN+"_"+PLATE_ITEM));
        singleTexture(Registration.RATHIAN_SCALE_ITEM.get().getRegistryName().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ RATHIAN+"_"+SCALE_ITEM));
        singleTexture(Registration.RATHIAN_TAIL_ITEM.get().getRegistryName().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ RATHIAN+"_"+TAIL_ITEM));
        singleTexture(Registration.RATHIAN_WEBBING_ITEM.get().getRegistryName().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ RATHIAN+"_"+WEBBING_ITEM));

        singleTexture(Registration.FLAME_SACK_ITEM.get().getRegistryName().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ SACKS[0]+"_"+SACK));
        singleTexture(Registration.FREEZER_SACK_ITEM.get().getRegistryName().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ SACKS[1]+"_"+SACK));
        singleTexture(Registration.SCREAMER_SACK_ITEM.get().getRegistryName().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ SACKS[2]+"_"+SACK));
        singleTexture(Registration.SLEEP_SACK_ITEM.get().getRegistryName().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ SACKS[3]+"_"+SACK));

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
        singleTexture(Registration.BITTERBUG_ITEM.get().getRegistryName().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.BITTERBUG_ITEM));
        singleTexture(Registration.BONE_HEAD.get().getRegistryName().getPath(),
                mcLoc("item/generated"),
                "layer0",modLoc("item/" + BONE_ARMOR_HEAD_ITEM));
        singleTexture(Registration.BONE_CHEST.get().getRegistryName().getPath(),
                mcLoc("item/generated"),
                "layer0",modLoc("item/" + BONE_ARMOR_CHESTPLATE_ITEM));
        singleTexture(Registration.BONE_LEGGINGS.get().getRegistryName().getPath(),
                mcLoc("item/generated"),
                "layer0",modLoc("item/" + BONE_ARMOR_LEGGING_ITEM));
        singleTexture(Registration.BONE_BOOTS.get().getRegistryName().getPath(),
                mcLoc("item/generated"),
                "layer0",modLoc("item/" + BONE_ARMOR_BOOT_ITEM));

        withExistingParent(Registration.APTONOTH_EGG_ITEM.get().getRegistryName().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(Registration.RATHIAN_EGG_ITEM.get().getRegistryName().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(Registration.RATHALOS_EGG_ITEM.get().getRegistryName().getPath(), mcLoc("item/template_spawn_egg"));

        withExistingParent(Registration.TOAD_EGG_ITEM.get().getRegistryName().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(Registration.BLANGONGA_EGG_ITEM.get().getRegistryName().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(Registration.BLANGO.get().getRegistryName().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(Registration.GIZUCHI_EGG_ITEM.get().getRegistryName().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(Registration.IZUCHI_EGG_ITEM.get().getRegistryName().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(Registration.ZAMITE_EGG_ITEM.get().getRegistryName().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(Registration.ZINOGRE_EGG_ITEM.get().getRegistryName().getPath(), mcLoc("item/template_spawn_egg"));

    }
}
