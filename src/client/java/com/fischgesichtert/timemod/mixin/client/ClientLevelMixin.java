package com.fischgesichtert.timemod.mixin.client;

import com.fischgesichtert.timemod.client.ClientTimeEngine;
import com.fischgesichtert.timemod.time.TimeFilter;
import com.fischgesichtert.timemod.time.TimeLevel;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    @Unique
    private ClientTimeEngine timemod$engine() {
        return (ClientTimeEngine) ((TimeLevel) this).timemod$getEngine();
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void timemod$createEngine(CallbackInfo ci) {
        ((TimeLevel) this).timemod$setEngine(new ClientTimeEngine((ClientLevel) (Object) this));
    }

    @Inject(method = "tickEntities", at = @At("HEAD"))
    private void timemod$beginTick(CallbackInfo ci) {
        ClientTimeEngine engine = timemod$engine();
        if (!engine.isIdle()) {
            engine.beginTick();
        }
    }

    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void timemod$tickEntity(Entity entity, CallbackInfo ci) {
        ClientTimeEngine engine = timemod$engine();
        if (engine.isIdle() || engine.isReentrant()) {
            return;
        }
        ci.cancel();
        ClientLevel self = (ClientLevel) (Object) this;
        engine.tickEntity(entity, () -> self.tickNonPassenger(entity));
    }

    /** Globale Zeit: Sonne, Mond und Spielzeit des Clients laufen im selben Tempo wie der Server. */
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;tickTime()V"))
    private void timemod$tickTime(ClientLevel level, Operation<Void> original) {
        ClientTimeEngine engine = timemod$engine();
        int steps = engine.isIdle() ? 1 : engine.worldSteps(TimeFilter.DAYTIME);
        for (int i = 0; i < steps; i++) {
            original.call(level);
        }
    }
}
