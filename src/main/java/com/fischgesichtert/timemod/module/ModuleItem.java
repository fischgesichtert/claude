package com.fischgesichtert.timemod.module;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Ein Modul für Zeitwerkzeuge (ab Stufe III). Filtermodule legen fest, <em>was</em> ein Werkzeug
 * beeinflusst, Verstärker verändern Radius, Dauer, Stärke und Verbrauch, Dämpfer den Paradox-Druck.
 * Eingebaut wird an der Temporal-Werkbank.
 */
public class ModuleItem extends Item {
    public enum Kind {
        BLANK, FILTER, RADIUS, DURATION, STRENGTH, EFFICIENCY, DAMPING;

        public boolean isAmplifier() {
            return this != BLANK && this != FILTER;
        }
    }

    private final Kind kind;
    private final int mask;

    public ModuleItem(Kind kind, int mask, Properties properties) {
        super(properties.stacksTo(16));
        this.kind = kind;
        this.mask = mask;
    }

    public Kind kind() {
        return kind;
    }

    /** Nur für Filtermodule: die Kategorien aus {@link com.fischgesichtert.timemod.time.TimeFilter}. */
    public int mask() {
        return mask;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
        if (kind != Kind.BLANK) {
            tooltip.add(Component.translatable("tooltip.timemod.module.install").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
