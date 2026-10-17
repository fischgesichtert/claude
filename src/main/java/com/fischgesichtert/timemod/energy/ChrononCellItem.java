package com.fischgesichtert.timemod.energy;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Zeitzelle: ein reiner Chronon-Speicher, aus dem Werkzeuge im Inventar nachladen. */
public class ChrononCellItem extends Item implements ChrononHolder {
    private final int capacity;

    public ChrononCellItem(int capacity, Properties properties) {
        super(properties);
        this.capacity = capacity;
    }

    @Override
    public int chrononCapacity(ItemStack stack) {
        return capacity;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * Chronon.stored(stack) / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb(0.52F, 0.8F, 1.0F);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.timemod.chronon", Chronon.format(Chronon.stored(stack)), Chronon.format(capacity)).withStyle(ChatFormatting.AQUA));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return Chronon.stored(stack) >= capacity;
    }
}
