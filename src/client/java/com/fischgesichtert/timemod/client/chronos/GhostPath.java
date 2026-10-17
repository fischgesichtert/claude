package com.fischgesichtert.timemod.client.chronos;

import com.fischgesichtert.timemod.chronos.ChronosSettings;
import com.fischgesichtert.timemod.chronos.ChronosWatchItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Der Geisterpfad: Im Rückspul-Modus zeigt eine leuchtende Spur, wo du in den letzten Sekunden
 * warst – bis zu dem Punkt, an den die Welt zurückspringen würde. Der Client merkt sich dafür
 * selbst die eigene Position der letzten Minute.
 */
final class GhostPath {
    private static final int CAPACITY = 1200;
    /** Größere Sprünge (Teleport, Enderperle) werden nicht verbunden. */
    private static final double MAX_STEP = 4.0;
    private static final double[] X = new double[CAPACITY];
    private static final double[] Y = new double[CAPACITY];
    private static final double[] Z = new double[CAPACITY];
    private static int head;
    private static int size;
    @Nullable
    private static ResourceKey<Level> dimension;

    private GhostPath() {
    }

    static void clear() {
        head = 0;
        size = 0;
    }

    static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            clear();
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        if (mc.level.dimension() != dimension) {
            clear();
            dimension = mc.level.dimension();
        }
        X[head] = mc.player.getX();
        Y[head] = mc.player.getY();
        Z[head] = mc.player.getZ();
        head = (head + 1) % CAPACITY;
        size = Math.min(CAPACITY, size + 1);
    }

    private static int index(int ago) {
        return Math.floorMod(head - 1 - ago, CAPACITY);
    }

    static void render(WorldRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || context.consumers() == null || mc.options.hideGui) {
            return;
        }
        ItemStack watch = ChronosWatchItem.inHand(mc.player);
        if (watch.isEmpty()) {
            return;
        }
        ChronosSettings s = ChronosSettings.of(watch);
        if (s.mode != ChronosSettings.Mode.REWIND) {
            return;
        }
        int memory = ChronosClient.availableTicks() + (ChronosClient.isScrubbing() ? ChronosClient.done : 0);
        int depth = Math.min(Math.min(s.rewindSeconds * 20, memory), size - 1);
        if (depth < 2) {
            return;
        }
        Vec3 cam = context.camera().getPosition();
        PoseStack pose = context.matrixStack();
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        float time = (mc.level.getGameTime() + context.tickDelta()) / 6.0F;
        // Erst alle Flächen, dann alle Linien: Ein Puffer gilt nur, bis der nächste angefordert wird
        VertexConsumer quads = context.consumers().getBuffer(RenderType.debugQuads());
        for (int ago = 0; ago < depth; ago++) {
            int a = index(ago);
            int b = index(ago + 1);
            double dx = X[b] - X[a];
            double dz = Z[b] - Z[a];
            double flat = Math.sqrt(dx * dx + dz * dz);
            if (flat < 1.0E-4 || flat > MAX_STEP) {
                continue;
            }
            float f = ago / (float) depth;
            float px = (float) (-dz / flat * 0.16);
            float pz = (float) (dx / flat * 0.16);
            float ax = (float) (X[a] - cam.x);
            float ay = (float) (Y[a] + 0.04 - cam.y);
            float az = (float) (Z[a] - cam.z);
            float bx = (float) (X[b] - cam.x);
            float by = (float) (Y[b] + 0.04 - cam.y);
            float bz = (float) (Z[b] - cam.z);
            float alpha = 0.18F + 0.37F * f;
            quads.vertex(m, ax - px, ay, az - pz).color(red(f), green(f), blue(f), alpha).endVertex();
            quads.vertex(m, ax + px, ay, az + pz).color(red(f), green(f), blue(f), alpha).endVertex();
            quads.vertex(m, bx + px, by, bz + pz).color(red(f), green(f), blue(f), alpha).endVertex();
            quads.vertex(m, bx - px, by, bz - pz).color(red(f), green(f), blue(f), alpha).endVertex();
        }
        int target = index(depth);
        marker(quads, m, X[target] - cam.x, Y[target] - cam.y, Z[target] - cam.z, time);
        VertexConsumer lines = context.consumers().getBuffer(RenderType.lines());
        for (int ago = 0; ago < depth; ago++) {
            int a = index(ago);
            int b = index(ago + 1);
            double dx = X[b] - X[a];
            double dy = Y[b] - Y[a];
            double dz = Z[b] - Z[a];
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1.0E-4 || len > MAX_STEP) {
                continue;
            }
            float f = ago / (float) depth;
            float alpha = 0.45F + 0.5F * f;
            float nx = (float) (dx / len);
            float ny = (float) (dy / len);
            float nz = (float) (dz / len);
            lines.vertex(m, (float) (X[a] - cam.x), (float) (Y[a] + 0.06 - cam.y), (float) (Z[a] - cam.z)).color(red(f), green(f), blue(f), alpha)
                .normal(n, nx, ny, nz).endVertex();
            lines.vertex(m, (float) (X[b] - cam.x), (float) (Y[b] + 0.06 - cam.y), (float) (Z[b] - cam.z)).color(red(f), green(f), blue(f), alpha)
                .normal(n, nx, ny, nz).endVertex();
        }
    }

    /** Farbverlauf von minzgrün (jetzt) zu violett (Ziel). */
    private static float red(float f) {
        return 0.30F + 0.55F * f;
    }

    private static float green(float f) {
        return 0.95F - 0.50F * f;
    }

    private static float blue(float f) {
        return 0.70F + 0.30F * f;
    }

    /** Am Ziel: ein Lichtpfeiler und ein pulsierender Ring am Boden. */
    private static void marker(VertexConsumer quads, Matrix4f m, double x, double y, double z, float time) {
        float pulse = 0.8F + 0.2F * (float) Math.sin(time);
        float w = 0.16F;
        float top = 3.0F;
        for (int side = 0; side < 2; side++) {
            float ox = side == 0 ? w : 0;
            float oz = side == 0 ? 0 : w;
            quads.vertex(m, (float) (x - ox), (float) y, (float) (z - oz)).color(0.85F, 0.55F, 1.0F, 0.85F).endVertex();
            quads.vertex(m, (float) (x + ox), (float) y, (float) (z + oz)).color(0.85F, 0.55F, 1.0F, 0.85F).endVertex();
            quads.vertex(m, (float) (x + ox), (float) (y + top), (float) (z + oz)).color(0.85F, 0.55F, 1.0F, 0.0F).endVertex();
            quads.vertex(m, (float) (x - ox), (float) (y + top), (float) (z - oz)).color(0.85F, 0.55F, 1.0F, 0.0F).endVertex();
        }
        int segments = 32;
        float inner = 0.45F * pulse;
        float outer = 0.85F * pulse;
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2 * i / segments;
            double a1 = Math.PI * 2 * (i + 1) / segments;
            float c0 = (float) Math.cos(a0);
            float s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);
            float ry = (float) (y + 0.05);
            quads.vertex(m, (float) x + c0 * inner, ry, (float) z + s0 * inner).color(0.85F, 0.55F, 1.0F, 0.8F).endVertex();
            quads.vertex(m, (float) x + c0 * outer, ry, (float) z + s0 * outer).color(0.85F, 0.55F, 1.0F, 0.25F).endVertex();
            quads.vertex(m, (float) x + c1 * outer, ry, (float) z + s1 * outer).color(0.85F, 0.55F, 1.0F, 0.25F).endVertex();
            quads.vertex(m, (float) x + c1 * inner, ry, (float) z + s1 * inner).color(0.85F, 0.55F, 1.0F, 0.8F).endVertex();
        }
    }
}
