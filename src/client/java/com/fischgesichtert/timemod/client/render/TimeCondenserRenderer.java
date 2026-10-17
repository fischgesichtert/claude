package com.fischgesichtert.timemod.client.render;

import com.fischgesichtert.timemod.block.entity.TimeCondenserBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Im Zeitkondensator schwebt das Werkzeug, das gerade geladen wird, und dreht sich langsam. */
public class TimeCondenserRenderer implements BlockEntityRenderer<TimeCondenserBlockEntity> {
    private final ItemRenderer itemRenderer;

    public TimeCondenserRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(TimeCondenserBlockEntity condenser, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemStack stack = !condenser.charging().isEmpty() ? condenser.charging() : condenser.fuel();
        if (stack.isEmpty() || condenser.getLevel() == null) {
            return;
        }
        float time = condenser.getLevel().getGameTime() + partialTick;
        pose.pushPose();
        pose.translate(0.5, 0.95 + Math.sin(time / 12.0) * 0.05, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(time * 3.0F));
        pose.scale(0.55F, 0.55F, 0.55F);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, 0xF000F0, OverlayTexture.NO_OVERLAY, pose, buffers, condenser.getLevel(), 0);
        pose.popPose();
    }
}
