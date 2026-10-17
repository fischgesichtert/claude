package com.fischgesichtert.timemod.mixin.client;

import com.fischgesichtert.timemod.client.era.EraEffects;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Der Dunst der Epochen (Schneetreiben der Eiszeit, feuchte Urzeit-Luft). */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @Inject(method = "setupFog", at = @At("TAIL"))
    private static void timemod$eraHaze(Camera camera, FogRenderer.FogMode mode, float renderDistance, boolean thickFog, float partialTick,
                                        CallbackInfo ci) {
        EraEffects.applyHaze(camera, mode, renderDistance, thickFog);
    }
}
