package com.fischgesichtert.timemod.armor;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.SmithingTemplateItem;

/** Schmiedevorlage „Chrono-Aufwertung“: Netherit-Rüstung + Chronit-Platte → Chrono-Rüstung. */
public final class ChronoUpgradeTemplate {
    private static final ResourceLocation EMPTY_HELMET = new ResourceLocation("item/empty_armor_slot_helmet");
    private static final ResourceLocation EMPTY_CHESTPLATE = new ResourceLocation("item/empty_armor_slot_chestplate");
    private static final ResourceLocation EMPTY_LEGGINGS = new ResourceLocation("item/empty_armor_slot_leggings");
    private static final ResourceLocation EMPTY_BOOTS = new ResourceLocation("item/empty_armor_slot_boots");
    private static final ResourceLocation EMPTY_INGOT = new ResourceLocation("item/empty_slot_ingot");

    private ChronoUpgradeTemplate() {
    }

    public static SmithingTemplateItem create() {
        return new SmithingTemplateItem(
            Component.translatable("item.timemod.chrono_upgrade_template.applies_to").withStyle(ChatFormatting.BLUE),
            Component.translatable("item.timemod.chrono_upgrade_template.ingredients").withStyle(ChatFormatting.BLUE),
            Component.translatable("item.timemod.chrono_upgrade_template.title").withStyle(ChatFormatting.GRAY),
            Component.translatable("item.timemod.chrono_upgrade_template.base_slot"),
            Component.translatable("item.timemod.chrono_upgrade_template.additions_slot"),
            List.of(EMPTY_HELMET, EMPTY_CHESTPLATE, EMPTY_LEGGINGS, EMPTY_BOOTS),
            List.of(EMPTY_INGOT));
    }
}
