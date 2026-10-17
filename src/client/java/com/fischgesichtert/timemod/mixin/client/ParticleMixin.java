package com.fischgesichtert.timemod.mixin.client;

import com.fischgesichtert.timemod.time.RenderTimeState;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Particle.class)
public abstract class ParticleMixin implements RenderTimeState {
    @Unique
    private float timemod$renderRate = 1.0F;
    @Unique
    private float timemod$renderPhase;

    @Override
    public float timemod$getRenderRate() {
        return timemod$renderRate;
    }

    @Override
    public float timemod$getRenderPhase() {
        return timemod$renderPhase;
    }

    @Override
    public void timemod$setRenderTime(float rate, float phase) {
        timemod$renderRate = rate;
        timemod$renderPhase = phase;
    }
}
