package com.fischgesichtert.timemod.era;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.registry.ModItems;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper;

/**
 * Was die Jahre mit Dingen machen: Essen verrottet, Eisen rostet, Kupfer läuft grün an, Holz, Wolle und
 * Papier zerfallen, Werkzeuge nutzen sich ab – und Diamanten bleiben Diamanten.
 */
public final class Aging {
    /** Übersteht jede Zeit: Edelsteine, Gold, Netherit, Zeit-Artefakte (per Datenpaket erweiterbar). */
    public static final TagKey<Item> TIMELESS = TagKey.create(Registries.ITEM, TimeMod.id("timeless"));
    /** Vergeht schnell: Holz, Wolle, Papier, Leder, Pflanzen (per Datenpaket erweiterbar). */
    public static final TagKey<Item> ORGANIC = TagKey.create(Registries.ITEM, TimeMod.id("organic"));

    private Aging() {
    }

    /** Der Gegenstand nach {@code years} Jahren; leer, wenn nichts davon übrig ist. */
    public static ItemStack age(ItemStack stack, long years) {
        if (stack.isEmpty() || years <= 0 || stack.is(TIMELESS)) {
            return stack.copy();
        }
        Item item = stack.getItem();
        String path = BuiltInRegistries.ITEM.getKey(item).getPath();
        if (item.isEdible()) {
            // Honig verdirbt nie – alles andere schon
            if (stack.is(Items.HONEY_BOTTLE) && years < 10_000) {
                return stack.copy();
            }
            return years >= 1_000 ? ItemStack.EMPTY : new ItemStack(Items.ROTTEN_FLESH, stack.getCount());
        }
        if (isOrganic(stack)) {
            if (years >= 50) {
                return ItemStack.EMPTY;
            }
            return years >= 10 ? withCount(stack, (stack.getCount() + 1) / 2) : wear(stack, years);
        }
        if (path.contains("copper") && item instanceof BlockItem blockItem) {
            return oxidized(stack, blockItem.getBlock(), years);
        }
        if (path.contains("iron")) {
            return rusted(stack, years);
        }
        if (isMineral(item)) {
            return stack.copy();
        }
        if (stack.isDamageableItem()) {
            return wear(stack, years);
        }
        return years >= 10_000 ? ItemStack.EMPTY : stack.copy();
    }

    private static boolean isOrganic(ItemStack stack) {
        return stack.is(ORGANIC) || stack.is(ItemTags.WOOL) || stack.is(ItemTags.WOOL_CARPETS) || stack.is(ItemTags.LOGS)
            || stack.is(ItemTags.PLANKS) || stack.is(ItemTags.SAPLINGS) || stack.is(ItemTags.LEAVES) || stack.is(ItemTags.FLOWERS)
            || stack.is(ItemTags.BOATS) || stack.is(ItemTags.BEDS) || stack.is(ItemTags.BANNERS) || stack.is(ItemTags.BOOKSHELF_BOOKS)
            || stack.is(ItemTags.WOODEN_DOORS) || stack.is(ItemTags.WOODEN_SLABS) || stack.is(ItemTags.WOODEN_STAIRS);
    }

    /** Stein, Ziegel, Erze und Glas: Mineralien überdauern. */
    private static boolean isMineral(Item item) {
        if (!(item instanceof BlockItem blockItem)) {
            return item == Items.FLINT || item == Items.COAL || item == Items.LAPIS_LAZULI || item == Items.REDSTONE || item == Items.BONE;
        }
        var state = blockItem.getBlock().defaultBlockState();
        return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.STONE_BRICKS) || state.is(BlockTags.STONE_ORE_REPLACEABLES)
            || state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES) || (state.is(BlockTags.MINEABLE_WITH_PICKAXE) && !state.hasBlockEntity());
    }

    /** Eisen rostet: Werkzeuge nutzen sich ab, Barren werden zu Rostklumpen, nach 100.000 Jahren ist alles zerfallen. */
    private static ItemStack rusted(ItemStack stack, long years) {
        if (years >= 100_000) {
            return ItemStack.EMPTY;
        }
        if (stack.isDamageableItem()) {
            return wear(stack, years * 4);
        }
        if (years < 5) {
            return stack.copy();
        }
        int count = years < 100 ? stack.getCount() : years < 1_000 ? (stack.getCount() + 1) / 2 : (stack.getCount() + 3) / 4;
        return new ItemStack(ModItems.RUSTY_IRON, count);
    }

    /** Kupfer läuft an: je älter, desto grüner (bis zur vollen Patina nach zehn Jahren). */
    private static ItemStack oxidized(ItemStack stack, Block block, long years) {
        int steps = years >= 10 ? 3 : years >= 3 ? 2 : 1;
        Block current = block;
        for (int i = 0; i < steps; i++) {
            Optional<Block> next = WeatheringCopper.getNext(current);
            if (next.isEmpty()) {
                break;
            }
            current = next.get();
        }
        ItemStack aged = new ItemStack(current.asItem(), stack.getCount());
        return aged.isEmpty() ? stack.copy() : aged;
    }

    /** Abnutzung: wächst logarithmisch mit den Jahren; ganz Abgenutztes zerfällt. */
    private static ItemStack wear(ItemStack stack, long years) {
        if (!stack.isDamageableItem()) {
            return stack.copy();
        }
        double fraction = Math.min(1.0, Math.log10(1 + years) / 3.0);
        int damage = stack.getDamageValue() + (int) Math.round(fraction * stack.getMaxDamage());
        if (damage >= stack.getMaxDamage()) {
            return ItemStack.EMPTY;
        }
        ItemStack aged = stack.copy();
        aged.setDamageValue(damage);
        return aged;
    }

    private static ItemStack withCount(ItemStack stack, int count) {
        ItemStack copy = stack.copy();
        copy.setCount(count);
        return copy;
    }
}
