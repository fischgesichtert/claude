package com.fischgesichtert.timemod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemDisplayContext;

/** Geschoss als Item, das der Kamera zugewandt rotiert (Zahnrad-Säge, Zeitegel). */
public class SpinningItemRenderer<T extends Entity & ItemSupplier> extends EntityRenderer<T> {
    private final ItemRenderer itemRenderer;
    private final float scale;
    private final float spin;
    private final boolean fullBright;

    public SpinningItemRenderer(EntityRendererProvider.Context context, float scale, float spin, boolean fullBright) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.scale = scale;
        this.spin = spin;
        this.fullBright = fullBright;
    }

    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.scale(scale, scale, scale);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Axis.ZP.rotationDegrees((entity.tickCount + partialTick) * spin));
        itemRenderer.renderStatic(entity.getItem(), ItemDisplayContext.GROUND, fullBright ? 0xF000F0 : light, OverlayTexture.NO_OVERLAY,
            pose, buffers, entity.level(), entity.getId());
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
