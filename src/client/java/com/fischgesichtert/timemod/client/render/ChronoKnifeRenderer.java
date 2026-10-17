package com.fischgesichtert.timemod.client.render;

import com.fischgesichtert.timemod.entity.ThrownChronoKnife;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;

/** Zeichnet das Messer selbst (als Item-Modell), mit der Klinge in Flugrichtung. */
public class ChronoKnifeRenderer extends EntityRenderer<ThrownChronoKnife> {
    private final ItemRenderer itemRenderer;

    public ChronoKnifeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ThrownChronoKnife knife, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, knife.yRotO, knife.getYRot()) - 90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, knife.xRotO, knife.getXRot()) - 45.0F));
        pose.scale(0.9F, 0.9F, 0.9F);
        itemRenderer.renderStatic(knife.knife(), ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers, knife.level(), knife.getId());
        pose.popPose();
        super.render(knife, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownChronoKnife knife) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
