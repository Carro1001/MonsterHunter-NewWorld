package com.carro1001.mhnw.datagen;

import com.carro1001.mhnw.setup.Registration;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.LanguageProvider;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class ModLanguageProvider extends LanguageProvider {

    public ModLanguageProvider(DataGenerator generator, String language) {
        super(generator, MODID,language);

    }

    @Override
    protected void addTranslations() {
        add(Registration.CARBALITE_ORE_BLOCK.get(),"Carbalite Ore" );
        add(Registration.MACHALITE_ORE_BLOCK.get(),"Machalite Ore" );
        add(Registration.DRAGONITE_ORE_BLOCK.get(),"Dragonite Ore" );
        add(Registration.ICE_CRYSTAL_CLUSTER_BLOCK.get(),"Ice Crystal Cluster" );
        add(Registration.EARTH_CRYSTAL_CLUSTER_BLOCK.get(),"Earth Crystal Cluster" );

        add(Registration.CARBALITE_ITEM.get(), "Carbalite" );
        add(Registration.DRAGONITE_ITEM.get(), "Dragonite" );
        add(Registration.MACHALITE_ITEM.get(), "Machalite" );
        add(Registration.RAW_CARBALITE_ITEM.get(), "Raw Carbalite" );
        add(Registration.RAW_DRAGONITE_ITEM.get(), "Raw Dragonite" );
        add(Registration.RAW_MACHALITE_ITEM.get(), "Raw Machalite" );
        add(Registration.EARTH_CRYSTAL_ITEM.get(), "Earth Crystal" );
        add(Registration.ICE_CRYSTAL_ITEM.get(), "Ice Crystal" );
        add(Registration.WELL_DONE_MEAT_ITEM.get(), "Well Done Steak" );
        add(Registration.MONSTER_FECES_ITEM.get(), "Monster Feces" );
        add(Registration.BUCKET_SLEEPTOAD_ITEM.get(), "Bucketed Sleeptoad" );
        add(Registration.BUCKET_PARATOAD_ITEM.get(), "Bucketed Paratoad" );
        add(Registration.BUCKET_POISONTOAD_ITEM.get(), "Bucketed Poisontoad" );
        add(Registration.BUCKET_NITROTOAD_ITEM.get(), "Bucketed Nitrotoad" );
        add(Registration.BONE_HEAD.get(), "Bone Helmet" );
        add(Registration.BONE_CHEST.get(), "Bone Chestplate" );
        add(Registration.BONE_LEGGINGS.get(), "Bone Leggings" );
        add(Registration.BONE_BOOTS.get(), "Bone Boots" );
        add("itemGroup.mhnw", "MHNW" );
        add(Registration.APTONOTH_EGG_ITEM.get(), "Aptonoth Spawn Egg");
        add(Registration.APTONOTH.get(), "Aptonoth");
        add(Registration.BITTERBUG_ITEM.get(), "Bitter Bug");
        add(Registration.BITTERBUG.get(), "Bitter Bug");
        add(Registration.RATHIAN_EGG_ITEM.get(), "Rathian Spawn Egg");
        add(Registration.RATHIAN.get(), "Rathian");
        add(Registration.RATHALOS_EGG_ITEM.get(), "Rathalos Spawn Egg");
        add(Registration.RATHALOS.get(), "Rathalos");

        add(Registration.TOAD_EGG_ITEM.get(), "Toads Spawn Egg");
        add(Registration.TOAD.get(), "Toad");
        add(Registration.BLANGO_EGG_ITEM.get(), "Blango Spawn Egg");
        add(Registration.BLANGO.get(), "Rathalos");
        add(Registration.BLANGONGA_EGG_ITEM.get(), "Blangonga Spawn Egg");
        add(Registration.BLANGONGA.get(), "Blangonga");
        add(Registration.GIZUCHI_EGG_ITEM.get(), "Great Izuchi Spawn Egg");
        add(Registration.GIZUCHI.get(), "Great Izuchi");
        add(Registration.IZUCHI_EGG_ITEM.get(), "Izuchi Spawn Egg");
        add(Registration.IZUCHI.get(), "Izuchi");
        add(Registration.ZAMITE_EGG_ITEM.get(), "Zamite Spawn Egg");
        add(Registration.ZAMITE.get(), "Zamite");
        add(Registration.ZINOGRE_EGG_ITEM.get(), "Zinogre Spawn Egg");
        add(Registration.ZINOGRE.get(), "Zinogre");
    }


}
