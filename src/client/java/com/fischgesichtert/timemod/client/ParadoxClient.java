package com.fischgesichtert.timemod.client;

import com.fischgesichtert.timemod.energy.ChrononHolder;
import com.fischgesichtert.timemod.item.PocketWatchItem;
import com.fischgesichtert.timemod.item.TimeSealItem;
import com.fischgesichtert.timemod.paradox.ParadoxPressure;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

/**
 * Paradox-Druck auf dem Client: oben links eine kleine Anzeige (mit Zeitwerkzeug in der Hand oder ab
 * 25 %), ab 50 % ein pulsierender violetter Rand – die Zeit franst aus.
 */
public final class ParadoxClient {
    private static final int BAR_WIDTH = 64;
    /** Was der Server zuletzt gemeldet hat (0..100). */
    private static float pressure;
    /** Geglättet für die Anzeige. */
    private static float shown;

    private ParadoxClient() {
    }

    public static float pressure() {
        return pressure;
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(ParadoxPressure.SYNC, (client, handler, buf, sender) -> {
            float value = buf.readFloat();
            client.execute(() -> pressure = value);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            pressure = 0.0F;
            shown = 0.0F;
        });
        HudRenderCallback.EVENT.register(ParadoxClient::render);
    }

    /** Farbe der Stufe: grau, gelb, rot, violett. */
    public static int color(float value) {
        if (value >= ParadoxPressure.HUNTED) {
            return 0xE070FF;
        }
        if (value >= ParadoxPressure.NOTICED) {
            return 0xFF5A4A;
        }
        return value >= ParadoxPressure.UNEASE ? 0xFFD24A : 0xA8A8B8;
    }

    private static boolean holdsTimeTool(Player player) {
        for (Item item : new Item[] {player.getMainHandItem().getItem(), player.getOffhandItem().getItem()}) {
            if (item instanceof ChrononHolder || item instanceof PocketWatchItem || item instanceof TimeSealItem) {
                return true;
            }
        }
        return false;
    }

    private static void render(GuiGraphics graphics, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        shown = Math.abs(shown - pressure) < 0.05F ? pressure : Mth.lerp(0.08F, shown, pressure);
        if (shown < 0.5F) {
            return;
        }
        if (shown >= ParadoxPressure.NOTICED) {
            edge(graphics, (shown - ParadoxPressure.NOTICED) / (ParadoxPressure.MAX - ParadoxPressure.NOTICED), mc.player.tickCount + tickDelta);
        }
        if (shown >= ParadoxPressure.UNEASE || holdsTimeTool(mc.player)) {
            gauge(graphics, mc.font, 6, 6);
        }
    }

    private static void gauge(GuiGraphics graphics, Font font, int x, int y) {
        int color = color(shown);
        Component label = Component.translatable("hud.timemod.paradox");
        String percent = Math.round(shown) + " %";
        int width = Math.max(BAR_WIDTH, font.width(label) + font.width(percent) + 8);
        graphics.fill(x - 3, y - 3, x + width + 3, y + 15, 0x98101018);
        graphics.fill(x - 3, y - 3, x - 1, y + 15, 0xFF000000 | color);
        graphics.drawString(font, label, x + 1, y, 0xE8E0F0);
        graphics.drawString(font, percent, x + width - font.width(percent), y, color);
        int filled = Math.round(width * shown / ParadoxPressure.MAX);
        graphics.fill(x + 1, y + 10, x + width, y + 12, 0xFF2A2436);
        graphics.fill(x + 1, y + 10, x + 1 + Math.max(0, filled - 1), y + 12, 0xFF000000 | color);
        // Schwellen-Marken bei 25, 50 und 75 %
        for (float mark : new float[] {ParadoxPressure.UNEASE, ParadoxPressure.NOTICED, ParadoxPressure.HUNTED}) {
            int mx = x + 1 + Math.round((width - 1) * mark / ParadoxPressure.MAX);
            graphics.fill(mx, y + 9, mx + 1, y + 13, 0xFF6A6080);
        }
    }

    /** Violetter Rand, der mit dem Druck kräftiger wird und langsam pulsiert. */
    private static void edge(GuiGraphics graphics, float strength, float time) {
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        float pulse = 0.65F + 0.35F * Mth.sin(time * 0.12F);
        int depth = 18 + Math.round(22 * strength);
        float peak = Mth.clamp(0.12F + 0.28F * strength, 0.0F, 0.4F) * pulse;
        for (int i = 0; i < depth; i++) {
            float fade = 1.0F - i / (float) depth;
            int alpha = Math.round(255 * peak * fade * fade);
            if (alpha <= 0) {
                continue;
            }
            int argb = (alpha << 24) | 0x6A1E9A;
            graphics.fill(i, i, width - i, i + 1, argb);
            graphics.fill(i, height - i - 1, width - i, height - i, argb);
            graphics.fill(i, i + 1, i + 1, height - i - 1, argb);
            graphics.fill(width - i - 1, i + 1, width - i, height - i - 1, argb);
        }
    }
}
