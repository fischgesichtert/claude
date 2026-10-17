package com.fischgesichtert.timemod.mixin.client;

import com.fischgesichtert.timemod.client.ClientTimeEngine;
import com.fischgesichtert.timemod.time.RenderTimeState;
import com.fischgesichtert.timemod.time.TimeLevel;
import com.fischgesichtert.timemod.time.TimeZone;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Partikel laufen im Tempo ihrer Zone: Funken rasen, Rauch schwebt, im Stopp hängt alles in der Luft. */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @Unique
    private static final int REWIND_PARTICLE_STEPS = 4;

    @Shadow
    protected ClientLevel level;

    @Unique
    private ClientTimeEngine timemod$engine() {
        return level != null && ((TimeLevel) level).timemod$getEngine() instanceof ClientTimeEngine engine && !engine.isIdle() ? engine : null;
    }

    @WrapOperation(method = "tickParticleList", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;tickParticle(Lnet/minecraft/client/particle/Particle;)V"))
    private void timemod$tickParticle(ParticleEngine self, Particle particle, Operation<Void> original) {
        ClientTimeEngine engine = timemod$engine();
        RenderTimeState render = (RenderTimeState) particle;
        if (engine == null) {
            render.timemod$setRenderTime(1.0F, 0.0F);
            original.call(self, particle);
            return;
        }
        ParticleAccessor access = (ParticleAccessor) particle;
        double x = access.timemod$getX();
        double y = access.timemod$getY();
        double z = access.timemod$getZ();
        TimeZone zone = engine.particleZone(x, y, z);
        if (zone == null) {
            render.timemod$setRenderTime(1.0F, 0.0F);
            original.call(self, particle);
            return;
        }
        if (zone.isRewind()) {
            // Was zurückgespult wird, hat es nie gegeben: Partikel vergehen rasch, statt hängen zu bleiben
            render.timemod$setRenderTime(1.0F, 0.0F);
            for (int i = 0; i < REWIND_PARTICLE_STEPS && particle.isAlive(); i++) {
                original.call(self, particle);
            }
            return;
        }
        render.timemod$setRenderTime((float) engine.effectiveFactor(zone), (float) zone.phase());
        int steps = zone.steps();
        for (int i = 0; i < steps && particle.isAlive(); i++) {
            original.call(self, particle);
        }
        if (steps > 1) {
            access.timemod$setXo(x);
            access.timemod$setYo(y);
            access.timemod$setZo(z);
        }
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;render(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/Camera;F)V"))
    private void timemod$renderParticle(Particle particle, VertexConsumer consumer, Camera camera, float partialTick, Operation<Void> original) {
        RenderTimeState render = (RenderTimeState) particle;
        float remapped = RenderTimeState.remap(partialTick, render.timemod$getRenderRate(), render.timemod$getRenderPhase());
        original.call(particle, consumer, camera, remapped);
    }
}
