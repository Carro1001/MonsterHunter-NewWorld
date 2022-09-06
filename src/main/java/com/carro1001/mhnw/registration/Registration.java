package com.carro1001.mhnw.registration;

import com.carro1001.mhnw.utils.MHNWReferences;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class Registration {
    public static  void init(){

    }

    public static class Tags {
//        public static final TagKey<Block> AMETHYST_REPLACE = create("amethyst_replaceables");

        private static TagKey<Block> create(String location) {
            return BlockTags.create(new ResourceLocation(MHNWReferences.MODID, location));
        }
    }






}
