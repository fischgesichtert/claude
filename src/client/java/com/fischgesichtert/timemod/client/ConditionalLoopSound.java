package com.fischgesichtert.timemod.client;

import java.util.function.BooleanSupplier;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** Ein Klang, der in Schleife läuft, solange eine Bedingung gilt, und dann sanft ausblendet. */
public final class ConditionalLoopSound extends AbstractTickableSoundInstance {
    private final BooleanSupplier condition;
    private final float fullVolume;
    private int fadeOut = -1;

    public ConditionalLoopSound(SoundEvent sound, float volume, BooleanSupplier condition) {
        super(sound, SoundSource.AMBIENT, RandomSource.create());
        this.condition = condition;
        this.fullVolume = volume;
        this.looping = true;
        this.delay = 0;
        this.volume = volume;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public void tick() {
        if (condition.getAsBoolean()) {
            fadeOut = -1;
            volume = fullVolume;
            return;
        }
        if (fadeOut < 0) {
            fadeOut = 10;
        }
        volume = fullVolume * fadeOut / 10.0F;
        if (fadeOut-- <= 0) {
            stop();
        }
    }
}
