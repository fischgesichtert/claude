package com.fischgesichtert.timemod.client;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.future.Echo;
import java.util.List;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;

/**
 * Beschreibungszeile für jedes TIME-Item mit Text „….desc“ – auch für schlichte Items wie das Herz der
 * Zeit oder Fossilien, deren Klasse selbst keinen Tooltip schreibt. Dazu der Hinweis auf Zeit-Echos.
 */
public final class ItemDescriptions {
    private ItemDescriptions() {
    }

    public static void init() {
        ItemTooltipCallback.EVENT.register((stack, context, lines) -> add(stack, lines));
    }

    private static void add(ItemStack stack, List<Component> lines) {
        if (Echo.is(stack)) {
            lines.add(Math.min(1, lines.size()), Component.translatable("tooltip.timemod.echo").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        }
        if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(TimeMod.MOD_ID)) {
            return;
        }
        String key = stack.getItem().getDescriptionId() + ".desc";
        if (!I18n.exists(key)) {
            return;
        }
        for (Component line : lines) {
            if (line.getContents() instanceof TranslatableContents contents && contents.getKey().equals(key)) {
                return;
            }
        }
        lines.add(Math.min(1, lines.size()), Component.translatable(key).withStyle(ChatFormatting.GRAY));
    }
}
