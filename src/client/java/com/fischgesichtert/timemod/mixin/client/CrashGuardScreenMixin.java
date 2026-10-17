package com.fischgesichtert.timemod.mixin.client;

import com.fischgesichtert.timemod.client.ClientSafety;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Absturzschutz für Bildschirme: Minecraft macht aus jedem Fehler beim Zeichnen, Klicken oder Tippen
 * einen Absturz. Ein Bildschirm von TIME schließt sich stattdessen; bei fremden Bildschirmen wird nur
 * ein Fehler abgefangen, den TIME verursacht hat (etwa ein Tooltip der Mod).
 */
@Mixin(Screen.class)
abstract class CrashGuardScreenMixin {
    @WrapOperation(method = "wrapScreenError", at = @At(value = "INVOKE", target = "Ljava/lang/Runnable;run()V"))
    private static void timemod$guardScreen(Runnable action, Operation<Void> original, @Local(argsOnly = true, ordinal = 0) String what,
                                            @Local(argsOnly = true, ordinal = 1) String screenName) {
        try {
            original.call(action);
        } catch (Throwable error) {
            if (!ClientSafety.absorbScreen(what, screenName, error)) {
                throw error;
            }
        }
    }
}
