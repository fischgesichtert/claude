package com.fischgesichtert.timemod.client.era;

import com.fischgesichtert.timemod.era.Era;
import com.fischgesichtert.timemod.era.Eras;
import com.fischgesichtert.timemod.gate.TimeStream;
import com.fischgesichtert.timemod.registry.ModSounds;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

/**
 * Der Zeitstrom im Client: Der Server kündigt die Reise an, dann zeigt der Bildschirm den Tunnel, bis
 * der Server die Landung meldet. Den Wechsel der Welt verdeckt der Strom – kein „Landschaft wird geladen“.
 */
public final class TimeStreamClient {
    private static boolean active;
    private static boolean arrived;
    private static int fromYears;
    private static int toYears;
    private static int duration;
    /** Ticks seit Beginn der Reise (Client-Zeit). */
    private static int ticks;
    /** Ticks seit der Landung (fürs Ausblenden). */
    private static int sinceArrival;
    @Nullable
    private static SoundInstance sound;
    @Nullable
    private static ClientLevel soundLevel;
    @Nullable
    private static TimeStreamScreen screen;

    private TimeStreamClient() {
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(TimeStream.START, (client, handler, buf, sender) -> {
            int from = buf.readInt();
            int to = buf.readInt();
            int length = buf.readVarInt();
            client.execute(() -> begin(from, to, length));
        });
        ClientPlayNetworking.registerGlobalReceiver(TimeStream.ARRIVE, (client, handler, buf, sender) -> client.execute(TimeStreamClient::arrive));
        ClientTickEvents.END_CLIENT_TICK.register(TimeStreamClient::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(TimeStreamClient::reset));
    }

    static void begin(int from, int to, int length) {
        active = true;
        arrived = false;
        fromYears = from;
        toYears = to;
        duration = Math.max(20, length);
        ticks = 0;
        sinceArrival = 0;
        stopSound();
        Minecraft.getInstance().setScreen(screen());
    }

    static void arrive() {
        if (active) {
            arrived = true;
            sinceArrival = 0;
            stopSound();
        }
    }

    private static void tick(Minecraft mc) {
        if (!active) {
            return;
        }
        ticks++;
        if (arrived) {
            sinceArrival++;
        }
        // Der Weltwechsel stoppt alle Klänge – das Rauschen des Stroms beginnt (wieder) in der neuen Welt
        if (!arrived && mc.level != null && (sound == null || mc.level != soundLevel)) {
            stopSound();
            sound = SimpleSoundInstance.forUI(ModSounds.TIME_STREAM, 1.0F, 0.9F);
            soundLevel = mc.level;
            mc.getSoundManager().play(sound);
        }
        // Falls ein anderer Bildschirm den Strom verdrängt hat (z. B. nach dem Weltwechsel)
        if (!arrived && mc.screen != screen() && mc.level != null) {
            mc.setScreen(screen());
        }
        if (arrived && sinceArrival > TimeStreamScreen.FADE_TICKS) {
            finish(mc);
        }
        // Sicherheitsnetz: Meldet der Server nichts mehr, endet der Strom von selbst
        if (ticks > duration + 600) {
            finish(mc);
        }
    }

    private static void finish(Minecraft mc) {
        boolean showing = mc.screen == screen;
        reset();
        if (showing) {
            mc.setScreen(null);
        }
    }

    private static void reset() {
        active = false;
        arrived = false;
        stopSound();
        screen = null;
    }

    private static void stopSound() {
        if (sound != null) {
            Minecraft.getInstance().getSoundManager().stop(sound);
            sound = null;
        }
    }

    public static boolean isActive() {
        return active;
    }

    /** Der Strom-Bildschirm, solange eine Reise läuft (für die Mixins beim Weltwechsel). */
    public static Screen screen() {
        if (screen == null) {
            screen = new TimeStreamScreen();
        }
        return screen;
    }

    // ------------------------------------------------------------------ Für die Anzeige

    static boolean arrived() {
        return arrived;
    }

    static float progress(float partialTick) {
        return Math.min(1.0F, (ticks + partialTick) / duration);
    }

    static float seconds(float partialTick) {
        return (ticks + partialTick) / 20.0F;
    }

    static float fade(float partialTick) {
        return arrived ? Math.min(1.0F, (sinceArrival + partialTick) / TimeStreamScreen.FADE_TICKS) : 0.0F;
    }

    static int fromYears() {
        return fromYears;
    }

    static int toYears() {
        return toYears;
    }

    /** Das Ziel als Epoche (für Farbe und Namen). */
    static Era target() {
        Era era = Eras.byYears(toYears);
        return era == null ? Eras.PRESENT : era;
    }

    /** Jahreszähler: gleitet im logarithmischen Maß, damit auch 245 Millionen Jahre in Sekunden vorbeirasen. */
    static double yearsAt(float t) {
        double a = signedLog(fromYears);
        double b = signedLog(toYears);
        double eased = t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
        double value = a + (b - a) * eased;
        return Math.signum(value) * (Math.pow(10, Math.abs(value)) - 1);
    }

    private static double signedLog(double years) {
        return Math.signum(years) * Math.log10(1 + Math.abs(years));
    }
}
