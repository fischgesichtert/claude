package com.fischgesichtert.timemod.mixin;

import com.fischgesichtert.timemod.time.RenderTimeState;
import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.TimeAware;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin implements TimeAware, RenderTimeState {
    @Unique
    private static final String TIMEMOD_KEY = "timemod:Eigenzeit";

    @Unique
    private long timemod$eigenzeit;
    @Unique
    private float timemod$renderRate = 1.0F;
    @Unique
    private float timemod$renderPhase;

    @Override
    public long timemod$getEigenzeit() {
        return timemod$eigenzeit;
    }

    @Override
    public void timemod$setEigenzeit(long ticks) {
        timemod$eigenzeit = ticks;
    }

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

    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void timemod$save(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        if (timemod$eigenzeit != 0) {
            tag.putLong(TIMEMOD_KEY, timemod$eigenzeit);
        }
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void timemod$load(CompoundTag tag, CallbackInfo ci) {
        timemod$eigenzeit = tag.getLong(TIMEMOD_KEY);
    }

    /** Zeitjournal: Verschwindet eine Entity (getötet, entfernt, Dimension gewechselt), ihren Zustand sichern. */
    @Inject(method = "setRemoved", at = @At("HEAD"))
    private void timemod$recordRemoval(Entity.RemovalReason reason, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.level() instanceof ServerLevel serverLevel && !self.isRemoved()) {
            ServerTimeEngine engine = ServerTimeEngine.of(serverLevel);
            if (engine != null) {
                engine.journal().onEntityRemoved(self, reason);
            }
        }
    }
}
