package com.fischgesichtert.timemod.client;

import com.fischgesichtert.timemod.client.effect.TimeStopEffect;
import com.fischgesichtert.timemod.registry.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** Die Stille im Zeitstopp: tiefes Brummen, Herzschlag, fernes Ticken – solange man drin steht. */
public final class StopAmbienceSound extends AbstractTickableSoundInstance {
    private int fadeOut = -1;

    public StopAmbienceSound() {
        super(ModSounds.STOP_AMBIENCE, SoundSource.AMBIENT, RandomSource.create());
        this.looping = true;
        this.delay = 0;
        this.volume = 0.9F;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public void tick() {
        if (TimeStopEffect.cameraStillness() > 0.0F) {
            fadeOut = -1;
            volume = 0.9F;
            return;
        }
        if (fadeOut < 0) {
            fadeOut = 10;
        }
        volume = 0.9F * fadeOut / 10.0F;
        if (fadeOut-- <= 0) {
            stop();
        }
    }
}
