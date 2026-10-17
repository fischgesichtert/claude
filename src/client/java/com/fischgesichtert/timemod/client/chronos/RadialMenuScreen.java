package com.fischgesichtert.timemod.client.chronos;

import com.fischgesichtert.timemod.chronos.ChronosSettings;
import com.fischgesichtert.timemod.chronos.ChronosWatchItem;
import com.fischgesichtert.timemod.energy.Chronon;
import com.fischgesichtert.timemod.rewind.RewindSession;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Das Radialmenü der Chronos-Uhr (Taste R). Gedrückt halten, zeigen, loslassen wählt den Modus wie
 * ein Waffenrad; kurz antippen lässt das Menü offen. Im äußeren Ring liegen die Filter, rechts die
 * Werte, links Kosten und Meisterschaft. Mausrad: Hauptwert, mit Umschalt: Radius, mit Strg: Dauer.
 */
public class RadialMenuScreen extends Screen {
    private static final float TAU = (float) (Math.PI * 2);
    private static final float CENTER_R = 30;
    private static final float MODE_IN = 36;
    private static final float MODE_OUT = 78;
    private static final float FILTER_R = 102;
    private static final float FILTER_SIZE = 11;
    private static final float GAP = 0.09F;
    private static final int LAYOUT_WIDTH = 520;
    private static final int LAYOUT_HEIGHT = 300;
    private static final int PANEL_WIDTH = 124;
    private static final long TAP_MILLIS = 300;

    private final long openedAt = Util.getMillis();
    private boolean held = true;
    private final List<Hotspot> hotspots = new ArrayList<>();
    private int cx;
    private int cy;
    private float scale = 1.0F;

    /** Anklickbarer Bereich (in Layout-Koordinaten) mit Aktion und Tooltip. */
    private record Hotspot(float x0, float y0, float x1, float y1, @Nullable Runnable action, @Nullable Component tooltip) {
        boolean contains(double x, double y) {
            return x >= x0 && x < x1 && y >= y0 && y < y1;
        }
    }

    public RadialMenuScreen() {
        super(Component.translatable("item.timemod.chronos_watch"));
    }

    @Override
    protected void init() {
        cx = width / 2;
        cy = height / 2 - 6;
        scale = Math.min(1.0F, Math.min(width / (float) LAYOUT_WIDTH, height / (float) LAYOUT_HEIGHT));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (minecraft == null || minecraft.player == null || ChronosWatchItem.inHand(minecraft.player).isEmpty()) {
            onClose();
        }
    }

    @Nullable
    private ChronosSettings settings() {
        if (minecraft == null || minecraft.player == null) {
            return null;
        }
        ItemStack watch = ChronosWatchItem.inHand(minecraft.player);
        return watch.isEmpty() ? null : ChronosSettings.of(watch);
    }

    private double layoutX(double mouseX) {
        return cx + (mouseX - cx) / scale;
    }

    private double layoutY(double mouseY) {
        return cy + (mouseY - cy) / scale;
    }

    /** Welcher Modus-Sektor liegt unter dem Zeiger? ({@code wide}: auch außerhalb des Rings, fürs Loslassen.) */
    private int modeAt(double x, double y, boolean wide) {
        double dx = x - cx;
        double dy = y - cy;
        double d = Math.sqrt(dx * dx + dy * dy);
        if (d < CENTER_R || d > (wide ? FILTER_R - FILTER_SIZE - 2 : MODE_OUT + 4)) {
            return -1;
        }
        double angle = Math.atan2(dy, dx) + Math.PI / 2;
        int index = (int) Math.round(angle / (Math.PI / 2));
        return Math.floorMod(index, 4);
    }

    // ------------------------------------------------------------------ Zeichnen

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ChronosSettings s = settings();
        if (s == null) {
            return;
        }
        graphics.fillGradient(0, 0, width, height, 0x90060410, 0xB0060410);
        double mx = layoutX(mouseX);
        double my = layoutY(mouseY);
        hotspots.clear();
        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.pose().translate(-cx, -cy, 0);
        modes(graphics, s, mx, my);
        center(graphics, s);
        filters(graphics, s, mx, my);
        values(graphics, s, mx, my);
        info(graphics, s);
        graphics.pose().popPose();
        graphics.drawCenteredString(font, Component.translatable("chronos.timemod.menu.hint"), width / 2, 6, 0x9090A0);
        for (Hotspot spot : hotspots) {
            if (spot.tooltip() != null && spot.contains(mx, my)) {
                graphics.renderTooltip(font, spot.tooltip(), mouseX, mouseY);
                break;
            }
        }
    }

    private void modes(GuiGraphics graphics, ChronosSettings s, double mx, double my) {
        int hovered = modeAt(mx, my, false);
        for (ChronosSettings.Mode mode : ChronosSettings.Mode.values()) {
            int i = mode.ordinal();
            float mid = (float) (-Math.PI / 2 + i * Math.PI / 2);
            float a0 = mid - (float) Math.PI / 4 + GAP / 2;
            float a1 = mid + (float) Math.PI / 4 - GAP / 2;
            boolean active = s.mode == mode;
            int color = ChronosStyle.color(mode);
            int alpha = active ? 0xD8 : i == hovered ? 0x98 : 0x50;
            ChronosStyle.arc(graphics, cx, cy, MODE_IN, MODE_OUT, a0, a1, ChronosStyle.withAlpha(color, alpha));
            if (active || i == hovered) {
                ChronosStyle.arc(graphics, cx, cy, MODE_OUT + 2, MODE_OUT + 4, a0, a1, ChronosStyle.withAlpha(color, active ? 0xFF : 0xA0));
            }
            float ir = (MODE_IN + MODE_OUT) / 2;
            int ix = Math.round(cx + (float) Math.cos(mid) * ir) - 8;
            int iy = Math.round(cy + (float) Math.sin(mid) * ir) - 13;
            graphics.renderItem(ChronosStyle.icon(mode), ix, iy);
            scaledCentered(graphics, ChronosWatchItem.modeName(mode), ix + 8, iy + 18, 0.75F, active ? 0xFFFFFF : 0xC8C8D0);
        }
    }

    private void center(GuiGraphics graphics, ChronosSettings s) {
        ChronosStyle.disc(graphics, cx, cy, CENTER_R, 0xE8101018);
        ChronosStyle.arc(graphics, cx, cy, CENTER_R - 1.5F, CENTER_R, 0, TAU, 0xFF000000 | ChronosStyle.color(s.mode));
        scaledCentered(graphics, Component.literal(ChronosStyle.mainValue(s)), cx, cy - 9, 1.6F, 0xFFFFFF);
        scaledCentered(graphics, ChronosStyle.details(s), cx, cy + 7, 0.7F, 0xB8B8C8);
    }

    private void filters(GuiGraphics graphics, ChronosSettings s, double mx, double my) {
        int count = ChronosSettings.FILTERS.length;
        int mask = s.effectiveMask();
        int color = ChronosStyle.color(s.mode);
        for (int i = 0; i < count; i++) {
            int bits = ChronosSettings.FILTERS[i];
            float a = (float) (-Math.PI / 2 + (i + 0.5) * TAU / count);
            float fx = cx + (float) Math.cos(a) * FILTER_R;
            float fy = cy + (float) Math.sin(a) * FILTER_R;
            boolean on = (mask & bits) != 0;
            boolean globalOnly = (bits & ChronosSettings.GLOBAL_ONLY) != 0;
            boolean idle = globalOnly && !s.global;
            boolean hover = (mx - fx) * (mx - fx) + (my - fy) * (my - fy) <= FILTER_SIZE * FILTER_SIZE;
            int background = on ? ChronosStyle.withAlpha(color, idle ? 0x58 : 0xC8) : 0x80202028;
            ChronosStyle.disc(graphics, fx, fy, FILTER_SIZE, background);
            if (hover) {
                ChronosStyle.arc(graphics, fx, fy, FILTER_SIZE, FILTER_SIZE + 1.5F, 0, TAU, 0xFFFFFFFF);
            }
            int ix = Math.round(fx) - 8;
            int iy = Math.round(fy) - 8;
            graphics.renderItem(ChronosStyle.filterIcon(i), ix, iy);
            if (!on || idle) {
                graphics.pose().pushPose();
                graphics.pose().translate(0, 0, 300);
                graphics.fill(ix, iy, ix + 16, iy + 16, on ? 0x60101018 : 0xA0101018);
                graphics.pose().popPose();
            }
            Component name = Component.translatable("filter.timemod." + ChronosSettings.FILTER_NAMES[i]);
            Component tooltip = globalOnly
                ? name.copy().append(Component.literal(" (").append(Component.translatable("chronos.timemod.menu.global_only")).append(")")
                    .withStyle(ChatFormatting.GRAY))
                : name;
            int index = i;
            hotspots.add(new Hotspot(fx - FILTER_SIZE, fy - FILTER_SIZE, fx + FILTER_SIZE, fy + FILTER_SIZE,
                () -> ChronosClient.send(ChronosSettings.Action.FILTER, index), tooltip));
        }
    }

    /** Rechte Spalte: Werte mit ‹ › und die Schalter. */
    private void values(GuiGraphics graphics, ChronosSettings s, double mx, double my) {
        int x = Math.round(cx + FILTER_R + FILTER_SIZE + 16);
        int y = cy - 56;
        graphics.fill(x - 6, y - 6, x + PANEL_WIDTH, y + 112, 0xA0101018);
        String main = s.mode == ChronosSettings.Mode.REWIND ? s.rewindSeconds + " s" : ChronosStyle.mainValue(s);
        y = row(graphics, x, y, Component.translatable("chronos.timemod.menu.value." + modeKey(s.mode)), main, true,
            ChronosSettings.Action.VALUE, mx, my);
        y = row(graphics, x, y, Component.translatable("chronos.timemod.menu.radius"), s.global ? "—" : String.valueOf(s.radius), !s.global,
            ChronosSettings.Action.RADIUS, mx, my);
        if (s.mode == ChronosSettings.Mode.FAST || s.mode == ChronosSettings.Mode.SLOW) {
            y = row(graphics, x, y, Component.translatable("chronos.timemod.menu.duration"), s.seconds() + " s", true,
                ChronosSettings.Action.DURATION, mx, my);
        }
        y += 4;
        boolean unlocked = ChronosSettings.globalUnlocked(s.level());
        y = toggle(graphics, x, y, unlocked ? Component.translatable("chronos.timemod.menu.global")
            : Component.translatable("chronos.timemod.menu.global_locked"), s.global, unlocked, ChronosSettings.Action.GLOBAL, mx, my);
        if (s.mode == ChronosSettings.Mode.REWIND) {
            y = toggle(graphics, x, y, Component.translatable("chronos.timemod.menu.self"), s.self, true, ChronosSettings.Action.SELF, mx, my);
        }
        if (s.mask != 0) {
            Component reset = Component.translatable("chronos.timemod.menu.filters").append(": ").append(Component.translatable("chronos.timemod.menu.default"));
            boolean hover = mx >= x && mx < x + font.width(reset) + 6 && my >= y && my < y + 12;
            graphics.fill(x, y, x + font.width(reset) + 6, y + 12, hover ? 0x80FFFFFF : 0x40FFFFFF);
            graphics.drawString(font, reset, x + 3, y + 2, 0xFFFFFF);
            hotspots.add(new Hotspot(x, y, x + font.width(reset) + 6, y + 12, () -> ChronosClient.send(ChronosSettings.Action.RESET_FILTER, 0), null));
        }
    }

    private int row(GuiGraphics graphics, int x, int y, Component label, String value, boolean enabled, ChronosSettings.Action action,
                    double mx, double my) {
        graphics.drawString(font, label, x, y + 2, 0x9A9AAE);
        int bx = x + PANEL_WIDTH - 34;
        graphics.drawString(font, value, bx - 6 - font.width(value), y + 2, enabled ? 0xFFFFFF : 0x6A6A78);
        button(graphics, bx, y, "‹", enabled, mx, my, () -> ChronosClient.send(action, -1));
        button(graphics, bx + 14, y, "›", enabled, mx, my, () -> ChronosClient.send(action, 1));
        return y + 15;
    }

    private void button(GuiGraphics graphics, int x, int y, String text, boolean enabled, double mx, double my, Runnable action) {
        boolean hover = enabled && mx >= x && mx < x + 12 && my >= y && my < y + 12;
        graphics.fill(x, y, x + 12, y + 12, !enabled ? 0x30FFFFFF : hover ? 0xC0FFFFFF : 0x60FFFFFF);
        graphics.drawCenteredString(font, text, x + 6, y + 2, hover ? 0x101018 : 0xFFFFFF);
        if (enabled) {
            hotspots.add(new Hotspot(x, y, x + 12, y + 12, action, null));
        }
    }

    private int toggle(GuiGraphics graphics, int x, int y, Component label, boolean on, boolean enabled, ChronosSettings.Action action,
                       double mx, double my) {
        boolean hover = enabled && mx >= x && mx < x + PANEL_WIDTH - 6 && my >= y && my < y + 12;
        graphics.fill(x, y + 1, x + 10, y + 11, enabled ? 0xFF2A2A36 : 0xFF1A1A22);
        if (on) {
            graphics.fill(x + 2, y + 3, x + 8, y + 9, 0xFF6FF2C2);
        }
        if (hover) {
            graphics.fill(x - 1, y, x + 11, y + 1, 0xFFFFFFFF);
            graphics.fill(x - 1, y + 11, x + 11, y + 12, 0xFFFFFFFF);
        }
        graphics.drawString(font, label, x + 14, y + 2, enabled ? 0xFFFFFF : 0x6A6A78);
        if (enabled) {
            hotspots.add(new Hotspot(x, y, x + PANEL_WIDTH - 6, y + 12, () -> ChronosClient.send(action, 0), null));
        }
        return y + 14;
    }

    /** Linke Spalte: was es kostet, wie viel Chronon da ist, Meisterschaft, Regeln beim Zurückspulen. */
    private void info(GuiGraphics graphics, ChronosSettings s) {
        ItemStack watch = minecraft != null && minecraft.player != null ? ChronosWatchItem.inHand(minecraft.player) : ItemStack.EMPTY;
        int x = Math.round(cx - FILTER_R - FILTER_SIZE - 16 - PANEL_WIDTH);
        int y = cy - 56;
        graphics.fill(x - 6, y - 6, x + PANEL_WIDTH, y + 112, 0xA0101018);
        graphics.drawString(font, ChronosWatchItem.modeName(s.mode).copy().withStyle(ChatFormatting.BOLD), x, y, ChronosStyle.color(s.mode));
        y += 12;
        graphics.drawString(font, Component.translatable("tooltip.timemod.chronos.cost", Chronon.format(ChronosWatchItem.cost(s))), x, y,
            ChronosStyle.CHRONON);
        y += 11;
        graphics.drawString(font, Chronon.format(Chronon.stored(watch)) + " / " + Chronon.format(ChronosWatchItem.CAPACITY), x, y, 0xBFEFFF);
        y += 10;
        ChronosStyle.bar(graphics, x, y, PANEL_WIDTH - 8, Chronon.stored(watch) / (float) ChronosWatchItem.CAPACITY, ChronosStyle.CHRONON);
        y += 8;
        graphics.drawString(font, Component.translatable("chronos.timemod.level." + s.level()), x, y, ChronosStyle.GOLD);
        y += 10;
        ChronosStyle.bar(graphics, x, y, PANEL_WIDTH - 8, s.levelProgress(), ChronosStyle.GOLD);
        y += 8;
        if (s.mode == ChronosSettings.Mode.REWIND) {
            boolean full = RewindSession.isFull(s.effectiveMask());
            Component rule = Component.translatable(full ? "chronos.timemod.menu.full" : "chronos.timemod.menu.partial");
            for (FormattedCharSequence line : font.split(rule, PANEL_WIDTH - 6)) {
                graphics.drawString(font, line, x, y, full ? 0x6FF2C2 : 0xFFC46A);
                y += 9;
            }
            ChronosHud.timeline(graphics, font, s, cx, cy + Math.round(FILTER_R) + 28);
        }
    }

    private void scaledCentered(GuiGraphics graphics, Component text, float x, float y, float size, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(size, size, 1);
        graphics.drawCenteredString(font, text, 0, 0, color);
        graphics.pose().popPose();
    }

    private static String modeKey(ChronosSettings.Mode mode) {
        return mode.name().toLowerCase(java.util.Locale.ROOT);
    }

    // ------------------------------------------------------------------ Eingaben

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double mx = layoutX(mouseX);
        double my = layoutY(mouseY);
        for (Hotspot spot : hotspots) {
            if (spot.action() != null && spot.contains(mx, my)) {
                spot.action().run();
                return true;
            }
        }
        int mode = modeAt(mx, my, false);
        if (mode >= 0) {
            ChronosClient.send(ChronosSettings.Action.MODE, mode);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int delta = amount > 0 ? 1 : -1;
        ChronosSettings.Action action = hasShiftDown() ? ChronosSettings.Action.RADIUS
            : hasControlDown() ? ChronosSettings.Action.DURATION : ChronosSettings.Action.VALUE;
        ChronosClient.send(action, delta);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_4) {
            ChronosClient.send(ChronosSettings.Action.MODE, key - GLFW.GLFW_KEY_1);
            return true;
        }
        if (!held && ChronosClient.MENU.matches(key, scan)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    /** Waffenrad: Loslassen über einem Sektor wählt ihn; kurzes Antippen lässt das Menü offen. */
    @Override
    public boolean keyReleased(int key, int scan, int modifiers) {
        if (held && ChronosClient.MENU.matches(key, scan) && minecraft != null) {
            held = false;
            double mx = layoutX(minecraft.mouseHandler.xpos() * width / minecraft.getWindow().getScreenWidth());
            double my = layoutY(minecraft.mouseHandler.ypos() * height / minecraft.getWindow().getScreenHeight());
            int mode = modeAt(mx, my, true);
            ChronosSettings s = settings();
            if (mode >= 0) {
                if (s != null && s.mode.ordinal() != mode) {
                    ChronosClient.send(ChronosSettings.Action.MODE, mode);
                }
                onClose();
            } else if (Util.getMillis() - openedAt > TAP_MILLIS) {
                onClose();
            }
            return true;
        }
        return super.keyReleased(key, scan, modifiers);
    }
}
