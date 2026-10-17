package com.fischgesichtert.timemod.client.effect;

import com.fischgesichtert.timemod.armor.ChronoArmorItem;
import com.fischgesichtert.timemod.time.TimeLevel;
import com.fischgesichtert.timemod.time.TimeZone;
import com.fischgesichtert.timemod.time.ZoneShape;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Zeitsicht des Chrono-Helms: Alle Zeitzonen werden als schimmernde Umrisse sichtbar – orange für
 * schneller, blau für langsamer, weiß für Stillstand, violett beim Zurückspulen.
 */
public final class ZoneSight {
    private static final int SEGMENTS = 64;

    private ZoneSight() {
    }

    public static void render(WorldRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !ChronoArmorItem.wearsHelmet(mc.player) || context.consumers() == null) {
            return;
        }
        var engine = ((TimeLevel) mc.level).timemod$getEngine();
        if (engine == null || engine.zones().isEmpty()) {
            return;
        }
        Vec3 cam = context.camera().getPosition();
        PoseStack pose = context.matrixStack();
        VertexConsumer lines = context.consumers().getBuffer(RenderType.lines());
        float pulse = 0.65F + 0.35F * (float) Math.sin((mc.level.getGameTime() + context.tickDelta()) / 8.0);
        for (TimeZone zone : engine.zones()) {
            float[] rgb = color(zone);
            if (zone.shape() instanceof ZoneShape.Sphere sphere) {
                Vec3 c = sphere.center().subtract(cam);
                float r = (float) sphere.radius();
                pose.pushPose();
                pose.translate(c.x, c.y, c.z);
                circles(pose, lines, r, rgb, pulse);
                pose.popPose();
            } else if (zone.shape().bounds() != null && !zone.shape().isGlobal()) {
                AABB box = zone.shape().bounds().move(-cam.x, -cam.y, -cam.z);
                LevelRenderer.renderLineBox(pose, lines, box, rgb[0], rgb[1], rgb[2], pulse);
            }
        }
    }

    private static float[] color(TimeZone zone) {
        if (zone.isRewind()) {
            return new float[] {0.85F, 0.35F, 1.0F};
        }
        if (zone.isStop()) {
            return new float[] {0.95F, 0.95F, 1.0F};
        }
        return zone.factor() > 1.0 ? new float[] {1.0F, 0.62F, 0.18F} : new float[] {0.3F, 0.7F, 1.0F};
    }

    /** Drei Großkreise (Äquator und zwei Meridiane) – genug, um die Kugel lesbar zu machen. */
    private static void circles(PoseStack pose, VertexConsumer lines, float r, float[] rgb, float alpha) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        for (int axis = 0; axis < 3; axis++) {
            for (int i = 0; i < SEGMENTS; i++) {
                double a0 = 2 * Math.PI * i / SEGMENTS;
                double a1 = 2 * Math.PI * (i + 1) / SEGMENTS;
                float[] p0 = point(axis, r, a0);
                float[] p1 = point(axis, r, a1);
                float nx = p1[0] - p0[0];
                float ny = p1[1] - p0[1];
                float nz = p1[2] - p0[2];
                float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                lines.vertex(m, p0[0], p0[1], p0[2]).color(rgb[0], rgb[1], rgb[2], alpha).normal(n, nx / len, ny / len, nz / len).endVertex();
                lines.vertex(m, p1[0], p1[1], p1[2]).color(rgb[0], rgb[1], rgb[2], alpha).normal(n, nx / len, ny / len, nz / len).endVertex();
            }
        }
    }

    private static float[] point(int axis, float r, double a) {
        float c = (float) (Math.cos(a) * r);
        float s = (float) (Math.sin(a) * r);
        return switch (axis) {
            case 0 -> new float[] {c, 0, s};
            case 1 -> new float[] {c, s, 0};
            default -> new float[] {0, c, s};
        };
    }
}
