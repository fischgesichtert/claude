package com.fischgesichtert.timemod.client.screen;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.block.entity.ChronosConsoleBlockEntity;
import com.fischgesichtert.timemod.block.entity.ChronosConsoleBlockEntity.GateState;
import com.fischgesichtert.timemod.block.entity.ChronosConsoleBlockEntity.Phase;
import com.fischgesichtert.timemod.energy.Chronon;
import com.fischgesichtert.timemod.era.Era;
import com.fischgesichtert.timemod.era.Eras;
import com.fischgesichtert.timemod.menu.ChronosConsoleMenu;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

/**
 * Die Chronos-Konsole: oben die Anzeige mit Zieljahr und Epoche, darunter ◀◀ ◀ [Aufladen] ▶ ▶▶,
 * dann Zellen-Platz, Chronon-Speicher und Fokus-Platz.
 */
public class ChronosConsoleScreen extends AbstractContainerScreen<ChronosConsoleMenu> {
    private static final ResourceLocation TEXTURE = TimeMod.id("textures/gui/container/chronos_console.png");
    private static final int BUTTON_Y = 76;
    private static final int[] ARROW_X = {7, 29, 127, 149};
    private static final int ARROW_W = 20;
    private static final int CHARGE_X = 53;
    private static final int CHARGE_W = 70;
    private static final int BUTTON_H = 18;
    private static final int BAR_X = 30;
    private static final int BAR_Y = 99;
    private static final int BAR_W = 116;
    private static final int BAR_H = 12;

    public ChronosConsoleScreen(ChronosConsoleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 208;
        inventoryLabelY = 115;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        Component hint = hoverHint(mouseX - leftPos, mouseY - topPos);
        if (hint != null) {
            graphics.renderTooltip(font, hint, mouseX, mouseY);
        }
    }

    @Nullable
    private Era here() {
        return minecraft == null || minecraft.level == null ? null : Eras.byLevel(minecraft.level.dimension());
    }

    private boolean idle() {
        return menu.phase() == Phase.IDLE;
    }

    private boolean arrowEnabled(int button) {
        if (!idle()) {
            return false;
        }
        int index = Eras.indexOf(menu.target());
        return button < 2 ? index > 0 : index < Eras.all().size() - 1;
    }

    private int cost() {
        Era here = here();
        return here == null ? 0 : ChronosConsoleBlockEntity.cost(here, menu.target());
    }

    private boolean canCharge() {
        Era here = here();
        return idle() && here != null && menu.gateState() == GateState.READY && menu.focusAllows(menu.target())
            && (menu.buffer() >= cost() || (minecraft != null && minecraft.player != null && minecraft.player.getAbilities().instabuild));
    }

    // ------------------------------------------------------------------ Zeichnen

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        // Pfeiltasten: Hintergrund je Zustand, darauf das Symbol
        for (int i = 0; i < 4; i++) {
            boolean enabled = arrowEnabled(i);
            boolean hover = enabled && inside(mx, my, ARROW_X[i], BUTTON_Y, ARROW_W, BUTTON_H);
            graphics.blit(TEXTURE, leftPos + ARROW_X[i], topPos + BUTTON_Y, 176, enabled ? (hover ? 18 : 0) : 36, ARROW_W, BUTTON_H);
            graphics.blit(TEXTURE, leftPos + ARROW_X[i] + 4, topPos + BUTTON_Y + 5, 196 + i * 12, enabled ? 0 : 8, 12, 8);
        }
        // Aufladen-Knopf, beim Laden mit Fortschritt
        boolean chargeEnabled = canCharge();
        boolean chargeHover = chargeEnabled && inside(mx, my, CHARGE_X, BUTTON_Y, CHARGE_W, BUTTON_H);
        graphics.blit(TEXTURE, leftPos + CHARGE_X, topPos + BUTTON_Y, chargeEnabled ? (chargeHover ? 70 : 0) : 140, 208, CHARGE_W, BUTTON_H);
        if (menu.phase() == Phase.CHARGING) {
            int fill = (int) (68 * Math.min(1.0F, menu.progress() / (float) ChronosConsoleBlockEntity.CHARGE_TICKS));
            graphics.blit(TEXTURE, leftPos + CHARGE_X + 1, topPos + BUTTON_Y + 1, 0, 226, fill, 16);
        } else if (menu.phase() == Phase.OPEN) {
            int fill = (int) (68 * Math.min(1.0F, menu.progress() / (float) ChronosConsoleBlockEntity.OPEN_TICKS));
            graphics.blit(TEXTURE, leftPos + CHARGE_X + 1, topPos + BUTTON_Y + 1, 68, 226, fill, 16);
        }
        // Chronon-Speicher; die benötigte Menge als Marke
        int buffer = menu.buffer();
        int fill = (int) (BAR_W * Math.min(1.0, buffer / (double) ChronosConsoleBlockEntity.CAPACITY));
        graphics.blit(TEXTURE, leftPos + BAR_X, topPos + BAR_Y, 0, 242, fill, BAR_H);
        int cost = cost();
        if (cost > 0) {
            int mark = BAR_X + (int) (BAR_W * Math.min(1.0, cost / (double) ChronosConsoleBlockEntity.CAPACITY));
            graphics.fill(leftPos + mark, topPos + BAR_Y - 1, leftPos + mark + 1, topPos + BAR_Y + BAR_H + 1, buffer >= cost ? 0xFF7CF29A : 0xFFFF6A5A);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x3A2A10, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
        Era target = menu.target();
        int centre = imageWidth / 2;
        // Epoche und Jahr auf der Anzeige
        Component name = target.title();
        graphics.drawString(font, name, centre - font.width(name) / 2, 21, 0xFFD27A, false);
        Component year = target.yearText();
        float scale = Math.min(2.0F, 150.0F / Math.max(1, font.width(year)));
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(centre, 33, 0);
        pose.scale(scale, scale, 1.0F);
        graphics.drawString(font, year, -font.width(year) / 2, 0, 0x9FE8FF, true);
        pose.popPose();
        int cost = cost();
        Component costLine = Component.translatable("gui.timemod.console.cost", Chronon.format(cost));
        graphics.drawString(font, costLine, centre - font.width(costLine) / 2, 52, menu.buffer() >= cost ? 0x8CF2A8 : 0xFF8C7A, false);
        Component status = status();
        graphics.drawString(font, status, centre - font.width(status) / 2, 62, statusColor(), false);
        // Knopf-Beschriftung
        Component label = switch (menu.phase()) {
            case CHARGING -> Component.translatable("gui.timemod.console.charging",
                (int) (100 * Math.min(1.0F, menu.progress() / (float) ChronosConsoleBlockEntity.CHARGE_TICKS)));
            case OPEN -> Component.translatable("gui.timemod.console.open", seconds(menu.progress()));
            default -> Component.translatable("gui.timemod.console.charge");
        };
        graphics.drawString(font, label, CHARGE_X + CHARGE_W / 2 - font.width(label) / 2, BUTTON_Y + 5, canCharge() || !idle() ? 0xFFF4D6 : 0x8A8070, true);
        Component stored = Component.literal(Chronon.format(menu.buffer()));
        graphics.drawString(font, stored, BAR_X + BAR_W / 2 - font.width(stored) / 2, BAR_Y + 2, 0xFFFFFF, true);
    }

    private static String seconds(int ticks) {
        int s = (ticks + 19) / 20;
        return s / 60 + ":" + String.format("%02d", s % 60);
    }

    private Component status() {
        if (menu.phase() == Phase.CHARGING) {
            return Component.translatable("gui.timemod.console.status.charging");
        }
        if (menu.phase() == Phase.OPEN) {
            return Component.translatable("gui.timemod.console.status.open");
        }
        return switch (menu.gateState()) {
            case NO_TIME -> Component.translatable("gui.timemod.console.status.no_time");
            case MISSING -> Component.translatable("gui.timemod.console.status.missing");
            case BLOCKED -> Component.translatable("gui.timemod.console.status.blocked");
            case READY -> !menu.focusAllows(menu.target())
                ? Component.translatable("gui.timemod.console.status.focus", Component.translatable(focusName(menu.target())))
                : Component.translatable("gui.timemod.console.status.ready");
        };
    }

    private static String focusName(Era era) {
        return era.age() == Era.Age.MESOZOIC ? "item.timemod.mesozoic_focus" : "item.timemod.era_focus";
    }

    private int statusColor() {
        if (!idle()) {
            return 0xFFE38C;
        }
        return menu.gateState() == GateState.READY && menu.focusAllows(menu.target()) ? 0x8CF2A8 : 0xFF8C7A;
    }

    @Nullable
    private Component hoverHint(int x, int y) {
        String[] keys = {"gui.timemod.console.jump_back", "gui.timemod.console.back", "gui.timemod.console.forward", "gui.timemod.console.jump_forward"};
        for (int i = 0; i < 4; i++) {
            if (inside(x, y, ARROW_X[i], BUTTON_Y, ARROW_W, BUTTON_H)) {
                return Component.translatable(keys[i]);
            }
        }
        if (inside(x, y, BAR_X, BAR_Y, BAR_W, BAR_H)) {
            return Component.translatable("gui.timemod.console.buffer", Chronon.format(menu.buffer()), Chronon.format(ChronosConsoleBlockEntity.CAPACITY));
        }
        return null;
    }

    private static boolean inside(int x, int y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    // ------------------------------------------------------------------ Eingaben

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && minecraft != null && minecraft.gameMode != null) {
            int x = (int) mouseX - leftPos;
            int y = (int) mouseY - topPos;
            for (int i = 0; i < 4; i++) {
                if (arrowEnabled(i) && inside(x, y, ARROW_X[i], BUTTON_Y, ARROW_W, BUTTON_H)) {
                    press(i);
                    return true;
                }
            }
            if (canCharge() && inside(x, y, CHARGE_X, BUTTON_Y, CHARGE_W, BUTTON_H)) {
                press(ChronosConsoleMenu.BUTTON_CHARGE);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        // Pfeiltasten der Tastatur: ←/→ eine Stufe, mit Umschalt ein Zeitalter
        boolean shift = hasShiftDown();
        if (key == 263 && arrowEnabled(shift ? 0 : 1)) {
            press(shift ? ChronosConsoleMenu.BUTTON_JUMP_BACK : ChronosConsoleMenu.BUTTON_BACK);
            return true;
        }
        if (key == 262 && arrowEnabled(shift ? 3 : 2)) {
            press(shift ? ChronosConsoleMenu.BUTTON_JUMP_FORWARD : ChronosConsoleMenu.BUTTON_FORWARD);
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    private void press(int id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) {
            mc.gameMode.handleInventoryButtonClick(menu.containerId, id);
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), id == ChronosConsoleMenu.BUTTON_CHARGE ? 0.7F : 1.4F, 0.4F));
        }
    }
}
