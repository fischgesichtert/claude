package com.fischgesichtert.timemod.registry;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.recipe.ClockmakerRecipe;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public final class ModRecipes {
    public static final RecipeType<ClockmakerRecipe> CLOCKMAKER = Registry.register(BuiltInRegistries.RECIPE_TYPE, TimeMod.id("clockmaker"),
        new RecipeType<>() {
            @Override
            public String toString() {
                return "timemod:clockmaker";
            }
        });
    public static final RecipeSerializer<ClockmakerRecipe> CLOCKMAKER_SERIALIZER = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
        TimeMod.id("clockmaker"), new ClockmakerRecipe.Serializer());

    private ModRecipes() {
    }

    public static void init() {
    }
}
