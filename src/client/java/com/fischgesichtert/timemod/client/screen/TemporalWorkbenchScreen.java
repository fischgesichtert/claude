package com.fischgesichtert.timemod.client.screen;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.menu.TemporalWorkbenchMenu;
import com.fischgesichtert.timemod.module.ModularTool;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Oberfläche der Temporal-Werkbank: Werkzeug, Modulplätze und die resultierenden Werte. */
public class TemporalWorkbenchScreen extends AbstractContainerScreen<TemporalWorkbenchMenu> {
    private static final ResourceLocation TEXTURE = TimeMod.id("textures/gui/container/temporal_workbench.png");
    private static final int PANEL_X = 76;
    private static final int PANEL_Y = 16;
    private static final int PANEL_W = 92;

    public TemporalWorkbenchScreen(TemporalWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        // Gesperrte Modulplätze abdunkeln (Schloss-Kachel aus der Textur)
        int active = menu.activeModuleSlots();
        for (int i = 0; i < TemporalWorkbenchMenu.MODULE_SLOTS; i++) {
            if (i >= active) {
                graphics.blit(TEXTURE, leftPos + TemporalWorkbenchMenu.MODULE_X - 1, topPos + TemporalWorkbenchMenu.MODULE_Y[i] - 1, 176, 0, 18, 18);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        ItemStack tool = menu.tool();
        int y = PANEL_Y + 3;
        if (tool.getItem() instanceof ModularTool modular) {
            for (FormattedCharSequence line : font.split(tool.getHoverName(), PANEL_W - 6)) {
                graphics.drawString(font, line, PANEL_X + 3, y, 0xFFE2A0, false);
                y += 10;
            }
            for (Component stat : modular.moduleStats(tool)) {
                for (FormattedCharSequence line : font.split(stat, PANEL_W - 6)) {
                    if (y > PANEL_Y + 52) {
                        return;
                    }
                    graphics.drawString(font, line, PANEL_X + 3, y, 0x9FE8FF, false);
                    y += 9;
                }
            }
        } else {
            for (FormattedCharSequence line : font.split(Component.translatable("gui.timemod.workbench.hint"), PANEL_W - 6)) {
                graphics.drawString(font, line, PANEL_X + 3, y, 0x8C8CA8, false);
                y += 9;
            }
        }
    }
}
