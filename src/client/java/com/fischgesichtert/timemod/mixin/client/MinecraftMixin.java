package com.fischgesichtert.timemod.mixin.client;

import com.fischgesichtert.timemod.client.ClientTimeEngine;
import com.fischgesichtert.timemod.time.TimeFilter;
import com.fischgesichtert.timemod.time.TimeLevel;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Bei globaler Zeit laufen auch Wolken, Regen und animierte Texturen (Wasser, Lava, Feuer) mit. */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow
    @Nullable
    public ClientLevel level;

    @Unique
    private int timemod$globalSteps() {
        if (level != null && ((TimeLevel) level).timemod$getEngine() instanceof ClientTimeEngine engine && !engine.isIdle()) {
            return engine.worldSteps(TimeFilter.ENVIRONMENT);
        }
        return 1;
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;tick()V"))
    private void timemod$levelRendererTick(LevelRenderer renderer, Operation<Void> original) {
        int steps = timemod$globalSteps();
        for (int i = 0; i < steps; i++) {
            original.call(renderer);
        }
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureManager;tick()V"))
    private void timemod$textureTick(TextureManager textures, Operation<Void> original) {
        int steps = timemod$globalSteps();
        for (int i = 0; i < steps; i++) {
            original.call(textures);
        }
    }
}
