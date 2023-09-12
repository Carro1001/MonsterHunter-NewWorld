package com.carro1001.mhnw.datagen;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraftforge.common.data.LanguageProvider;

import static com.carro1001.mhnw.registration.ModBlocks.*;
import static com.carro1001.mhnw.registration.ModEntities.*;
import static com.carro1001.mhnw.registration.ModItems.*;
import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class ModLanguageProvider extends LanguageProvider {

    public ModLanguageProvider(PackOutput output, String language) {
        super(output, MODID,language);

    }

    @Override
    protected void addTranslations() {
        add(CARBALITE_ORE_BLOCK.get(),"Carbalite Ore" );
        add(MACHALITE_ORE_BLOCK.get(),"Machalite Ore" );
        add(DRAGONITE_ORE_BLOCK.get(),"Dragonite Ore" );
        add(ICE_CRYSTAL_CLUSTER_BLOCK.get(),"Ice Crystal Cluster" );
        add(EARTH_CRYSTAL_CLUSTER_BLOCK.get(),"Earth Crystal Cluster" );

        add(CARBALITE_ITEM.get(), "Carbalite" );
        add(DRAGONITE_ITEM.get(), "Dragonite" );
        add(MACHALITE_ITEM.get(), "Machalite" );
        add(RAW_CARBALITE_ITEM.get(), "Raw Carbalite" );
        add(RAW_DRAGONITE_ITEM.get(), "Raw Dragonite" );
        add(RAW_MACHALITE_ITEM.get(), "Raw Machalite" );
        add(EARTH_CRYSTAL_ITEM.get(), "Earth Crystal" );
        add(ICE_CRYSTAL_ITEM.get(), "Ice Crystal" );
        add(RAW_MEAT_ITEM.get(), "Raw Steak" );
        add(RARE_MEAT_ITEM.get(), "Rare Steak" );
        add(WELL_DONE_MEAT_ITEM.get(), "Well Done Steak" );
        add(RATHALOS_PLATE_ITEM.get(), "Rathalos Plate" );
        add(RATHALOS_SCALE_ITEM.get(), "Rathalos Scale" );
        add(RATHALOS_TAIL_ITEM.get(), "Rathalos Tail" );
        add(RATHALOS_WEBBING_ITEM.get(), "Rathalos Webbing" );
        add(RATHIAN_PLATE_ITEM.get(), "Rathian Plate" );
        add(RATHIAN_SCALE_ITEM.get(), "Rathian Scale" );
        add(RATHIAN_TAIL_ITEM.get(), "Rathian Tail" );
        add(RATHIAN_WEBBING_ITEM.get(), "Rathian Webbing" );
        add(FREEZER_SACK_ITEM.get(), "Freezer Sack" );
        add(FLAME_SACK_ITEM.get(), "Flame Sack" );
        add(SCREAMER_SACK_ITEM.get(), "Screamer Sack" );
        add(SLEEP_SACK_ITEM.get(), "Sleep Sack" );
        add(MONSTER_FECES_ITEM.get(), "Monster Feces" );
        add(BUCKET_SLEEPTOAD_ITEM.get(), "Sleeptoad Bucket" );
        add(BUCKET_PARATOAD_ITEM.get(), "Paratoad Bucket" );
        add(BUCKET_POISONTOAD_ITEM.get(), "Poisontoad Bucket" );
        add(BUCKET_NITROTOAD_ITEM.get(), "Nitrotoad Bucket" );
        add(BONE_HEAD.get(), "Bone Helmet" );
        add(BONE_CHEST.get(), "Bone Chestplate" );
        add(BONE_LEGGINGS.get(), "Bone Leggings" );
        add(BONE_BOOTS.get(), "Bone Boots" );
        add("itemGroup.mhnw", "MHNW" );
        add(APTONOTH_EGG_ITEM.get(), "Aptonoth Spawn Egg");
        add(APTONOTH.get(), "Aptonoth");
        add(BITTERBUG_ITEM.get(), "Bitter Bug");
        add(BITTERBUG.get(), "Bitter Bug");
        add(RATHIAN_EGG_ITEM.get(), "Rathian Spawn Egg");
        add(RATHIAN.get(), "Rathian");
        add(RATHALOS_EGG_ITEM.get(), "Rathalos Spawn Egg");
        add(RATHALOS.get(), "Rathalos");

        add(FLASHBUG.get(), "Flash Bug");
        add(FLASHFLY_EGG_ITEM.get(), "Flash Bug Spawn Egg");
        add(TOAD_EGG_ITEM.get(), "Toads Spawn Egg");
        add(TOAD.get(), "Toad");
        add(BLANGO_EGG_ITEM.get(), "Blango Spawn Egg");
        add(BLANGO.get(), "Rathalos");
        add(BLANGONGA_EGG_ITEM.get(), "Blangonga Spawn Egg");
        add(BLANGONGA.get(), "Blangonga");
        add(GIZUCHI_EGG_ITEM.get(), "Great Izuchi Spawn Egg");
        add(GIZUCHI.get(), "Great Izuchi");
        add(IZUCHI_EGG_ITEM.get(), "Izuchi Spawn Egg");
        add(IZUCHI.get(), "Izuchi");
        add(ZINOGRE_EGG_ITEM.get(), "Zinogre Spawn Egg");
        add(ZINOGRE.get(), "Zinogre");
    }

}
