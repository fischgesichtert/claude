package com.fischgesichtert.timemod.item;

import com.fischgesichtert.timemod.registry.ModItems;
import com.fischgesichtert.timemod.safety.Safety;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Wer die Welt zum ersten Mal betritt, bekommt die Chronik geschenkt (einmal pro Spieler). */
public final class ChronikGift {
    public static final String TAG = "timemod.got_chronik";

    private ChronikGift() {
    }

    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> Safety.run("Chronik verschenken", () -> give(handler.getPlayer())));
    }

    public static void give(ServerPlayer player) {
        if (player.getTags().contains(TAG)) {
            return;
        }
        player.addTag(TAG);
        ItemStack book = new ItemStack(ModItems.CHRONIK);
        if (!player.getInventory().add(book)) {
            player.drop(book, false);
        }
    }
}
