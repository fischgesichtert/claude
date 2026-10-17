package com.fischgesichtert.timemod.mixin.client;

import com.fischgesichtert.timemod.chronos.ChronosWatchItem;
import com.fischgesichtert.timemod.client.chronos.ChronosRings;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Die Chronos-Uhr bekommt außerhalb des Inventars ihre schwebenden Ringe. */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @Inject(method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"))
    private void timemod$chronosRings(ItemStack stack, ItemDisplayContext context, boolean leftHand, PoseStack pose, MultiBufferSource buffers,
                                      int light, int overlay, BakedModel model, CallbackInfo ci) {
        if (stack.getItem() instanceof ChronosWatchItem && ChronosRings.shows(context)) {
            ChronosRings.render(pose, buffers);
        }
    }
}
