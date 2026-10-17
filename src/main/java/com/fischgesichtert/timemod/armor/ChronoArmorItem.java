package com.fischgesichtert.timemod.armor;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Chrono-Rüstung. Jedes Teil spart 5 % Chronon. Das volle Set lässt dich nach JoJo-Regel in der
 * angehaltenen Zeit anderer weiterbewegen; der Helm zeigt alle Zeitzonen.
 */
public class ChronoArmorItem extends ArmorItem {
    public static final double SAVING_PER_PIECE = 0.05;

    public ChronoArmorItem(Type type, Properties properties) {
        super(ChronoArmorMaterial.INSTANCE, type, properties.fireResistant());
    }

    /** Wie viele Chrono-Teile trägt der Spieler? */
    public static int pieces(Player player) {
        int count = 0;
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (player.getItemBySlot(slot).getItem() instanceof ChronoArmorItem) {
                count++;
            }
        }
        return count;
    }

    public static boolean hasFullSet(Player player) {
        return pieces(player) == 4;
    }

    public static boolean wearsHelmet(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof ChronoArmorItem;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.timemod.chrono_armor.piece").withStyle(ChatFormatting.GRAY));
        if (getType() == Type.HELMET) {
            tooltip.add(Component.translatable("tooltip.timemod.chrono_armor.helmet").withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.timemod.chrono_armor.set").withStyle(ChatFormatting.DARK_AQUA));
    }
}
