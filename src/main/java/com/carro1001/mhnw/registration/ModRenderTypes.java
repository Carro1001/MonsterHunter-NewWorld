package com.carro1001.mhnw.registration;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ModRenderTypes {

    private ModRenderTypes() {}

    public static void init() {
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.EARTH_CRYSTAL_CLUSTER_BLOCK.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.ICE_CRYSTAL_CLUSTER_BLOCK.get(), RenderType.cutout());
    }


}
