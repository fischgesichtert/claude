package com.fischgesichtert.timemod.client.render;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.block.TemporalWorkbenchBlock;
import com.fischgesichtert.timemod.block.entity.TemporalWorkbenchBlockEntity;
import com.fischgesichtert.timemod.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Über der erwachten Temporal-Werkbank kreisen drei Messingringe gegeneinander; in ihrer Mitte
 * schwebt das Werkzeug (oder ein Chronit-Kristall).
 */
public class TemporalWorkbenchRenderer implements BlockEntityRenderer<TemporalWorkbenchBlockEntity> {
    private static final int FULL_BRIGHT = 0xF000F0;
    private final ItemRenderer itemRenderer;
    private final ItemStack crystal = new ItemStack(ModItems.CHRONITE_CRYSTAL);

    public TemporalWorkbenchRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(TemporalWorkbenchBlockEntity workbench, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (workbench.getLevel() == null) {
            return;
        }
        boolean formed = workbench.getBlockState().getValue(TemporalWorkbenchBlock.FORMED);
        float time = workbench.getLevel().getGameTime() + partialTick;
        ItemStack shown = workbench.tool().isEmpty() ? (formed ? crystal : ItemStack.EMPTY) : workbench.tool();

        if (!shown.isEmpty()) {
            pose.pushPose();
            float height = formed ? 1.45F + (float) Math.sin(time / 14.0) * 0.06F : 0.95F;
            pose.translate(0.5, height, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(formed ? time * 2.5F : 0.0F));
            if (!formed) {
                pose.mulPose(Axis.XP.rotationDegrees(90));
            }
            pose.scale(0.6F, 0.6F, 0.6F);
            itemRenderer.renderStatic(shown, ItemDisplayContext.FIXED, formed ? FULL_BRIGHT : light, OverlayTexture.NO_OVERLAY, pose, buffers,
                workbench.getLevel(), (int) workbench.getBlockPos().asLong());
            pose.popPose();
        }
        if (!formed) {
            return;
        }
        TextureAtlasSprite brass = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(TimeMod.id("block/brass_block"));
        TextureAtlasSprite chronite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(TimeMod.id("block/chrono_pillar_core"));
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        pose.pushPose();
        pose.translate(0.5, 1.45, 0.5);
        ring(pose, buffer, brass, 1.12F, 56, 22.0F, 0.0F, time * 1.6F, 0.028F);
        ring(pose, buffer, brass, 0.88F, 44, -38.0F, 30.0F, -time * 2.4F, 0.024F);
        ring(pose, buffer, chronite, 0.6F, 32, 70.0F, -20.0F, time * 4.0F, 0.022F);
        pose.popPose();
    }

    /** Ein Ring aus kleinen Segmenten, gekippt und um die eigene Achse gedreht. */
    private static void ring(PoseStack pose, VertexConsumer buffer, TextureAtlasSprite sprite, float radius, int segments,
                             float tiltX, float tiltZ, float spin, float thickness) {
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(tiltX));
        pose.mulPose(Axis.ZP.rotationDegrees(tiltZ));
        pose.mulPose(Axis.YP.rotationDegrees(spin));
        // Fast geschlossener Reif, jedes vierte Segment trägt einen Zahn nach außen
        float length = (float) (2 * Math.PI * radius / segments) * 0.96F;
        for (int i = 0; i < segments; i++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(360.0F * i / segments));
            pose.translate(radius, 0, 0);
            box(pose, buffer, sprite, new AABB(-thickness, -thickness * 1.6F, -length / 2, thickness, thickness * 1.6F, length / 2));
            if (i % 4 == 0) {
                box(pose, buffer, sprite, new AABB(thickness, -thickness, -length * 0.3F, thickness * 3.2F, thickness, length * 0.3F));
            }
            pose.popPose();
        }
        pose.popPose();
    }

    private static void box(PoseStack pose, VertexConsumer buffer, TextureAtlasSprite sprite, AABB b) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        float x0 = (float) b.minX, y0 = (float) b.minY, z0 = (float) b.minZ;
        float x1 = (float) b.maxX, y1 = (float) b.maxY, z1 = (float) b.maxZ;
        float u0 = sprite.getU(4), u1 = sprite.getU(12), v0 = sprite.getV(4), v1 = sprite.getV(12);
        quad(buffer, m, n, sprite, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, 0, 1, 0, u0, v0, u1, v1);
        quad(buffer, m, n, sprite, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, 0, -1, 0, u0, v0, u1, v1);
        quad(buffer, m, n, sprite, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, 1, 0, 0, u0, v0, u1, v1);
        quad(buffer, m, n, sprite, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0, -1, 0, 0, u0, v0, u1, v1);
        quad(buffer, m, n, sprite, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, 0, 0, -1, u0, v0, u1, v1);
        quad(buffer, m, n, sprite, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1, 0, 0, 1, u0, v0, u1, v1);
    }

    private static void quad(VertexConsumer buffer, Matrix4f m, Matrix3f n, TextureAtlasSprite sprite,
                             float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz,
                             float nx, float ny, float nz, float u0, float v0, float u1, float v1) {
        vertex(buffer, m, n, ax, ay, az, u0, v1, nx, ny, nz);
        vertex(buffer, m, n, bx, by, bz, u0, v0, nx, ny, nz);
        vertex(buffer, m, n, cx, cy, cz, u1, v0, nx, ny, nz);
        vertex(buffer, m, n, dx, dy, dz, u1, v1, nx, ny, nz);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f m, Matrix3f n, float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        buffer.vertex(m, x, y, z).color(255, 255, 255, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT)
            .normal(n, nx, ny, nz).endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(TemporalWorkbenchBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 64;
    }
}
