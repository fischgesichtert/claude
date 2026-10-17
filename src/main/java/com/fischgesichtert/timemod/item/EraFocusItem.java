package com.fischgesichtert.timemod.item;

import com.fischgesichtert.timemod.era.Era;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Ein Epochen-Fokus richtet die Konsole des Chronos-Tors auf ferne Zeitalter aus. Ohne Fokus reicht
 * sie nur bis ins Frühmittelalter und bis +1000 Jahre.
 */
public class EraFocusItem extends Item {
    private final Set<Era.Age> unlocks;

    public EraFocusItem(Properties properties, Set<Era.Age> unlocks) {
        super(properties);
        this.unlocks = EnumSet.copyOf(unlocks);
    }

    /** Zeitalter, die jede Konsole auch ohne Fokus erreicht. */
    public static boolean needsFocus(Era.Age age) {
        return age != Era.Age.FUTURE && age != Era.Age.PRESENT && age != Era.Age.HISTORY;
    }

    /** Ob die Konsole mit diesem (womöglich leeren) Fokus-Platz das Zeitalter erreicht. */
    public static boolean allows(ItemStack focus, Era.Age age) {
        return !needsFocus(age) || (focus.getItem() instanceof EraFocusItem item && item.unlocks.contains(age));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        for (Era.Age age : unlocks) {
            tooltip.add(Component.translatable("tooltip.timemod.focus.unlocks",
                Component.translatable("era.timemod.age." + age.name().toLowerCase(java.util.Locale.ROOT))).withStyle(ChatFormatting.AQUA));
        }
    }
}
