package com.carro1001.mhnw.datagen;

import com.carro1001.mhnw.utils.MHNWReferences;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

import static com.carro1001.mhnw.registration.ModBlocks.*;
import static com.carro1001.mhnw.registration.ModEntities.*;
import static com.carro1001.mhnw.registration.ModItems.*;
import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class ModItemModels extends ItemModelProvider {
    public ModItemModels(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        withExistingParent(CARBALITE_ORE_ITEM.getId().getPath(),modLoc("block/"+ MHNWReferences.CARBALITE_ORE));
        withExistingParent(MACHALITE_ORE_ITEM.getId().getPath(),modLoc("block/"+ MHNWReferences.MACHALITE_ORE));
        withExistingParent(DRAGONITE_ORE_ITEM.getId().getPath(),modLoc("block/"+ MHNWReferences.DRAGONITE_ORE));
        withExistingParent(EARTH_CRYSTAL_CLUSTER_BLOCK.getId().getPath(),modLoc("block/" + MHNWReferences.EARTH_CRYSTAL_CLUSTER));
        withExistingParent(ICE_CRYSTAL_CLUSTER_BLOCK.getId().getPath(),modLoc("block/"+ MHNWReferences.ICE_CRYSTAL_CLUSTER));

        singleTexture(CARBALITE_ITEM.getId().getPath(),
                mcLoc("item/generated"),
                "layer0",modLoc("item/"+ MHNWReferences.CARBALITE_ITEM));
        singleTexture(DRAGONITE_ITEM.getId().getPath(),
                mcLoc("item/generated"),
                "layer0",modLoc("item/"+ MHNWReferences.DRAGONITE_ITEM));
        singleTexture(MACHALITE_ITEM.getId().getPath(),
                mcLoc("item/generated"),
                "layer0",modLoc("item/"+ MHNWReferences.MACHALITE_ITEM));
        singleTexture(RAW_CARBALITE_ITEM.getId().getPath(),
                mcLoc("item/generated"),
                "layer0",modLoc("item/"+ MHNWReferences.RAW_CARBALITE_ITEM));
        singleTexture(RAW_DRAGONITE_ITEM.getId().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.RAW_DRAGONITE_ITEM));
        singleTexture(RAW_MACHALITE_ITEM.getId().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.RAW_MACHALITE_ITEM));
        singleTexture(EARTH_CRYSTAL_ITEM.getId().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.EARTH_CRYSTAL_ITEM));
        singleTexture(ICE_CRYSTAL_ITEM.getId().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.ICE_CRYSTAL_ITEM));

        singleTexture(RAW_MEAT_ITEM.getId().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.RAW_MEAT_ITEM));
        singleTexture(RARE_MEAT_ITEM.getId().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.RARE_MEAT_ITEM));
        singleTexture(WELL_DONE_MEAT_ITEM.getId().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.WELL_DONE_MEAT_ITEM));

        singleTexture(MONSTER_FECES_ITEM.getId().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.MONSTER_FECES_ITEM));

        singleTexture(RATHALOS_PLATE_ITEM.getId().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.RATHALOS+"_"+MHNWReferences.PLATE_ITEM));
        singleTexture(RATHALOS_SCALE_ITEM.getId().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.RATHALOS+"_"+MHNWReferences.SCALE_ITEM));
        singleTexture(RATHALOS_TAIL_ITEM.getId().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.RATHALOS+"_"+MHNWReferences.TAIL_ITEM));
        singleTexture(RATHALOS_WEBBING_ITEM.getId().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.RATHALOS+"_"+MHNWReferences.WEBBING_ITEM));

        singleTexture(RATHIAN_PLATE_ITEM.getId().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.RATHIAN+"_"+MHNWReferences.PLATE_ITEM));
        singleTexture(RATHIAN_SCALE_ITEM.getId().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.RATHIAN+"_"+MHNWReferences.SCALE_ITEM));
        singleTexture(RATHIAN_TAIL_ITEM.getId().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.RATHIAN+"_"+MHNWReferences.TAIL_ITEM));
        singleTexture(RATHIAN_WEBBING_ITEM.getId().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.RATHIAN+"_"+MHNWReferences.WEBBING_ITEM));

        singleTexture(FLAME_SACK_ITEM.getId().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.SACKS[0]+"_"+MHNWReferences.SACK));
        singleTexture(FREEZER_SACK_ITEM.getId().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.SACKS[1]+"_"+MHNWReferences.SACK));
        singleTexture(SCREAMER_SACK_ITEM.getId().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.SACKS[2]+"_"+MHNWReferences.SACK));
        singleTexture(SLEEP_SACK_ITEM.getId().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.SACKS[3]+"_"+MHNWReferences.SACK));

        singleTexture(BUCKET_SLEEPTOAD_ITEM.getId().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.BUCKET_SLEEPTOAD_ITEM));
        singleTexture(BUCKET_PARATOAD_ITEM.getId().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.BUCKET_PARATOAD_ITEM));
        singleTexture(BUCKET_POISONTOAD_ITEM.getId().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.BUCKET_POISONTOAD_ITEM));
        singleTexture(BUCKET_NITROTOAD_ITEM.getId().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.BUCKET_NITROTOAD_ITEM));
        singleTexture(BITTERBUG_ITEM.getId().getPath(),
                mcLoc("item/handheld"),
                "layer0",modLoc("item/"+ MHNWReferences.BITTERBUG_ITEM));
        singleTexture(BONE_HEAD.getId().getPath(),
                mcLoc("item/generated"),
                "layer0",modLoc("item/" + MHNWReferences.BONE_ARMOR_HEAD_ITEM));
        singleTexture(BONE_CHEST.getId().getPath(),
                mcLoc("item/generated"),
                "layer0",modLoc("item/" + MHNWReferences.BONE_ARMOR_CHESTPLATE_ITEM));
        singleTexture(BONE_LEGGINGS.getId().getPath(),
                mcLoc("item/generated"),
                "layer0",modLoc("item/" + MHNWReferences.BONE_ARMOR_LEGGING_ITEM));
        singleTexture(BONE_BOOTS.getId().getPath(),
                mcLoc("item/generated"),
                "layer0",modLoc("item/" + MHNWReferences.BONE_ARMOR_BOOT_ITEM));

        withExistingParent(APTONOTH_EGG_ITEM.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(RATHIAN_EGG_ITEM.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(RATHALOS_EGG_ITEM.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(DEVILJOE_EGG_ITEM.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(LAGIACRUS_EGG_ITEM.getId().getPath(), mcLoc("item/template_spawn_egg"));

        withExistingParent(TOAD_EGG_ITEM.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(BLANGONGA_EGG_ITEM.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(BLANGO.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(GIZUCHI_EGG_ITEM.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(IZUCHI_EGG_ITEM.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(ZINOGRE_EGG_ITEM.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(FLASHFLY_EGG_ITEM.getId().getPath(), mcLoc("item/template_spawn_egg"));

    }
}
