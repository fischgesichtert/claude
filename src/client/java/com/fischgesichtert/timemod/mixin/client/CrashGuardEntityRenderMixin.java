package com.fischgesichtert.timemod.mixin.client;

import com.fischgesichtert.timemod.client.ClientSafety;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Absturzschutz: Ein Wesen der Mod, das sich nicht zeichnen lässt, fehlt in diesem Bild – mehr nicht. */
@Mixin(EntityRenderDispatcher.class)
abstract class CrashGuardEntityRenderMixin {
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;"
        + "render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    private void timemod$guardEntityRender(EntityRenderer<Entity> renderer, Entity entity, float yaw, float partialTick, PoseStack pose,
                                           MultiBufferSource buffers, int light, Operation<Void> original) {
        int depth = ClientSafety.depth(pose);
        try {
            original.call(renderer, entity, yaw, partialTick, pose, buffers, light);
        } catch (Throwable error) {
            if (!ClientSafety.absorbEntityRender(renderer, entity, error)) {
                throw error;
            }
            ClientSafety.restore(pose, depth);
        }
    }
}
