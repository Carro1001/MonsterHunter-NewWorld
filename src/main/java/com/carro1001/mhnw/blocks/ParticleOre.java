package com.carro1001.mhnw.blocks;

import com.carro1001.mhnw.setup.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Random;

public class ParticleOre extends Block {
    private static final VoxelShape SHAPE = box(6, 6, 6, 10, 10, 10);

    //JUST HERE TO RENDER PARTICLES FOR TESTING
    public ParticleOre(Properties p_49795_) {
        super(p_49795_);
    }
    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public boolean canBeReplaced(BlockState pState, BlockPlaceContext pUseContext) {
        return true;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return true;
    }

    @Override
    public void animateTick(BlockState pState, Level pLevel, BlockPos pos, Random pRandom) {
        super.animateTick(pState, pLevel, pos, pRandom);
        double d0 = (double) pos.getX() + 0.5D;
        double d1 = (double) pos.getY() + 0.5D;
        double d2 = (double) pos.getZ() + 0.5D;
        pLevel.addParticle(Registration.SLEEP_PARTICLE_TYPE.get(), d0, d1, d2, 0.0D, 0.0D, 0.0D);
    }
}
