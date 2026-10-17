package com.fischgesichtert.timemod.client.chronos;

import com.fischgesichtert.timemod.chronos.ChronosSettings;
import com.fischgesichtert.timemod.registry.ModItems;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.math.BigDecimal;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix4f;

/** Gemeinsames Aussehen von Radialmenü und HUD: Farben, Symbole, Werte, Kreisbögen. */
final class ChronosStyle {
    /** Stopp: eisblau · Raffer: bernstein · Lupe: lila · Zurück: minzgrün. */
    static final int[] MODE_COLORS = {0x5AD8FF, 0xFFB43A, 0xC27CFF, 0x4CE6A4};
    static final int GOLD = 0xFFD66B;
    static final int CHRONON = 0x6FE3FF;

    private ChronosStyle() {
    }

    static int color(ChronosSettings.Mode mode) {
        return MODE_COLORS[mode.ordinal()];
    }

    static ItemStack icon(ChronosSettings.Mode mode) {
        Item item = switch (mode) {
            case STOP -> ModItems.STASIS_WATCH;
            case FAST -> ModItems.TIME_ACCELERATOR;
            case SLOW -> ModItems.SLOW_PENDULUM;
            case REWIND -> ModItems.REWIND_SPOOL;
        };
        return new ItemStack(item);
    }

    /** Symbole der Filter, in der Reihenfolge von {@link ChronosSettings#FILTERS}. */
    static ItemStack filterIcon(int index) {
        Item item = switch (index) {
            case 0 -> ModItems.FILTER_ANIMALS;
            case 1 -> ModItems.FILTER_MONSTERS;
            case 2 -> ModItems.FILTER_VILLAGERS;
            case 3 -> ModItems.FILTER_PLAYERS;
            case 4 -> ModItems.FILTER_PROJECTILES;
            case 5 -> ModItems.FILTER_ITEMS;
            case 6 -> ModItems.FILTER_VEHICLES;
            case 7 -> ModItems.FILTER_GROWTH;
            case 8 -> ModItems.FILTER_ENVIRONMENT;
            case 9 -> ModItems.FILTER_FLUIDS;
            case 10 -> ModItems.FILTER_REDSTONE;
            case 11 -> ModItems.FILTER_MACHINES;
            case 12 -> Items.CLOCK;
            default -> Items.LIGHTNING_ROD;
        };
        return new ItemStack(item);
    }

    static String factor(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString() + "×";
    }

    /** Der Hauptwert groß in der Mitte: „3 s“, „4×“, „0.25×“, „−15 s“. */
    static String mainValue(ChronosSettings s) {
        return switch (s.mode) {
            case STOP -> s.stopSeconds + " s";
            case FAST, SLOW -> factor(s.factor());
            case REWIND -> "−" + s.rewindSeconds + " s";
        };
    }

    /** Zweite Zeile: Wirkbereich und Dauer. */
    static Component details(ChronosSettings s) {
        Component where = s.global ? Component.translatable("tooltip.timemod.chronos.global")
            : Component.translatable("tooltip.timemod.chronos.radius", s.radius);
        return switch (s.mode) {
            case FAST, SLOW -> Component.literal(s.seconds() + " s · ").append(where);
            default -> where.copy();
        };
    }

    // ------------------------------------------------------------------ Zeichnen

    /** Gefüllter Kreisbogen (Ringsegment) von {@code a0} bis {@code a1} (Bogenmaß, 0 = rechts, im Uhrzeigersinn). */
    static void arc(GuiGraphics graphics, float cx, float cy, float r0, float r1, float a0, float a1, int argb) {
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f m = graphics.pose().last().pose();
        int a = argb >>> 24;
        int r = argb >> 16 & 255;
        int g = argb >> 8 & 255;
        int b = argb & 255;
        int steps = Math.max(2, (int) Math.ceil(Math.abs(a1 - a0) / 0.06F));
        for (int i = 0; i < steps; i++) {
            float b0 = a0 + (a1 - a0) * i / steps;
            float b1 = a0 + (a1 - a0) * (i + 1) / steps;
            buffer.vertex(m, cx + (float) Math.cos(b0) * r0, cy + (float) Math.sin(b0) * r0, 0).color(r, g, b, a).endVertex();
            buffer.vertex(m, cx + (float) Math.cos(b1) * r0, cy + (float) Math.sin(b1) * r0, 0).color(r, g, b, a).endVertex();
            buffer.vertex(m, cx + (float) Math.cos(b1) * r1, cy + (float) Math.sin(b1) * r1, 0).color(r, g, b, a).endVertex();
            buffer.vertex(m, cx + (float) Math.cos(b0) * r1, cy + (float) Math.sin(b0) * r1, 0).color(r, g, b, a).endVertex();
        }
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    static void disc(GuiGraphics graphics, float cx, float cy, float r, int argb) {
        arc(graphics, cx, cy, 0, r, 0, (float) (Math.PI * 2), argb);
    }

    /** Schmaler Balken mit Rahmen (Chronon, Meisterschaft). */
    static void bar(GuiGraphics graphics, int x, int y, int width, float fill, int color) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + 3, 0xC0101018);
        graphics.fill(x, y, x + Math.round(width * Math.max(0, Math.min(1, fill))), y + 2, 0xFF000000 | color);
    }

    static int withAlpha(int rgb, int alpha) {
        return alpha << 24 | (rgb & 0xFFFFFF);
    }
}
