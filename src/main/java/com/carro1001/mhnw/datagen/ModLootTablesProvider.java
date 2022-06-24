package com.carro1001.mhnw.datagen;


import com.carro1001.mhnw.setup.Registration;
import com.carro1001.mhnw.utils.MHNWReferences;
import net.minecraft.data.DataGenerator;

public class ModLootTablesProvider extends BaseLootTableProvider {
    public ModLootTablesProvider(DataGenerator generator) {
        super(generator);
    }

    @Override
    protected void addTables() {
        lootTables.put(Registration.CARBALITE_ORE_BLOCK.get(),createSilktoucheTable(MHNWReferences.CARBALITE_ORE, Registration.CARBALITE_ORE_BLOCK.get(), Registration.RAW_CARBALITE_ITEM.get(),1,3));
        lootTables.put(Registration.MACHALITE_ORE_BLOCK.get(),createSilktoucheTable(MHNWReferences.MACHALITE_ORE, Registration.MACHALITE_ORE_BLOCK.get(), Registration.RAW_MACHALITE_ITEM.get(),1,3));
        lootTables.put(Registration.DRAGONITE_ORE_BLOCK.get(),createSilktoucheTable(MHNWReferences.DRAGONITE_ORE, Registration.DRAGONITE_ORE_BLOCK.get(), Registration.RAW_DRAGONITE_ITEM.get(),1,3));
        lootTables.put(Registration.EARTH_CRYSTAL_CLUSTER_BLOCK.get(),createSilktoucheTable(MHNWReferences.EARTH_CRYSTAL_CLUSTER, Registration.EARTH_CRYSTAL_CLUSTER_BLOCK.get(), Registration.EARTH_CRYSTAL_ITEM.get(),1,3));
        lootTables.put(Registration.ICE_CRYSTAL_CLUSTER_BLOCK.get(),createSilktoucheTable(MHNWReferences.ICE_CRYSTAL_CLUSTER, Registration.ICE_CRYSTAL_CLUSTER_BLOCK.get(), Registration.ICE_CRYSTAL_ITEM.get(),1,3));

    }


}
