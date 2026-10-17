package com.fischgesichtert.timemod.mixin.client;

import com.fischgesichtert.timemod.client.ClientTimeEngine;
import com.fischgesichtert.timemod.client.TimeClientState;
import com.fischgesichtert.timemod.client.era.TimeStreamClient;
import com.fischgesichtert.timemod.time.TimeFilter;
import com.fischgesichtert.timemod.time.TimeLevel;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    /** Weltwechsel im Zeitstrom: kein „Welt wird betreten“-Bild, der Strom läuft weiter. */
    @WrapOperation(method = "setLevel", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/Minecraft;updateScreenAndTick(Lnet/minecraft/client/gui/screens/Screen;)V"))
    private void timemod$streamInsteadOfProgress(Minecraft minecraft, Screen screen, Operation<Void> original) {
        original.call(minecraft, TimeStreamClient.isActive() ? TimeStreamClient.screen() : screen);
    }

    // ---------------- Eingefroren: keine Angriffe, kein Benutzen, kein Abbauen ----------------

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void timemod$frozenAttack(CallbackInfoReturnable<Boolean> cir) {
        if (TimeClientState.isLocalPlayerFrozen()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void timemod$frozenMining(boolean attacking, CallbackInfo ci) {
        if (TimeClientState.isLocalPlayerFrozen()) {
            ci.cancel();
        }
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void timemod$frozenUse(CallbackInfo ci) {
        if (TimeClientState.isLocalPlayerFrozen()) {
            ci.cancel();
        }
    }

    @Inject(method = "pickBlock", at = @At("HEAD"), cancellable = true)
    private void timemod$frozenPick(CallbackInfo ci) {
        if (TimeClientState.isLocalPlayerFrozen()) {
            ci.cancel();
        }
    }
}
