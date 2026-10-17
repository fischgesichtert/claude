package com.fischgesichtert.timemod.mixin.client;

import com.fischgesichtert.timemod.chronos.ChronosSettings;
import com.fischgesichtert.timemod.chronos.ChronosWatchItem;
import com.fischgesichtert.timemod.client.TimeClientState;
import com.fischgesichtert.timemod.client.chronos.ChronosClient;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    /** Im fremden Zeitstopp lässt sich nicht einmal der Blick bewegen. */
    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void timemod$frozenLook(CallbackInfo ci) {
        if (TimeClientState.isLocalPlayerFrozen()) {
            ci.cancel();
        }
    }

    /** Schleichen + Mausrad mit der Chronos-Uhr verstellt den Hauptwert statt den Hotbar-Platz. */
    @WrapOperation(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;swapPaint(D)V"))
    private void timemod$chronosWheel(Inventory inventory, double direction, Operation<Void> original) {
        if (minecraft.player != null && minecraft.player.isShiftKeyDown() && !ChronosWatchItem.inHand(minecraft.player).isEmpty()) {
            ChronosClient.send(ChronosSettings.Action.VALUE, direction > 0 ? 1 : -1);
            return;
        }
        original.call(inventory, direction);
    }
}
