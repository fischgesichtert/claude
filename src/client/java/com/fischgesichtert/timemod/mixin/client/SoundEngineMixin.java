package com.fischgesichtert.timemod.mixin.client;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.client.ClientTimeEngine;
import com.fischgesichtert.timemod.client.effect.TimeStopEffect;
import com.fischgesichtert.timemod.time.TimeLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Klänge klingen wie ein schneller bzw. langsamer abgespieltes Video: Die Tonhöhe folgt dem
 * Zeitfaktor am Ort der Quelle. Vanilla begrenzt die Tonhöhe auf 0,5–2,0; hier geht es weiter.
 */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    @Inject(method = "calculatePitch", at = @At("HEAD"), cancellable = true)
    private void timemod$timePitch(SoundInstance sound, CallbackInfoReturnable<Float> cir) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || sound.isRelative() || sound.getAttenuation() == SoundInstance.Attenuation.NONE) {
            return;
        }
        if (!(((TimeLevel) level).timemod$getEngine() instanceof ClientTimeEngine engine) || engine.isIdle()) {
            return;
        }
        double factor = engine.soundFactor(sound.getX(), sound.getY(), sound.getZ());
        if (factor == 1.0 || factor == 0.0) {
            return;
        }
        cir.setReturnValue(Mth.clamp((float) (sound.getPitch() * factor), 0.05F, 8.0F));
    }

    /** Im Zeitstopp verstummt die Welt; nur die Klänge der Zeit selbst bleiben. */
    @Inject(method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F", at = @At("RETURN"), cancellable = true)
    private void timemod$muffle(SoundInstance sound, CallbackInfoReturnable<Float> cir) {
        float stillness = TimeStopEffect.cameraStillness();
        if (stillness <= 0.0F || TimeMod.MOD_ID.equals(sound.getLocation().getNamespace())) {
            return;
        }
        SoundSource source = sound.getSource();
        if (source == SoundSource.MUSIC || source == SoundSource.RECORDS || source == SoundSource.VOICE) {
            return;
        }
        cir.setReturnValue(cir.getReturnValue() * (1.0F - 0.88F * stillness));
    }
}
