package com.carro1001.mhnw.datagen;

import net.minecraft.core.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.Nullable;
import java.util.function.Consumer;

import static com.carro1001.mhnw.registration.ModBlocks.*;
import static com.carro1001.mhnw.registration.ModItems.*;

public class ModRecipes extends RecipeProvider {

    public ModRecipes(DataGenerator generator) {
        super(generator);
    }

    @Override
    protected void buildCraftingRecipes(Consumer<FinishedRecipe> p_176532_) {

        SimpleCookingRecipeBuilder.smelting(Ingredient.of(CARBALITE_ORE_ITEM.get()),
                CARBALITE_ITEM.get(),1f,100)
                .unlockedBy("has_ore", has(CARBALITE_ORE_ITEM.get()))
                .save(p_176532_, "carbalite");
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(DRAGONITE_ORE_ITEM.get()),
                        DRAGONITE_ITEM.get(),1f,100)
                .unlockedBy("has_ore", has(DRAGONITE_ORE_ITEM.get()))
                .save(p_176532_, "dragonite");
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(MACHALITE_ORE_ITEM.get()),
                        MACHALITE_ITEM.get(),1f,100)
                .unlockedBy("has_ore", has(MACHALITE_ORE_ITEM.get()))
                .save(p_176532_, "machalite");
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(RAW_CARBALITE_ITEM.get()),
                        CARBALITE_ITEM.get(),1f,100)
                .unlockedBy("has_raw", has(RAW_CARBALITE_ITEM.get()))
                .save(p_176532_, "carbalite1");
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(RAW_DRAGONITE_ITEM.get()),
                        DRAGONITE_ITEM.get(),1f,100)
                .unlockedBy("has_raw", has(RAW_DRAGONITE_ITEM.get()))
                .save(p_176532_, "dragonite1");
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(RAW_MACHALITE_ITEM.get()),
                        MACHALITE_ITEM.get(),1f,100)
                .unlockedBy("has_raw", has(RAW_MACHALITE_ITEM.get()))
                .save(p_176532_, "machalite1");
        SimpleCookingRecipeBuilder.campfireCooking(Ingredient.of(RAW_MEAT_ITEM.get()),
                        RARE_MEAT_ITEM.get(),1f,100)
                .unlockedBy("has_item", has(RAW_MEAT_ITEM.get()))
                .save(p_176532_, "raw_monster_meat");
        SimpleCookingRecipeBuilder.campfireCooking(Ingredient.of(RARE_MEAT_ITEM.get()),
                        WELL_DONE_MEAT_ITEM.get(),1f,50)
                .unlockedBy("has_item", has(RARE_MEAT_ITEM.get()))
                .save(p_176532_, "rare_monster_meat");

    }

    protected static RecipeBuilder stairBuilder(ItemLike p_176711_, Ingredient p_176712_) {
        return ShapedRecipeBuilder.shaped(p_176711_, 4).define('#', p_176712_).pattern("#  ").pattern("## ").pattern("###");
    }

    protected static RecipeBuilder slabBuilder(ItemLike p_176705_, Ingredient p_176706_) {
        return ShapedRecipeBuilder.shaped(p_176705_, 6).define('#', p_176706_).pattern("###");
    }
    protected static String getSimpleRecipeName(ItemLike p_176645_) {
        return getItemName(p_176645_);
    }

    protected static void nineBlockStorageRecipes(Consumer<FinishedRecipe> p_176744_, ItemLike p_176745_, ItemLike p_176746_) {
        nineBlockStorageRecipes(p_176744_, p_176745_, p_176746_, getSimpleRecipeName(p_176746_), (String)null, getSimpleRecipeName(p_176745_), (String)null);
    }

    protected static String getItemName(ItemLike p_176633_) {
        return Registry.ITEM.getKey(p_176633_.asItem()).getPath();
    }

    protected static String getHasName(ItemLike p_176603_) {
        return "has_" + getItemName(p_176603_);
    }

    protected static void nineBlockStorageRecipes(Consumer<FinishedRecipe> p_176569_, ItemLike p_176570_, ItemLike p_176571_, String p_176572_, @Nullable String p_176573_, String p_176574_, @Nullable String p_176575_) {
        ShapelessRecipeBuilder.shapeless(p_176570_, 9).requires(p_176571_).group(p_176575_).unlockedBy(getHasName(p_176571_), has(p_176571_)).save(p_176569_, new ResourceLocation(p_176574_));
        ShapedRecipeBuilder.shaped(p_176571_).define('#', p_176570_).pattern("###").pattern("###").pattern("###").group(p_176573_).unlockedBy(getHasName(p_176570_), has(p_176570_)).save(p_176569_, new ResourceLocation(p_176572_));
    }
    private static void shapeless1x1Recipes(Consumer<FinishedRecipe> p_176569_, ItemLike p_176570_, ItemLike p_176571_) {
        ShapelessRecipeBuilder.shapeless(p_176570_, 1).requires(p_176571_).group((String)null).unlockedBy(getHasName(p_176571_), has(p_176571_)).save(p_176569_, new ResourceLocation(getSimpleRecipeName(p_176570_)));
    }
    protected static void woodenBoat(Consumer<FinishedRecipe> p_126022_, ItemLike p_126023_, ItemLike p_126024_) {
        ShapedRecipeBuilder.shaped(p_126023_).define('#', p_126024_).pattern("# #").pattern("###").group("boat").unlockedBy("in_water", insideOf(Blocks.WATER)).save(p_126022_);
    }


}
