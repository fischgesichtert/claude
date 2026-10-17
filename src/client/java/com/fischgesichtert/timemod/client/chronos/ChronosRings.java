package com.fischgesichtert.timemod.client.chronos;

import com.fischgesichtert.timemod.TimeMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Die schwebenden Ringe der Chronos-Uhr: drei leuchtende Ziffernringe, die sich in verschiedenen
 * Ebenen um die Uhr drehen – in der Hand, am Boden und im Rahmen.
 */
public final class ChronosRings {
    private static final ResourceLocation TEXTURE = TimeMod.id("textures/misc/chronos_ring.png");
    private static final int SEGMENTS = 40;
    private static final int FULL_BRIGHT = 0xF000F0;

    private ChronosRings() {
    }

    public static boolean shows(ItemDisplayContext context) {
        return context != ItemDisplayContext.GUI && context != ItemDisplayContext.NONE;
    }

    /** {@code pose} steht im Modellraum des Items (Würfel 0..1, Mitte bei 0,5). */
    public static void render(PoseStack pose, MultiBufferSource buffers) {
        float t = (Util.getMillis() % 3_600_000L) / 1000.0F;
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        ring(pose, consumer, 0.47F, 0.036F, 72, 10, t * 42, 1.0F, 1.0F, 1.0F, 0.95F);
        ring(pose, consumer, 0.55F, 0.028F, -38, 55, -t * 28, 0.70F, 0.95F, 1.0F, 0.80F);
        ring(pose, consumer, 0.40F, 0.024F, 18, -62, t * 66, 1.0F, 0.88F, 0.55F, 0.85F);
        pose.popPose();
    }

    private static void ring(PoseStack pose, VertexConsumer consumer, float radius, float width, float tiltX, float tiltZ, float spin,
                             float r, float g, float b, float a) {
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(tiltZ));
        pose.mulPose(Axis.XP.rotationDegrees(tiltX));
        pose.mulPose(Axis.YP.rotationDegrees(spin));
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        float inner = radius - width / 2;
        float outer = radius + width / 2;
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = (float) (Math.PI * 2 * i / SEGMENTS);
            float a1 = (float) (Math.PI * 2 * (i + 1) / SEGMENTS);
            float u0 = 4.0F * i / SEGMENTS;
            float u1 = 4.0F * (i + 1) / SEGMENTS;
            float c0 = (float) Math.cos(a0);
            float s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);
            // Oberseite und Unterseite, damit der Ring von beiden Seiten leuchtet
            vertex(consumer, m, n, c0 * inner, s0 * inner, u0, 1, 1, r, g, b, a);
            vertex(consumer, m, n, c0 * outer, s0 * outer, u0, 0, 1, r, g, b, a);
            vertex(consumer, m, n, c1 * outer, s1 * outer, u1, 0, 1, r, g, b, a);
            vertex(consumer, m, n, c1 * inner, s1 * inner, u1, 1, 1, r, g, b, a);
            vertex(consumer, m, n, c1 * inner, s1 * inner, u1, 1, -1, r, g, b, a);
            vertex(consumer, m, n, c1 * outer, s1 * outer, u1, 0, -1, r, g, b, a);
            vertex(consumer, m, n, c0 * outer, s0 * outer, u0, 0, -1, r, g, b, a);
            vertex(consumer, m, n, c0 * inner, s0 * inner, u0, 1, -1, r, g, b, a);
        }
        pose.popPose();
    }

    private static void vertex(VertexConsumer consumer, Matrix4f m, Matrix3f n, float x, float z, float u, float v, float ny,
                               float r, float g, float b, float a) {
        consumer.vertex(m, x, 0, z).color(r, g, b, a).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT)
            .normal(n, 0, ny, 0).endVertex();
    }
}
