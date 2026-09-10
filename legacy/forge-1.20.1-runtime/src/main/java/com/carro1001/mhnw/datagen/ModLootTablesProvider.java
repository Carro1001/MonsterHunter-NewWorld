package com.carro1001.mhnw.datagen;

import com.carro1001.mhnw.datagen.loot.ModLootTables;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;

public class ModLootTablesProvider {
    public static LootTableProvider create(PackOutput output) {
        return new LootTableProvider(output, Set.of(), List.of(
                new LootTableProvider.SubProviderEntry(ModLootTables::new, LootContextParamSets.BLOCK)
        ));
    }
}
