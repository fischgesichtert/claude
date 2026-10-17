package com.fischgesichtert.timemod.client.era;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.client.render.PortalColors;
import com.fischgesichtert.timemod.era.Era;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Der Flug durch den Zeitstrom: Zahnringe rasen aus der Tiefe heran, Lichtschlieren ziehen vorbei, und
 * in der Mitte zählt ein riesiger Zähler die Jahre bis zum Ziel. Kurz vor der Landung wird alles weiß.
 */
public class TimeStreamScreen extends Screen {
    static final int FADE_TICKS = 24;
    private static final ResourceLocation OUTER = TimeMod.id("textures/misc/gate_ring_outer.png");
    private static final ResourceLocation INNER = TimeMod.id("textures/misc/gate_ring_inner.png");
    private static final ResourceLocation GLOW = TimeMod.id("textures/misc/gate_glow.png");
    private static final int STREAKS = 160;
    private static final int RINGS = 8;
    private static final float[] ANGLE = new float[STREAKS];
    private static final float[] OFFSET = new float[STREAKS];
    private static final float[] SPEED = new float[STREAKS];

    static {
        Random random = new Random(20261017L);
        for (int i = 0; i < STREAKS; i++) {
            ANGLE[i] = random.nextFloat() * Mth.TWO_PI;
            OFFSET[i] = random.nextFloat();
            SPEED[i] = 0.35F + random.nextFloat() * 0.5F;
        }
    }

    public TimeStreamScreen() {
        super(Component.translatable("stream.timemod.title"));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (TimeStreamClient.arrived()) {
            // Die neue Welt erscheint, das Licht klingt ab
            float fade = 1.0F - TimeStreamClient.fade(partialTick);
            graphics.fill(0, 0, width, height, (int) (255 * fade * fade) << 24 | 0xFFFFFF);
            return;
        }
        float t = TimeStreamClient.progress(partialTick);
        float seconds = TimeStreamClient.seconds(partialTick);
        // Anlauf, Reisetempo, Abbremsen
        float speed = t < 0.25F ? 0.3F + 0.7F * (t / 0.25F) * (t / 0.25F) : (t > 0.85F ? 1.0F - 0.6F * (t - 0.85F) / 0.15F : 1.0F);
        float travelled = seconds * speed;
        Era target = TimeStreamClient.target();
        int tint = PortalColors.of(target);
        float r = (tint >> 16 & 255) / 255.0F;
        float g = (tint >> 8 & 255) / 255.0F;
        float b = (tint & 255) / 255.0F;
        int cx = width / 2;
        int cy = height / 2;
        float reach = (float) Math.hypot(width, height) / 2;

        graphics.fillGradient(0, 0, width, height, 0xFF0B0718, 0xFF140A2C);
        RenderSystem.enableBlend();
        // Fernes Leuchten in der Farbe der Epoche
        RenderSystem.setShaderColor(r, g, b, 0.55F);
        int glow = (int) (Math.min(width, height) * (0.9F + 0.08F * Mth.sin(seconds * 3.1F)));
        graphics.blit(GLOW, cx - glow, cy - glow, glow * 2, glow * 2, 0, 0, 64, 64, 64, 64);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        streaks(graphics, cx, cy, reach, travelled, seconds, r, g, b);
        rings(graphics, cx, cy, travelled, seconds);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.55F);
        int core = (int) (Math.min(width, height) * (0.10F + 0.02F * Mth.sin(seconds * 7.0F)));
        graphics.blit(GLOW, cx - core, cy - core, core * 2, core * 2, 0, 0, 64, 64, 64, 64);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        counter(graphics, cx, cy, t, target);
        if (t > 0.88F) {
            float flash = Math.min(1.0F, (t - 0.88F) / 0.12F);
            graphics.fill(0, 0, width, height, (int) (255 * flash * flash) << 24 | 0xFFFFFF);
        }
        RenderSystem.disableBlend();
    }

    /** Lichtschlieren, die aus der Mitte nach außen schießen. */
    private void streaks(GuiGraphics graphics, int cx, int cy, float reach, float travelled, float seconds, float r, float g, float b) {
        PoseStack pose = graphics.pose();
        for (int i = 0; i < STREAKS; i++) {
            float phase = (OFFSET[i] + travelled * SPEED[i]) % 1.0F;
            float distance = phase * phase * reach;
            float length = 2 + 70 * phase * phase * phase;
            int alpha = (int) (230 * Math.min(1.0F, phase * 1.6F));
            boolean white = i % 5 == 0;
            int color = alpha << 24 | (white ? 0xFFFFFF : ((int) (r * 200 + 55) << 16 | (int) (g * 200 + 55) << 8 | (int) (b * 200 + 55)));
            pose.pushPose();
            pose.translate(cx, cy, 0);
            pose.mulPose(Axis.ZP.rotation(ANGLE[i] + seconds * 0.05F));
            int thickness = phase > 0.6F ? 2 : 1;
            graphics.fill((int) distance, 0, (int) (distance + length), thickness, color);
            pose.popPose();
        }
    }

    /** Zahnringe, die aus der Tiefe auf den Reisenden zurasen. */
    private void rings(GuiGraphics graphics, int cx, int cy, float travelled, float seconds) {
        PoseStack pose = graphics.pose();
        float base = Math.min(width, height) * 0.05F;
        for (int k = 0; k < RINGS; k++) {
            float phase = (travelled * 0.45F + k / (float) RINGS) % 1.0F;
            float radius = base / Math.max(0.04F, 1.0F - phase);
            // Ein- und vor dem Vorbeirasen wieder ausblenden, damit keine riesigen Klötze das Bild füllen
            float alpha = Math.min(1.0F, phase / 0.25F) * (phase > 0.7F ? Math.max(0.0F, (0.92F - phase) / 0.22F) : 1.0F);
            if (alpha <= 0.01F || radius > width * 1.2F) {
                continue;
            }
            boolean brass = k % 2 == 0;
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha * (brass ? 0.85F : 1.0F));
            pose.pushPose();
            pose.translate(cx, cy, 0);
            pose.mulPose(Axis.ZP.rotationDegrees(k * 37.0F + seconds * (brass ? 24.0F : -38.0F)));
            int size = (int) radius;
            int texture = brass ? 144 : 112;
            graphics.blit(brass ? OUTER : INNER, -size, -size, size * 2, size * 2, 0, 0, texture, texture, texture, texture);
            pose.popPose();
        }
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    /** Der riesige Jahreszähler und darunter, kurz vor dem Ziel, der Name der Epoche. */
    private void counter(GuiGraphics graphics, int cx, int cy, float t, Era target) {
        String number = format(TimeStreamClient.yearsAt(t));
        Component label = Component.translatable("stream.timemod.year");
        PoseStack pose = graphics.pose();
        float scale = Math.min(4.0F, width * 0.8F / Math.max(1, font.width(number)));
        pose.pushPose();
        pose.translate(cx, cy - 4 * scale - 6, 0);
        pose.scale(scale, scale, 1.0F);
        int left = -font.width(number) / 2;
        // Dunkle Kontur, damit der Zähler auch vor hellem Licht lesbar bleibt
        for (int[] offset : new int[][] {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
            graphics.drawString(font, number, left + offset[0], offset[1], 0xC01A0E2E, false);
        }
        graphics.drawString(font, number, left, 0, 0xFFF6E0, false);
        pose.popPose();
        graphics.drawString(font, label, cx - font.width(label) / 2, (int) (cy - 4 * scale - 18), 0xC8D8FF, true);
        if (t > 0.6F) {
            int alpha = (int) (255 * Math.min(1.0F, (t - 0.6F) / 0.25F));
            Component title = target.title();
            pose.pushPose();
            pose.translate(cx, cy + 4 * scale + 4, 0);
            pose.scale(1.6F, 1.6F, 1.0F);
            graphics.drawString(font, title, -font.width(title) / 2, 0, Math.max(4, alpha) << 24 | 0xFFD27A, true);
            pose.popPose();
        }
    }

    /** −66.000.000 auf Deutsch, −66,000,000 auf Englisch. */
    private static String format(double years) {
        long value = Math.round(years);
        String code = Minecraft.getInstance().options.languageCode;
        NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.forLanguageTag(code.replace('_', '-')));
        String digits = numbers.format(Math.abs(value));
        return value < 0 ? "−" + digits : value > 0 ? "+" + digits : digits;
    }
}
