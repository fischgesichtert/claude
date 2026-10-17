package com.fischgesichtert.timemod.mixin.client;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface ParticleAccessor {
    @Accessor("x")
    double timemod$getX();

    @Accessor("y")
    double timemod$getY();

    @Accessor("z")
    double timemod$getZ();

    @Accessor("xo")
    void timemod$setXo(double value);

    @Accessor("yo")
    void timemod$setYo(double value);

    @Accessor("zo")
    void timemod$setZo(double value);
}
