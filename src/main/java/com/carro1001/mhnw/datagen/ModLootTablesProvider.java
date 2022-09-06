package com.carro1001.mhnw.datagen;


import com.carro1001.mhnw.utils.MHNWReferences;
import net.minecraft.data.DataGenerator;

import static com.carro1001.mhnw.registration.ModBlocks.*;
import static com.carro1001.mhnw.registration.ModItems.*;

public class ModLootTablesProvider extends BaseLootTableProvider {
    public ModLootTablesProvider(DataGenerator generator) {
        super(generator);
    }

    @Override
    protected void addTables() {
        lootTables.put(CARBALITE_ORE_BLOCK.get(),createSilktoucheTable(MHNWReferences.CARBALITE_ORE, CARBALITE_ORE_BLOCK.get(), RAW_CARBALITE_ITEM.get(),1,3));
        lootTables.put(MACHALITE_ORE_BLOCK.get(),createSilktoucheTable(MHNWReferences.MACHALITE_ORE, MACHALITE_ORE_BLOCK.get(), RAW_MACHALITE_ITEM.get(),1,3));
        lootTables.put(DRAGONITE_ORE_BLOCK.get(),createSilktoucheTable(MHNWReferences.DRAGONITE_ORE, DRAGONITE_ORE_BLOCK.get(), RAW_DRAGONITE_ITEM.get(),1,3));
        lootTables.put(EARTH_CRYSTAL_CLUSTER_BLOCK.get(),createSilktoucheTable(MHNWReferences.EARTH_CRYSTAL_CLUSTER, EARTH_CRYSTAL_CLUSTER_BLOCK.get(), EARTH_CRYSTAL_ITEM.get(),1,3));
        lootTables.put(ICE_CRYSTAL_CLUSTER_BLOCK.get(),createSilktoucheTable(MHNWReferences.ICE_CRYSTAL_CLUSTER, ICE_CRYSTAL_CLUSTER_BLOCK.get(), ICE_CRYSTAL_ITEM.get(),1,3));

    }


}
