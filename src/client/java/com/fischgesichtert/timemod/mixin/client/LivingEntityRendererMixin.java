package com.fischgesichtert.timemod.mixin.client;

import com.fischgesichtert.timemod.client.ClientAging;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Gealterte Wesen (Lebenszeit-Extraktor) werden blass und grau-bläulich. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @WrapOperation(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"))
    private void timemod$fadeWithAge(EntityModel<?> model, PoseStack pose, VertexConsumer consumer, int light, int overlay,
                                     float r, float g, float b, float a, Operation<Void> original, @Local(argsOnly = true) LivingEntity entity) {
        float age = ClientAging.age(entity.getId());
        if (age > 0.0F) {
            float fade = 1.0F - 0.38F * age;
            r *= fade;
            g *= fade * 1.03F;
            b *= Math.min(1.0F, fade * 1.15F);
        }
        original.call(model, pose, consumer, light, overlay, r, g, b, a);
    }
}
