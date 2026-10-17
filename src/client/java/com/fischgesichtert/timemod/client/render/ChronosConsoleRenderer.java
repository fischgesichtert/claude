package com.fischgesichtert.timemod.client.render;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.block.entity.ChronosConsoleBlockEntity;
import com.fischgesichtert.timemod.gate.ChronosGate;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Die Zahnrad-Ringe des Chronos-Tors: zwei Messingringe vor und hinter dem Rahmen, die sich
 * gegenläufig drehen, und ein leuchtender Chronit-Ring, der beim Aufladen erwacht. Im letzten
 * Moment sammelt sich in der Mitte das Licht der Öffnung.
 */
public class ChronosConsoleRenderer implements BlockEntityRenderer<ChronosConsoleBlockEntity> {
    private static final ResourceLocation OUTER = TimeMod.id("textures/misc/gate_ring_outer.png");
    private static final ResourceLocation INNER = TimeMod.id("textures/misc/gate_ring_inner.png");
    private static final ResourceLocation GLOW = TimeMod.id("textures/misc/gate_glow.png");
    private static final float OUTER_RADIUS = 4.6F;
    private static final float INNER_RADIUS = 3.5F;

    public ChronosConsoleRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ChronosConsoleBlockEntity console, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ChronosGate.Shape gate = console.gate();
        if (gate == null || console.getLevel() == null) {
            return;
        }
        Vec3 center = gate.center();
        BlockPos origin = console.getBlockPos();
        float angle = console.ringAngle(partialTick);
        float glow = console.glow(partialTick);
        int worldLight = LevelRenderer.getLightColor(console.getLevel(), BlockPos.containing(center).relative(gate.axis() == Direction.Axis.X
            ? Direction.SOUTH : Direction.EAST));
        // Beim Aufladen beginnen auch die Messingringe zu glühen
        int ringLight = glow > 0.5F ? LightTexture.FULL_BRIGHT : worldLight;
        pose.pushPose();
        pose.translate(center.x - origin.getX(), center.y - origin.getY(), center.z - origin.getZ());
        if (gate.axis() == Direction.Axis.Z) {
            pose.mulPose(Axis.YP.rotationDegrees(90));
        }
        VertexConsumer brass = buffers.getBuffer(RenderType.entityCutoutNoCull(OUTER));
        disc(pose, brass, OUTER_RADIUS, 0.56F, angle, 1.0F, ringLight);
        disc(pose, brass, OUTER_RADIUS, -0.56F, -angle, 1.0F, ringLight);
        if (glow > 0.02F) {
            VertexConsumer chronite = buffers.getBuffer(RenderType.entityTranslucentEmissive(INNER));
            disc(pose, chronite, INNER_RADIUS, 0.6F, -1.5F * angle, glow, LightTexture.FULL_BRIGHT);
            disc(pose, chronite, INNER_RADIUS, -0.6F, 1.5F * angle, glow, LightTexture.FULL_BRIGHT);
            VertexConsumer light2 = buffers.getBuffer(RenderType.entityTranslucentEmissive(GLOW));
            float pulse = 0.85F + 0.15F * (float) Math.sin(angle * 0.05F);
            disc(pose, light2, 0.4F + 2.4F * glow * glow * pulse, 0.0F, angle * 0.3F, Math.min(1.0F, glow * 1.2F), LightTexture.FULL_BRIGHT);
        }
        pose.popPose();
    }

    /** Ein runder, texturierter Ring als Quadrat in der Ebene des Tors, um {@code spin} gedreht. */
    private static void disc(PoseStack pose, VertexConsumer consumer, float radius, float depth, float spin, float alpha, int light) {
        pose.pushPose();
        pose.translate(0, 0, depth);
        pose.mulPose(Axis.ZP.rotationDegrees(spin));
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        vertex(consumer, m, n, -radius, -radius, 0, 1, alpha, light);
        vertex(consumer, m, n, radius, -radius, 1, 1, alpha, light);
        vertex(consumer, m, n, radius, radius, 1, 0, alpha, light);
        vertex(consumer, m, n, -radius, radius, 0, 0, alpha, light);
        pose.popPose();
    }

    private static void vertex(VertexConsumer consumer, Matrix4f m, Matrix3f n, float x, float y, float u, float v, float alpha, int light) {
        consumer.vertex(m, x, y, 0).color(1.0F, 1.0F, 1.0F, alpha).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
            .normal(n, 0, 0, 1).endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(ChronosConsoleBlockEntity console) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
