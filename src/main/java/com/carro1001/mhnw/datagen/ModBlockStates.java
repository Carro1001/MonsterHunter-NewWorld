package com.carro1001.mhnw.datagen;

import net.minecraft.core.Direction;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.function.BiConsumer;

import static com.carro1001.mhnw.registration.ModBlocks.*;
import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class ModBlockStates {
    /*public ModBlockStates(DataGenerator generator, ExistingFileHelper existingFileHelper) {
        super(generator, MODID,existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        simpleBlock(MACHALITE_ORE_BLOCK.get());
        simpleBlock(CARBALITE_ORE_BLOCK.get());
        simpleBlock(DRAGONITE_ORE_BLOCK.get());
        simpleBlock(EARTH_CRYSTAL_CLUSTER_BLOCK.get(),models().cross(EARTH_CRYSTAL_CLUSTER_BLOCK.getId().getPath(), modLoc("block/" + EARTH_CRYSTAL_CLUSTER_BLOCK.getId().getPath())).renderType("cutout"));
        simpleBlock(ICE_CRYSTAL_CLUSTER_BLOCK.get(),models().cross(ICE_CRYSTAL_CLUSTER_BLOCK.getId().getPath(), modLoc("block/" + ICE_CRYSTAL_CLUSTER_BLOCK.getId().getPath())).renderType("cutout"));

    }

    private BiConsumer<Direction, ModelBuilder<BlockModelBuilder>.ElementBuilder.FaceBuilder> addTexture(String texture) {
        return ($, f) -> f.texture(texture).uvs(0,0,16,16).tintindex(0);
    }*/
}
