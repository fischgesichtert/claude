package com.fischgesichtert.timemod.paradox;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.chronos.ChronosSettings;
import com.fischgesichtert.timemod.chronos.ChronosWatchItem;
import com.fischgesichtert.timemod.era.Era;
import com.fischgesichtert.timemod.gate.TimeStream;
import com.fischgesichtert.timemod.module.Modules;
import com.fischgesichtert.timemod.registry.ModAdvancements;
import com.fischgesichtert.timemod.registry.ModSounds;
import com.fischgesichtert.timemod.safety.Safety;
import io.netty.buffer.Unpooled;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Paradox-Druck: Wer die Zeit verbiegt, hinterlässt Spuren in ihr. Jeder Spieler trägt einen Druck
 * von 0 bis 100 %. Er steigt mit jedem Chronon, das in Zeitmanipulation fließt, und mit jeder Reise
 * durch die Zeit – und klingt von selbst wieder ab, schneller im Schlaf.
 * <ul>
 *   <li>ab 25 % wird die Zeit um den Spieler unruhig,</li>
 *   <li>ab 50 % bemerken ihn die Zeitwächter,</li>
 *   <li>ab 75 % jagen sie ihn in Gruppen.</li>
 * </ul>
 * Wer im Kreativmodus spielt, verbiegt nichts: Er zahlt kein Chronon und baut keinen Druck auf.
 */
public class ParadoxPressure extends SavedData {
    private static final String NAME = "timemod_paradox";
    public static final ResourceLocation SYNC = TimeMod.id("paradox");

    public static final float UNEASE = 25.0F;
    public static final float NOTICED = 50.0F;
    public static final float HUNTED = 75.0F;
    public static final float MAX = 100.0F;
    /** Von selbst: 2 % pro Minute. */
    public static final float DECAY_PER_SECOND = 2.0F / 60.0F;
    /** Eine durchschlafene Nacht beruhigt die Zeitlinie. */
    public static final float SLEEP_RELIEF = 15.0F;
    /** Jedes Dämpfungsmodul halbiert den Druck seines Werkzeugs. */
    public static final float DAMPING = 0.5F;

    private final Map<UUID, Float> pressure = new HashMap<>();
    /** Was der Client zuletzt erfahren hat (nicht gespeichert). */
    private final Map<UUID, Float> synced = new HashMap<>();

    public static ParadoxPressure get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(ParadoxPressure::load, ParadoxPressure::new, NAME);
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 == 0) {
                Safety.run("Paradox-Druck: Abklingen", () -> get(server).decay(server));
            }
            Safety.run("Zeitwächter", () -> TimeWardens.tick(server));
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
            server.execute(() -> Safety.run("Paradox-Druck: senden", () -> sync(handler.player, true))));
        EntitySleepEvents.STOP_SLEEPING.register((entity, pos) -> Safety.run("Paradox-Druck: Schlaf", () -> {
            if (entity instanceof ServerPlayer player && player.level().isDay() && of(player) > 0.0F) {
                relieve(player, SLEEP_RELIEF);
            }
        }));
    }

    // ------------------------------------------------------------------ Lesen und Schreiben

    public static float of(ServerPlayer player) {
        return get(player.server).value(player.getUUID());
    }

    public float value(UUID player) {
        return pressure.getOrDefault(player, 0.0F);
    }

    public static void set(ServerPlayer player, float value) {
        float before = of(player);
        float after = Mth.clamp(value, 0.0F, MAX);
        ParadoxPressure data = get(player.server);
        if (after <= 0.0F) {
            data.pressure.remove(player.getUUID());
        } else {
            data.pressure.put(player.getUUID(), after);
        }
        data.setDirty();
        announce(player, before, after);
        sync(player, false);
    }

    public static void add(ServerPlayer player, float amount) {
        if (amount > 0.0F) {
            set(player, of(player) + amount);
        }
    }

    public static void relieve(ServerPlayer player, float amount) {
        if (amount > 0.0F) {
            set(player, of(player) - amount);
        }
    }

    /** Wer gerade zählt: Spieler im Überlebens- oder Abenteuermodus. */
    public static boolean counts(Player player) {
        return player instanceof ServerPlayer && !player.isSpectator() && !player.getAbilities().instabuild;
    }

    // ------------------------------------------------------------------ Ursachen

    /** Druck für eine Zeitmanipulation: wächst mit der Wurzel ihrer Kosten (400 Ch → 2 %, 100 000 Ch → 32 %). */
    public static float forChronon(int cost) {
        return cost <= 0 ? 0.0F : (float) Math.sqrt(cost) / 10.0F;
    }

    /** Druck einer Reise: 2,5 % je Zehnerpotenz an Jahren (+1000 → 7,5 %, −245 Mio. → 21 %). */
    public static float forTravel(long fromYears, long toYears) {
        return (float) (2.5 * Math.log10(1.0 + Math.abs((double) toYears - fromYears)));
    }

    /**
     * Wie stark ein Werkzeug den Druck weitergibt: Dämpfungsmodule halbieren ihn je Stück, die
     * Meisterschaft der Chronos-Uhr senkt ihn um bis zu 40 %.
     */
    public static float damping(ItemStack tool) {
        float factor = (float) Math.pow(DAMPING, Modules.effects(tool).damping());
        if (tool.getItem() instanceof ChronosWatchItem) {
            factor *= 1.0F - 0.08F * ChronosSettings.of(tool).level();
        }
        return factor;
    }

    /** Ein Werkzeug hat {@code cost} Chronon verbraucht. */
    public static void onChrononSpent(Player player, ItemStack tool, int cost) {
        if (counts(player)) {
            add((ServerPlayer) player, forChronon(cost) * damping(tool));
        }
    }

    public static void onTravel(ServerPlayer player, Era from, Era to) {
        if (counts(player)) {
            add(player, forTravel(from.years(), to.years()));
        }
    }

    // ------------------------------------------------------------------ Ablauf

    private void decay(MinecraftServer server) {
        if (pressure.isEmpty()) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Float value = pressure.get(player.getUUID());
            // Im Zeitstrom steht die Zeit für einen selbst still
            if (value != null && !TimeStream.inStream(player.getUUID())) {
                set(player, value - DECAY_PER_SECOND);
            }
        }
    }

    /** Meldungen beim Überschreiten der Schwellen – hinauf wie hinab. */
    private static void announce(ServerPlayer player, float before, float after) {
        String key = null;
        ChatFormatting color = ChatFormatting.DARK_PURPLE;
        if (before < HUNTED && after >= HUNTED) {
            key = "hunted";
            color = ChatFormatting.LIGHT_PURPLE;
            ModAdvancements.award(player, "wanted");
        } else if (before < NOTICED && after >= NOTICED) {
            key = "noticed";
        } else if (before < UNEASE && after >= UNEASE) {
            key = "unease";
        } else if (before >= UNEASE && after < UNEASE) {
            key = "calm";
            color = ChatFormatting.GRAY;
        }
        if (key == null) {
            return;
        }
        player.displayClientMessage(Component.translatable("message.timemod.paradox." + key).withStyle(color), true);
        if (!key.equals("calm")) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.PARADOX_WARNING, SoundSource.HOSTILE,
                0.8F, key.equals("hunted") ? 0.8F : 1.0F);
        }
    }

    /** Schickt dem Client den Druck, sobald er sich um ein halbes Prozent geändert hat (oder sofort). */
    public static void sync(ServerPlayer player, boolean force) {
        ParadoxPressure data = get(player.server);
        float value = data.value(player.getUUID());
        Float last = data.synced.get(player.getUUID());
        if (!force && last != null && Math.abs(last - value) < 0.5F && (value > 0.0F || last == 0.0F)) {
            return;
        }
        if (!ServerPlayNetworking.canSend(player, SYNC)) {
            return;
        }
        data.synced.put(player.getUUID(), value);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeFloat(value);
        ServerPlayNetworking.send(player, SYNC, buf);
    }

    // ------------------------------------------------------------------ Speichern

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag players = new CompoundTag();
        pressure.forEach((uuid, value) -> players.putFloat(uuid.toString(), value));
        tag.put("Players", players);
        return tag;
    }

    private static ParadoxPressure load(CompoundTag tag) {
        ParadoxPressure data = new ParadoxPressure();
        CompoundTag players = tag.getCompound("Players");
        for (String key : players.getAllKeys()) {
            try {
                data.pressure.put(UUID.fromString(key), players.getFloat(key));
            } catch (IllegalArgumentException ignored) {
                // Unlesbarer Eintrag – vergessen
            }
        }
        return data;
    }
}
