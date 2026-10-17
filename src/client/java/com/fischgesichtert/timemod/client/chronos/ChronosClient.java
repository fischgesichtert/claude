package com.fischgesichtert.timemod.client.chronos;

import com.fischgesichtert.timemod.chronos.ChronosSettings;
import com.fischgesichtert.timemod.chronos.ChronosWatchItem;
import com.fischgesichtert.timemod.network.ChronosNetworking;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * Chronos-Uhr im Client: Taste R für das Radialmenü, Zustand der Zeitleiste vom Server, HUD,
 * Geisterpfad – und Aktionen, die sofort lokal sichtbar werden und dann zum Server gehen.
 */
public final class ChronosClient {
    public static final KeyMapping MENU = new KeyMapping("key.timemod.chronos_menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R,
        "key.categories.timemod");

    /** Stand des Zeitjournals (vom Server, alle paar Ticks). */
    static boolean recording;
    static int available;
    static int capacity = 1200;
    /** Läuft gerade eine eigene Rückspul-Wiedergabe? */
    static boolean scrubbing;
    static int done;
    static int total;
    private static long ticks;
    private static long lastState = -1000;

    private ChronosClient() {
    }

    public static void init() {
        KeyBindingHelper.registerKeyBinding(MENU);
        ClientPlayNetworking.registerGlobalReceiver(ChronosNetworking.STATE, (client, handler, buf, sender) -> {
            boolean rec = buf.readBoolean();
            int avail = buf.readVarInt();
            int cap = buf.readVarInt();
            boolean own = buf.readBoolean();
            int d = buf.readVarInt();
            int t = buf.readVarInt();
            client.execute(() -> {
                recording = rec;
                available = avail;
                capacity = Math.max(20, cap);
                scrubbing = own;
                done = d;
                total = t;
                lastState = ticks;
            });
        });
        ClientTickEvents.END_CLIENT_TICK.register(ChronosClient::tick);
        HudRenderCallback.EVENT.register(ChronosHud::render);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(GhostPath::render);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            lastState = -1000;
            scrubbing = false;
            GhostPath.clear();
        });
    }

    private static void tick(Minecraft mc) {
        ticks++;
        GhostPath.tick(mc);
        if (mc.player == null) {
            return;
        }
        while (MENU.consumeClick()) {
            if (mc.screen == null && !ChronosWatchItem.inHand(mc.player).isEmpty()) {
                mc.setScreen(new RadialMenuScreen());
            }
        }
    }

    /** Ist der Stand der Zeitleiste aktuell (höchstens eine Sekunde alt)? */
    static boolean fresh() {
        return ticks - lastState < 20;
    }

    /** Wie viele Ticks zurück sind gerade möglich? */
    static int availableTicks() {
        return fresh() && recording ? available : 0;
    }

    static boolean isScrubbing() {
        return fresh() && scrubbing;
    }

    /** Aktion an den Server – und sofort lokal anwenden, damit Menü und HUD ohne Verzögerung folgen. */
    public static void send(ChronosSettings.Action action, int value) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        ItemStack watch = ChronosWatchItem.inHand(mc.player);
        if (watch.isEmpty()) {
            return;
        }
        ChronosSettings settings = ChronosSettings.of(watch);
        if (settings.apply(action, value)) {
            settings.save(watch);
        }
        if (ClientPlayNetworking.canSend(ChronosNetworking.ACTION)) {
            ClientPlayNetworking.send(ChronosNetworking.ACTION, ChronosNetworking.encodeAction(action, value));
        }
    }
}
