package com.fischgesichtert.timemod.network;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.chronos.ChronosSettings;
import com.fischgesichtert.timemod.chronos.ChronosWatchItem;
import com.fischgesichtert.timemod.registry.ModSounds;
import com.fischgesichtert.timemod.rewind.ChronoJournal;
import com.fischgesichtert.timemod.rewind.RewindSession;
import com.fischgesichtert.timemod.safety.Safety;
import com.fischgesichtert.timemod.time.ServerTimeEngine;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/**
 * Chronos-Uhr übers Netz. Der Client schickt Aktionen aus Radialmenü und Mausrad; der Server prüft
 * sie und speichert sie in der Uhr. Umgekehrt meldet der Server allen, die die Uhr halten, den
 * Stand des Zeitjournals für die Zeitleiste im HUD.
 */
public final class ChronosNetworking {
    public static final ResourceLocation ACTION = TimeMod.id("chronos_action");
    public static final ResourceLocation STATE = TimeMod.id("chronos_state");
    private static final int STATE_INTERVAL = 4;

    private ChronosNetworking() {
    }

    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(ACTION, (server, player, handler, buf, sender) -> {
            int action;
            int value;
            try {
                action = buf.readVarInt();
                value = buf.readVarInt();
            } catch (Throwable error) {
                Safety.report("Chronos-Uhr: Paket lesen", error);
                return;
            }
            server.execute(() -> Safety.run("Chronos-Uhr: Aktion", () -> handle(player, action, value)));
        });
        ServerTickEvents.END_WORLD_TICK.register(level -> Safety.run("Chronos-Uhr: Zustand senden", () -> {
            if (level.getGameTime() % STATE_INTERVAL == 0) {
                for (ServerPlayer player : level.players()) {
                    if (!ChronosWatchItem.inHand(player).isEmpty()) {
                        sendState(level, player);
                    }
                }
            }
        }));
    }

    static void handle(ServerPlayer player, int actionId, int value) {
        if (actionId < 0 || actionId >= ChronosSettings.Action.values().length) {
            return;
        }
        ItemStack stack = ChronosWatchItem.inHand(player);
        if (stack.isEmpty()) {
            return;
        }
        ChronosSettings settings = ChronosSettings.of(stack);
        ChronosSettings.Action action = ChronosSettings.Action.values()[actionId];
        if (settings.apply(action, value)) {
            settings.save(stack);
            float pitch = action == ChronosSettings.Action.MODE ? 1.0F : 1.6F + 0.1F * Math.signum(value);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.CLOCK_TICK, SoundSource.PLAYERS, 0.4F, pitch);
        }
    }

    /** Zeitleiste: Wie weit lässt sich zurückspulen, und läuft gerade eine eigene Wiedergabe? */
    private static void sendState(ServerLevel level, ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, STATE)) {
            return;
        }
        ChronoJournal journal = ServerTimeEngine.of(level).journal();
        RewindSession session = journal.session();
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBoolean(journal.isRecording());
        buf.writeVarInt(journal.isRecording() ? journal.availableTicks() : 0);
        buf.writeVarInt(journal.capacityTicks());
        boolean own = session != null && player.getUUID().equals(session.owner());
        buf.writeBoolean(own);
        buf.writeVarInt(own ? session.done() : 0);
        buf.writeVarInt(own ? session.totalSteps() : 0);
        ServerPlayNetworking.send(player, STATE, buf);
    }

    /** Für den Client: eine Aktion verschlüsseln. */
    public static FriendlyByteBuf encodeAction(ChronosSettings.Action action, int value) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(action.ordinal());
        buf.writeVarInt(value);
        return buf;
    }
}
