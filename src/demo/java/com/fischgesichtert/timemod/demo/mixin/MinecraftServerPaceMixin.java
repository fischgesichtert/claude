package com.fischgesichtert.timemod.demo.mixin;

import com.fischgesichtert.timemod.demo.DemoPace;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Nur Demo: Der Server tickt langsamer, damit auch ein langsamer Software-Renderer jeden Tick zeigt. */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerPaceMixin {
    @ModifyConstant(method = "runServer", constant = @Constant(longValue = 50L))
    private long timemod$demoTickLength(long original) {
        return DemoPace.TICK_MS;
    }
}
