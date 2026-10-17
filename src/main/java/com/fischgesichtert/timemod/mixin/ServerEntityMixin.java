package com.fischgesichtert.timemod.mixin;

import com.fischgesichtert.timemod.time.TimeEngine;
import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.TimeLevel;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Vorgespulte Entities werden jeden Tick übertragen, sonst sähe ihre Bewegung ruckelig aus. */
@Mixin(ServerEntity.class)
public abstract class ServerEntityMixin {
    @Shadow
    @Final
    private Entity entity;

    @ModifyExpressionValue(method = "sendChanges", at = @At(value = "FIELD", target = "Lnet/minecraft/server/level/ServerEntity;updateInterval:I"))
    private int timemod$fastSync(int interval) {
        TimeEngine engine = ((TimeLevel) entity.level()).timemod$getEngine();
        if (engine instanceof ServerTimeEngine server && server.wantsFastSync(entity)) {
            return 1;
        }
        return interval;
    }
}
