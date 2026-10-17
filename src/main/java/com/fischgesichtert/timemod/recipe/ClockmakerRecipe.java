package com.fischgesichtert.timemod.recipe;

import com.fischgesichtert.timemod.energy.Chronon;
import com.fischgesichtert.timemod.energy.ChrononHolder;
import com.fischgesichtert.timemod.registry.ModRecipes;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;

/**
 * Präzisionsrezept der Uhrmacher-Werkbank. Gleiches Format wie ein geformtes Werkbank-Rezept,
 * aber nur an der Uhrmacher-Werkbank herstellbar.
 */
public class ClockmakerRecipe extends ShapedRecipe {
    public ClockmakerRecipe(ResourceLocation id, String group, CraftingBookCategory category, int width, int height,
                            NonNullList<Ingredient> ingredients, ItemStack result, boolean notify) {
        super(id, group, category, width, height, ingredients, result, notify);
    }

    static ClockmakerRecipe of(ShapedRecipe shaped) {
        return new ClockmakerRecipe(shaped.getId(), shaped.getGroup(), shaped.category(), shaped.getWidth(), shaped.getHeight(),
            shaped.getIngredients(), shaped.getResultItem(RegistryAccess.EMPTY), shaped.showNotification());
    }

    /** Chronon der Zutaten (Zellen, Werkzeuge) geht ins Ergebnis über, soweit es dort Platz hat. */
    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess access) {
        ItemStack result = super.assemble(container, access);
        if (result.getItem() instanceof ChrononHolder) {
            long sum = 0;
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack ingredient = container.getItem(i);
                if (ingredient.getItem() instanceof ChrononHolder) {
                    sum += Chronon.stored(ingredient);
                }
            }
            if (sum > 0) {
                Chronon.insert(result, (int) Math.min(Integer.MAX_VALUE, sum));
            }
        }
        return result;
    }

    /** Nicht im Rezeptbuch der normalen Werkbank anzeigen (es ist ja ein Werkbank-Rezept im Herzen). */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.CLOCKMAKER;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CLOCKMAKER_SERIALIZER;
    }

    public static class Serializer implements RecipeSerializer<ClockmakerRecipe> {
        @Override
        public ClockmakerRecipe fromJson(ResourceLocation id, JsonObject json) {
            return of(RecipeSerializer.SHAPED_RECIPE.fromJson(id, json));
        }

        @Override
        public ClockmakerRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return of(RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buf));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, ClockmakerRecipe recipe) {
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buf, recipe);
        }
    }
}
