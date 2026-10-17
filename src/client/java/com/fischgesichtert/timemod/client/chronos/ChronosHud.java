package com.fischgesichtert.timemod.client.chronos;

import com.fischgesichtert.timemod.chronos.ChronosSettings;
import com.fischgesichtert.timemod.chronos.ChronosWatchItem;
import com.fischgesichtert.timemod.energy.Chronon;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * HUD der Chronos-Uhr: neben der Hotbar Modus, Wert, Kosten, Chronon und Meisterschaft; beim
 * Zurückspulen darüber die Zeitleiste – wie weit sich die Welt erinnert, wohin es zurückgeht und
 * (beim Scrubbing) wo die Wiedergabe gerade steht.
 */
final class ChronosHud {
    private static final int PANEL_WIDTH = 118;
    private static final int TIMELINE_WIDTH = 182;

    private ChronosHud() {
    }

    static void render(GuiGraphics graphics, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen instanceof RadialMenuScreen) {
            return;
        }
        ItemStack watch = ChronosWatchItem.inHand(mc.player);
        if (watch.isEmpty()) {
            return;
        }
        ChronosSettings s = ChronosSettings.of(watch);
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        panel(graphics, mc, watch, s, width, height);
        if (s.mode == ChronosSettings.Mode.REWIND) {
            timeline(graphics, mc.font, s, width / 2, height - 76);
        }
    }

    private static void panel(GuiGraphics graphics, Minecraft mc, ItemStack watch, ChronosSettings s, int width, int height) {
        Font font = mc.font;
        int x = width / 2 + 100;
        int y = height - 46;
        if (x + PANEL_WIDTH > width - 2) {
            x = width - PANEL_WIDTH - 2;
            y = height - 120;
        }
        int color = ChronosStyle.color(s.mode);
        graphics.fill(x - 4, y - 3, x + PANEL_WIDTH, y + 45, 0x98101018);
        graphics.fill(x - 4, y - 3, x - 2, y + 45, 0xFF000000 | color);
        graphics.renderItem(ChronosStyle.icon(s.mode), x, y);
        graphics.drawString(font, ChronosWatchItem.modeName(s.mode), x + 19, y, color);
        Component value = Component.literal(ChronosStyle.mainValue(s)).withStyle(ChatFormatting.BOLD)
            .append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
            .append(ChronosStyle.details(s).copy().withStyle(ChatFormatting.GRAY));
        graphics.drawString(font, value, x + 19, y + 9, 0xFFFFFF);
        boolean cooling = mc.player.getCooldowns().isOnCooldown(watch.getItem());
        Component cost = cooling ? Component.translatable("chronos.timemod.hud.cooldown")
            : Component.translatable("chronos.timemod.hud.cost", Chronon.format(ChronosWatchItem.cost(s)));
        graphics.drawString(font, cost, x, y + 20, cooling ? 0x8A8A9A : ChronosStyle.CHRONON);
        String stored = Chronon.format(Chronon.stored(watch));
        graphics.drawString(font, stored, x + PANEL_WIDTH - 4 - font.width(stored), y + 20, 0xBFEFFF);
        ChronosStyle.bar(graphics, x, y + 30, PANEL_WIDTH - 5, Chronon.stored(watch) / (float) ChronosWatchItem.CAPACITY, ChronosStyle.CHRONON);
        Component level = Component.translatable("chronos.timemod.level." + s.level());
        graphics.drawString(font, level, x, y + 35, ChronosStyle.GOLD);
        int barX = x + font.width(level) + 5;
        ChronosStyle.bar(graphics, barX, y + 38, x + PANEL_WIDTH - 5 - barX, s.levelProgress(), ChronosStyle.GOLD);
    }

    /** Zeitleiste: links die älteste Erinnerung, rechts „jetzt“. */
    static void timeline(GuiGraphics graphics, Font font, ChronosSettings s, int centerX, int y) {
        int x0 = centerX - TIMELINE_WIDTH / 2;
        int x1 = x0 + TIMELINE_WIDTH;
        graphics.fill(x0 - 3, y - 14, x1 + 3, y + 19, 0x98101018);
        if (!ChronosClient.recording || !ChronosClient.fresh()) {
            graphics.drawCenteredString(font, Component.translatable("chronos.timemod.hud.no_memory"), centerX, y, 0x8A8A9A);
            return;
        }
        int capacity = ChronosClient.capacity;
        boolean scrubbing = ChronosClient.isScrubbing();
        int done = scrubbing ? ChronosClient.done : 0;
        int span = Math.min(capacity, ChronosClient.availableTicks() + done);
        graphics.fill(x0, y, x1, y + 6, 0xFF1C1C26);
        int recorded = Math.round(TIMELINE_WIDTH * span / (float) capacity);
        graphics.fill(x1 - recorded, y, x1, y + 6, 0xFF2A8C6A);
        graphics.fill(x1 - recorded, y, x1, y + 1, 0xFF6FF2C2);
        for (int sec = 10; sec * 20 < capacity; sec += 10) {
            int tx = x1 - Math.round(TIMELINE_WIDTH * sec * 20F / capacity);
            graphics.fill(tx, y + 1, tx + 1, y + 6, 0x70FFFFFF);
        }
        if (scrubbing) {
            int head = x1 - Math.round(TIMELINE_WIDTH * done / (float) capacity);
            graphics.fill(head, y, x1, y + 6, 0xE0A060E8);
            graphics.fill(head - 1, y - 3, head + 1, y + 9, 0xFFFFFFFF);
            graphics.drawCenteredString(font, Component.translatable("chronos.timemod.hud.scrubbing"), centerX, y + 9, 0xD9A8FF);
            return;
        }
        int depth = Math.min(s.rewindSeconds * 20, span);
        int target = x1 - Math.round(TIMELINE_WIDTH * depth / (float) capacity);
        graphics.fill(target, y - 3, target + 1, y + 9, 0xFFFFFFFF);
        String label = "−" + Math.round(depth / 20F) + " s";
        graphics.drawCenteredString(font, label, Math.max(x0 + 10, Math.min(x1 - 10, target)), y - 12, 0xFFFFFF);
        graphics.drawString(font, "−" + capacity / 20 + " s", x0, y + 9, 0x8A8A9A);
        Component now = Component.translatable("chronos.timemod.hud.now");
        graphics.drawString(font, now, x1 - font.width(now), y + 9, 0x8A8A9A);
        graphics.drawCenteredString(font, Component.translatable("chronos.timemod.hud.memory", span / 20), centerX, y + 9, 0x6FF2C2);
    }
}
