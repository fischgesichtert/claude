package com.fischgesichtert.timemod.registry;

import com.fischgesichtert.timemod.TimeMod;
import net.minecraft.advancements.Advancement;
import net.minecraft.server.level.ServerPlayer;

/** Erfolge, die der Code vergibt (Kriterium „done“, Auslöser „impossible“ im Datenpaket). */
public final class ModAdvancements {
    private ModAdvancements() {
    }

    public static void award(ServerPlayer player, String id) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(TimeMod.id(id));
        if (advancement != null) {
            player.getAdvancements().award(advancement, "done");
        }
    }
}
