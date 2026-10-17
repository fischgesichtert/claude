package com.fischgesichtert.timemod.module;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Ein Werkzeug, in das an der Temporal-Werkbank Module eingebaut werden können. */
public interface ModularTool {
    /** Anzahl der Modulplätze. */
    int moduleSlots();

    /** Welche Modularten dieses Werkzeug versteht. */
    boolean accepts(ModuleItem.Kind kind);

    /** Kurze Werte-Übersicht mit den eingebauten Modulen (für Werkbank und Tooltip). */
    List<Component> moduleStats(ItemStack stack);
}
