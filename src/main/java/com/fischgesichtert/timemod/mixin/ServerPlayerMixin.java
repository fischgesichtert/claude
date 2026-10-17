package com.fischgesichtert.timemod.mixin;

import com.fischgesichtert.timemod.time.TimeEngine;
import com.fischgesichtert.timemod.time.TimeHitQueue;
import com.fischgesichtert.timemod.time.TimeLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Eingefrorene Spieler altern nicht: kein Hunger, keine Regeneration, keine ablaufenden Effekte. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(method = "doTick", at = @At("HEAD"), cancellable = true)
    private void timemod$freeze(CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        TimeEngine engine = ((TimeLevel) self.level()).timemod$getEngine();
        if (engine != null && !engine.isIdle() && engine.isFrozenPlayer(self)) {
            ci.cancel();
            return;
        }
        ((TimeHitQueue) self).timemod$releaseHits();
    }
}
