package com.yanny.ages.api.manual;

import net.minecraft.item.crafting.Ingredient;

public class RecipeIngredient {
    final Ingredient item;
    final int x;
    final int y;

    public RecipeIngredient(Ingredient item, int x, int y) {
        this.item = item;
        this.x = x;
        this.y = y;
    }
}
