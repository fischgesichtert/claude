package com.fischgesichtert.timemod.demo.mixin;

import com.fischgesichtert.timemod.demo.DemoPace;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Nur Demo: langsamer Takt und Bilder exakt auf dem Tick (für ruckelfreie Videos). */
@Mixin(Minecraft.class)
public abstract class MinecraftPaceMixin {
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Timer;<init>(FJ)V"), index = 0)
    private float timemod$demoTicksPerSecond(float ticksPerSecond) {
        return 1000.0F / DemoPace.TICK_MS;
    }

    /** Jedes Bild zeigt exakt den Stand am Tick-Anfang (Teil-Tick 0). */
    @ModifyArg(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;render(FJZ)V"), index = 0)
    private float timemod$onTickFrames(float partialTick) {
        return DemoPace.TICK_MS > 50L ? 0.0F : partialTick;
    }
}
