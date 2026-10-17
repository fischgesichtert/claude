package com.fischgesichtert.timemod.client.screen;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.menu.ClockmakerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Oberfläche der Uhrmacher-Werkbank. */
public class ClockmakerScreen extends AbstractContainerScreen<ClockmakerMenu> {
    private static final ResourceLocation TEXTURE = TimeMod.id("textures/gui/container/clockmaker_bench.png");

    public ClockmakerScreen(ClockmakerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        titleLabelX = 29;
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
    }
}
