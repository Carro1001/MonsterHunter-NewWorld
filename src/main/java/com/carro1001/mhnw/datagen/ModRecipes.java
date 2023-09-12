package com.carro1001.mhnw.datagen;

import com.carro1001.mhnw.registration.ModBlocks;
import com.carro1001.mhnw.registration.ModItems;
import com.carro1001.mhnw.utils.MHNWReferences;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;

import java.util.List;
import java.util.function.Consumer;

import static com.carro1001.mhnw.registration.ModBlocks.*;
import static com.carro1001.mhnw.registration.ModItems.*;
import static com.carro1001.mhnw.utils.MHNWReferences.MODID;

public class ModRecipes extends RecipeProvider implements IConditionBuilder {
    public ModRecipes(PackOutput pOutput) {
        super(pOutput);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> pWriter) {
        oreSmelting(pWriter, List.of(CARBALITE_ORE_ITEM.get(), RAW_CARBALITE_ITEM.get()), RecipeCategory.MISC, CARBALITE_ITEM.get(), 0.25f, 200, MHNWReferences.CARBALITE_ITEM);
        oreBlasting(pWriter, List.of(CARBALITE_ORE_ITEM.get(), RAW_CARBALITE_ITEM.get()), RecipeCategory.MISC, CARBALITE_ITEM.get(), 0.25f, 100, MHNWReferences.CARBALITE_ITEM);

        oreSmelting(pWriter, List.of(DRAGONITE_ORE_ITEM.get(), RAW_DRAGONITE_ITEM.get()), RecipeCategory.MISC, DRAGONITE_ITEM.get(), 0.25f, 200, MHNWReferences.DRAGONITE_ITEM);
        oreBlasting(pWriter, List.of(DRAGONITE_ORE_ITEM.get(), RAW_DRAGONITE_ITEM.get()), RecipeCategory.MISC, DRAGONITE_ITEM.get(), 0.25f, 100, MHNWReferences.DRAGONITE_ITEM);

        oreSmelting(pWriter, List.of(MACHALITE_ORE_ITEM.get(), RAW_MACHALITE_ITEM.get()), RecipeCategory.MISC, DRAGONITE_ITEM.get(), 0.25f, 200, MHNWReferences.DRAGONITE_ITEM);
        oreBlasting(pWriter, List.of(MACHALITE_ORE_ITEM.get(), RAW_MACHALITE_ITEM.get()), RecipeCategory.MISC, DRAGONITE_ITEM.get(), 0.25f, 100, MHNWReferences.DRAGONITE_ITEM);

    }

    protected static void oreSmelting(Consumer<FinishedRecipe> pFinishedRecipeConsumer, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult, float pExperience, int pCookingTIme, String pGroup) {
        oreCooking(pFinishedRecipeConsumer, RecipeSerializer.SMELTING_RECIPE, pIngredients, pCategory, pResult, pExperience, pCookingTIme, pGroup, "_from_smelting");
    }

    protected static void oreBlasting(Consumer<FinishedRecipe> pFinishedRecipeConsumer, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult, float pExperience, int pCookingTime, String pGroup) {
        oreCooking(pFinishedRecipeConsumer, RecipeSerializer.BLASTING_RECIPE, pIngredients, pCategory, pResult, pExperience, pCookingTime, pGroup, "_from_blasting");
    }

    protected static void oreCooking(Consumer<FinishedRecipe> pFinishedRecipeConsumer, RecipeSerializer<? extends AbstractCookingRecipe> pCookingSerializer, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult, float pExperience, int pCookingTime, String pGroup, String pRecipeName) {
        for(ItemLike itemlike : pIngredients) {
            SimpleCookingRecipeBuilder.generic(Ingredient.of(itemlike), pCategory, pResult,
                            pExperience, pCookingTime, pCookingSerializer)
                    .group(pGroup).unlockedBy(getHasName(itemlike), has(itemlike))
                    .save(pFinishedRecipeConsumer,  MODID + ":" + getItemName(pResult) + pRecipeName + "_" + getItemName(itemlike));
        }
    }


}
