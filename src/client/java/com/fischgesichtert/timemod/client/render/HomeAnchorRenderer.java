package com.fischgesichtert.timemod.client.render;

import com.fischgesichtert.timemod.block.entity.HomeAnchorBlockEntity;
import com.fischgesichtert.timemod.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Über dem Heimkehr-Anker schwebt ein leuchtender Chronit-Kristall – weithin sichtbar. */
public class HomeAnchorRenderer implements BlockEntityRenderer<HomeAnchorBlockEntity> {
    private static final ItemStack CRYSTAL = new ItemStack(ModItems.CHRONITE_CRYSTAL);

    public HomeAnchorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(HomeAnchorBlockEntity anchor, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (anchor.getLevel() == null) {
            return;
        }
        float time = anchor.getLevel().getGameTime() + partialTick;
        pose.pushPose();
        pose.translate(0.5, 1.25 + 0.08 * Math.sin(time / 12.0), 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(time * 3.0F));
        pose.scale(0.75F, 0.75F, 0.75F);
        Minecraft.getInstance().getItemRenderer().renderStatic(CRYSTAL, ItemDisplayContext.GROUND, LightTexture.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY, pose, buffers, anchor.getLevel(), 0);
        pose.popPose();
    }
}
