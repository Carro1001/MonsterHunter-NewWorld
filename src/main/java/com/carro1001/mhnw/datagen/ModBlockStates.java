package com.carro1001.mhnw.datagen;

import com.carro1001.mhnw.setup.Registration;
import net.minecraft.core.Direction;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.function.BiConsumer;

import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class ModBlockStates extends BlockStateProvider {
    public ModBlockStates(DataGenerator generator, ExistingFileHelper existingFileHelper) {
        super(generator, MODID,existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        simpleBlock(Registration.MACHALITE_ORE_BLOCK.get());
        simpleBlock(Registration.CARBALITE_ORE_BLOCK.get());
        simpleBlock(Registration.DRAGONITE_ORE_BLOCK.get());
        simpleBlock(Registration.EARTH_CRYSTAL_CLUSTER_BLOCK.get());
        simpleBlock(Registration.ICE_CRYSTAL_CLUSTER_BLOCK.get());

    }
    private BiConsumer<Direction, ModelBuilder<BlockModelBuilder>.ElementBuilder.FaceBuilder> addTexture(String texture) {
        return ($, f) -> f.texture(texture).uvs(0,0,16,16).tintindex(0);
    }
}
