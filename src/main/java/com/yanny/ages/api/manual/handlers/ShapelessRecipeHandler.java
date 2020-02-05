package com.yanny.ages.api.manual.handlers;

import com.yanny.ages.api.Reference;
import com.yanny.ages.api.manual.IRecipeHandler;
import com.yanny.ages.api.manual.RecipeBackground;
import com.yanny.ages.api.manual.RecipeIngredient;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipe;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class ShapelessRecipeHandler implements IRecipeHandler {
    @Nonnull
    @Override
    public List<RecipeIngredient> getRecipeIngredients(IRecipe<?> recipe) {
        ShapelessRecipe shapelessRecipe = (ShapelessRecipe) recipe;
        List<RecipeIngredient> list = new ArrayList<>();

        for (int i = 0; i < shapelessRecipe.getIngredients().size(); i++) {
            list.add(new RecipeIngredient(shapelessRecipe.getIngredients().get(i), 3 + (i % 3) * 18, 4 + (i / 3) * 18));
        }

        list.add(new RecipeIngredient(Ingredient.fromStacks(shapelessRecipe.getRecipeOutput()), 97, 22));

        return list;
    }

    @Nonnull
    @Override
    public RecipeBackground getRecipeBackground() {
        return new RecipeBackground(new ResourceLocation(Reference.MODID, "textures/gui/manual/default_recipes.png"), 0, 61, 120,60, 256, 256);
    }
}
