package com.carro1001.mhnw.datagen.loot;


import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraftforge.registries.RegistryObject;

import java.util.Set;

import static com.carro1001.mhnw.registration.ModBlocks.*;
import static com.carro1001.mhnw.registration.ModItems.*;

public class ModLootTables extends BlockLootSubProvider {

    public ModLootTables() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected void generate() {


        this.add(EARTH_CRYSTAL_CLUSTER_BLOCK.get(),
                block -> createCopperLikeOreDrops(EARTH_CRYSTAL_CLUSTER_BLOCK.get(), EARTH_CRYSTAL_ITEM.get()));
        this.add(ICE_CRYSTAL_CLUSTER_BLOCK.get(),
                block -> createCopperLikeOreDrops(ICE_CRYSTAL_CLUSTER_BLOCK.get(), ICE_CRYSTAL_ITEM.get()));
        this.add(CARBALITE_ORE_BLOCK.get(),
                block -> createCopperLikeOreDrops(CARBALITE_ORE_BLOCK.get(), RAW_CARBALITE_ITEM.get()));
        this.add(DRAGONITE_ORE_BLOCK.get(),
                block -> createCopperLikeOreDrops(DRAGONITE_ORE_BLOCK.get(), RAW_DRAGONITE_ITEM.get()));
        this.add(MACHALITE_ORE_BLOCK.get(),
                block -> createCopperLikeOreDrops(MACHALITE_ORE_BLOCK.get(), RAW_MACHALITE_ITEM.get()));

    }

    protected LootTable.Builder createCopperLikeOreDrops(Block pBlock, Item item) {
        return createSilkTouchDispatchTable(pBlock,
                this.applyExplosionDecay(pBlock,
                        LootItem.lootTableItem(item)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F)))
                                .apply(ApplyBonusCount.addOreBonusCount(Enchantments.BLOCK_FORTUNE))));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return BLOCKS.getEntries().stream().map(RegistryObject::get)::iterator;
    }



}
