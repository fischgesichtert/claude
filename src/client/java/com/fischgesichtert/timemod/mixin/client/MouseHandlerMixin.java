package com.fischgesichtert.timemod.mixin.client;

import com.fischgesichtert.timemod.client.TimeClientState;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Im fremden Zeitstopp lässt sich nicht einmal der Blick bewegen. */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void timemod$frozenLook(CallbackInfo ci) {
        if (TimeClientState.isLocalPlayerFrozen()) {
            ci.cancel();
        }
    }
}
