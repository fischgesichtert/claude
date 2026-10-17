package com.fischgesichtert.timemod.client.screen;

import com.fischgesichtert.timemod.era.Era;
import com.fischgesichtert.timemod.era.Eras;
import com.fischgesichtert.timemod.menu.TimeCapsuleMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Die Zeitkapsel: eine einreihige Truhe mit einer Zeile darunter. Eine gefundene Kapsel nennt dort, wer
 * sie versiegelt hat; eine neue erklärt, wie man sie auf die Reise schickt.
 */
public class TimeCapsuleScreen extends AbstractContainerScreen<TimeCapsuleMenu> {
    private static final ResourceLocation CHEST = new ResourceLocation("textures/gui/container/generic_54.png");
    /** Titelzeile und eine Reihe Plätze im Truhen-Hintergrund. */
    private static final int TOP = 35;
    private static final int SEPIA = 0x7A5230;

    public TimeCapsuleScreen(TimeCapsuleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 132 + TimeCapsuleMenu.TEXT_HEIGHT;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(CHEST, leftPos, topPos, 0, 0, imageWidth, TOP);
        // Die glatte Fläche aus der Titelzeile streckt den Hintergrund um die Textzeile
        graphics.blit(CHEST, leftPos, topPos + TOP, 0, 4, imageWidth, TimeCapsuleMenu.TEXT_HEIGHT);
        graphics.blit(CHEST, leftPos, topPos + TOP + TimeCapsuleMenu.TEXT_HEIGHT, 0, 126, imageWidth, 96);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        Component line = line();
        int width = font.width(line);
        float scale = Math.min(1.0F, (imageWidth - 16) / (float) width);
        graphics.pose().pushPose();
        graphics.pose().translate(imageWidth / 2.0F, TOP + 2.5F, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, line, -width / 2, 0, SEPIA, false);
        graphics.pose().popPose();
    }

    private Component line() {
        if (menu.years() > 0) {
            return Component.translatable("container.timemod.time_capsule.sender", menu.sender());
        }
        Minecraft mc = Minecraft.getInstance();
        Era era = mc.level == null ? null : Eras.byLevel(mc.level.dimension());
        return Component.translatable(era != null && era.isPast() ? "container.timemod.time_capsule.hint" : "container.timemod.time_capsule.hint_here");
    }
}
