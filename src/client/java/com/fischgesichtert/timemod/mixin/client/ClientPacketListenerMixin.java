package com.fischgesichtert.timemod.mixin.client;

import com.fischgesichtert.timemod.client.era.TimeStreamClient;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Während des Zeitstroms bleibt der Strom sichtbar, statt „Landschaft wird geladen“ zu zeigen. */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @WrapOperation(method = "handleRespawn", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"))
    private void timemod$keepStream(Minecraft minecraft, Screen screen, Operation<Void> original) {
        original.call(minecraft, TimeStreamClient.isActive() ? TimeStreamClient.screen() : screen);
    }
}
