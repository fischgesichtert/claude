package com.fischgesichtert.timemod.mixin.client;

import com.fischgesichtert.timemod.client.ClientTimeEngine;
import com.fischgesichtert.timemod.time.TimeLevel;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Zeitlupe gleitet weich, gestoppte Entities stehen exakt still. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @ModifyVariable(method = "renderEntity", at = @At("HEAD"), argsOnly = true)
    private float timemod$remapPartialTick(float partialTick, @Local(argsOnly = true) Entity entity) {
        if (((TimeLevel) entity.level()).timemod$getEngine() instanceof ClientTimeEngine engine) {
            return engine.remapPartialTick(entity, partialTick);
        }
        return partialTick;
    }
}
