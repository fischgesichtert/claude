package com.fischgesichtert.timemod.mixin.client;

import com.fischgesichtert.timemod.client.ClientSafety;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Absturzschutz: Eine Maschine der Mod, die sich nicht zeichnen lässt, fehlt in diesem Bild. */
@Mixin(BlockEntityRenderDispatcher.class)
abstract class CrashGuardBlockEntityRenderMixin {
    @WrapOperation(method = "setupAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;"
        + "render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"))
    private static void timemod$guardBlockEntityRender(BlockEntityRenderer<BlockEntity> renderer, BlockEntity blockEntity, float partialTick,
                                                       PoseStack pose, MultiBufferSource buffers, int light, int overlay, Operation<Void> original) {
        int depth = ClientSafety.depth(pose);
        try {
            original.call(renderer, blockEntity, partialTick, pose, buffers, light, overlay);
        } catch (Throwable error) {
            if (!ClientSafety.absorbBlockEntityRender(renderer, blockEntity, error)) {
                throw error;
            }
            ClientSafety.restore(pose, depth);
        }
    }
}
