package com.carro1001.mhnw.datagen;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static com.carro1001.mhnw.registration.ModBlocks.*;
import static com.carro1001.mhnw.registration.ModItems.*;

public class ModRecipes extends RecipeProvider {

    public ModRecipes(DataGenerator generator) {
        super(generator);
    }

    @Override
    protected void buildCraftingRecipes(@NotNull Consumer<FinishedRecipe> p_176532_) {

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
}
