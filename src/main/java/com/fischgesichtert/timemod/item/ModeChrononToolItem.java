package com.fischgesichtert.timemod.item;

import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Chronon-Werkzeug mit umschaltbarem Faktor (Schleichen + Rechtsklick). */
public abstract class ModeChrononToolItem extends ChrononToolItem {
    private static final String MODE = "Mode";

    protected ModeChrononToolItem(int capacity, Properties properties) {
        super(capacity, properties);
    }

    /** Die wählbaren Faktoren – Stärkemodule können weitere freischalten. */
    protected abstract double[] factors(ItemStack stack);

    public double factor(ItemStack stack) {
        double[] factors = factors(stack);
        int mode = stack.hasTag() ? stack.getTag().getInt(MODE) : 0;
        return factors[Math.floorMod(mode, factors.length)];
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            double[] factors = factors(stack);
            int mode = Math.floorMod((stack.hasTag() ? stack.getTag().getInt(MODE) : 0) + 1, factors.length);
            stack.getOrCreateTag().putInt(MODE, mode);
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.timemod.factor", format(factors[mode])).withStyle(ChatFormatting.AQUA), true);
            }
            return InteractionResultHolder.success(stack);
        }
        return activate(level, player, stack, factor(stack));
    }

    protected abstract InteractionResultHolder<ItemStack> activate(Level level, Player player, ItemStack stack, double factor);

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("message.timemod.factor", format(factor(stack))).withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.timemod.mode_hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    public static String format(double factor) {
        return factor == Math.rint(factor) ? (long) factor + "×" : String.format(Locale.ROOT, "%.2f×", factor);
    }
}
