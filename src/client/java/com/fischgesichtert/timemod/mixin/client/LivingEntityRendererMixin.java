package com.fischgesichtert.timemod.mixin.client;

import com.fischgesichtert.timemod.client.ClientAging;
import com.fischgesichtert.timemod.entity.boss.ParadoxBoss;
import com.fischgesichtert.timemod.entity.boss.ParadoxEcho;
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

/** Gealterte Wesen (Lebenszeit-Extraktor) werden blass und grau-bläulich; Paradox und seine Echos violett. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @WrapOperation(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"))
    private void timemod$fadeWithAge(EntityModel<?> model, PoseStack pose, VertexConsumer consumer, int light, int overlay,
                                     float r, float g, float b, float a, Operation<Void> original, @Local(argsOnly = true) LivingEntity entity) {
        if (entity instanceof ParadoxBoss) {
            // Paradox: kühl violett getönt, wie ein Spiegelbild
            r *= 0.86F;
            g *= 0.78F;
        } else if (entity instanceof ParadoxEcho) {
            r *= 0.7F;
            g *= 0.6F;
            a *= 0.55F;
        }
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
