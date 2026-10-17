package com.fischgesichtert.timemod.item;

import com.fischgesichtert.timemod.energy.Chronon;
import com.fischgesichtert.timemod.energy.ChrononHolder;
import com.fischgesichtert.timemod.module.ModularTool;
import com.fischgesichtert.timemod.module.ModuleItem;
import com.fischgesichtert.timemod.module.Modules;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Werkzeuge ab Stufe II: laufen mit Chronon (eigener Speicher plus Zeitzellen im Inventar). */
public abstract class ChrononToolItem extends Item implements ChrononHolder {
    private final int capacity;

    protected ChrononToolItem(int capacity, Properties properties) {
        super(properties.stacksTo(1));
        this.capacity = capacity;
    }

    @Override
    public int chrononCapacity(ItemStack stack) {
        return capacity;
    }

    protected static boolean pay(Player player, ItemStack stack, int cost) {
        if (Chronon.pay(player, stack, cost)) {
            return true;
        }
        player.displayClientMessage(Component.translatable("message.timemod.no_chronon", cost).withStyle(ChatFormatting.AQUA), true);
        return false;
    }

    /** Laden und Bezahlen ändern die Item-Daten; die Hand soll dabei nicht neu ausholen. */
    @Override
    public boolean allowNbtUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
        return false;
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
        return Mth.hsvToRgb(0.52F, 0.85F, 1.0F);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.timemod.chronon", Chronon.format(Chronon.stored(stack)), Chronon.format(capacity)).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
        if (this instanceof ModularTool modular) {
            List<ModuleItem> installed = Modules.installed(stack);
            if (!installed.isEmpty()) {
                tooltip.add(Component.translatable("tooltip.timemod.modules").withStyle(ChatFormatting.DARK_AQUA));
                for (ModuleItem module : installed) {
                    tooltip.add(Component.literal(" • ").append(module.getDescription()).withStyle(ChatFormatting.GRAY));
                }
            }
            for (Component line : modular.moduleStats(stack)) {
                tooltip.add(line.copy().withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }
}
