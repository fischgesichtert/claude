package com.fischgesichtert.timemod.rewind;

import com.fischgesichtert.timemod.item.RewindCapable;
import com.fischgesichtert.timemod.time.ServerTimeEngine;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Die Welt zeichnet automatisch auf, solange jemand ein Rückspul-Werkzeug bei sich trägt – sonst
 * gäbe es nichts zum Zurückspulen. Ohne solche Spieler endet die automatische Aufzeichnung.
 */
public final class RecordingDemand {
    private RecordingDemand() {
    }

    public static void init() {
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            if (level.getGameTime() % 20 == 0) {
                update(level);
            }
        });
    }

    private static void update(ServerLevel level) {
        ChronoJournal journal = ServerTimeEngine.of(level).journal();
        boolean wanted = false;
        for (ServerPlayer player : level.players()) {
            if (carriesRewindTool(player)) {
                wanted = true;
                break;
            }
        }
        journal.setAutoRecording(wanted);
    }

    private static boolean carriesRewindTool(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof RewindCapable) {
                return true;
            }
        }
        return player.getOffhandItem().getItem() instanceof RewindCapable;
    }
}
