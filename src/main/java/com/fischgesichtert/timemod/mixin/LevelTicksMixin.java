package com.fischgesichtert.timemod.mixin;

import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.TimeLevel;
import com.fischgesichtert.timemod.time.TimeTicks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Leitet geplante Ticks in Zeitzonen um, damit sie dort in Zonen-Eigenzeit laufen. */
@Mixin(LevelTicks.class)
public abstract class LevelTicksMixin<T> implements TimeTicks {
    @Unique
    private ServerLevel timemod$level;
    @Unique
    private boolean timemod$fluid;

    @Override
    public void timemod$bind(ServerLevel level, boolean fluid) {
        timemod$level = level;
        timemod$fluid = fluid;
    }

    @Unique
    private ServerTimeEngine timemod$engine() {
        return timemod$level == null ? null : (ServerTimeEngine) ((TimeLevel) timemod$level).timemod$getEngine();
    }

    @Inject(method = "schedule", at = @At("HEAD"), cancellable = true)
    private void timemod$schedule(ScheduledTick<T> tick, CallbackInfo ci) {
        ServerTimeEngine engine = timemod$engine();
        if (engine != null && engine.routeSchedule(tick, timemod$fluid)) {
            ci.cancel();
        }
    }

    @Inject(method = "hasScheduledTick", at = @At("HEAD"), cancellable = true)
    private void timemod$hasScheduledTick(BlockPos pos, T type, CallbackInfoReturnable<Boolean> cir) {
        ServerTimeEngine engine = timemod$engine();
        if (engine != null && engine.hasZoneTick(pos, type, timemod$fluid)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "willTickThisTick", at = @At("HEAD"), cancellable = true)
    private void timemod$willTickThisTick(BlockPos pos, T type, CallbackInfoReturnable<Boolean> cir) {
        ServerTimeEngine engine = timemod$engine();
        if (engine != null && engine.willZoneTickThisStep(pos, type, timemod$fluid)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "addContainer", at = @At("TAIL"))
    private void timemod$addContainer(ChunkPos chunkPos, LevelChunkTicks<T> container, CallbackInfo ci) {
        ServerTimeEngine engine = timemod$engine();
        if (engine != null) {
            engine.onContainerAdded(chunkPos, container, timemod$fluid);
        }
    }

    @Inject(method = "removeContainer", at = @At("HEAD"))
    private void timemod$removeContainer(ChunkPos chunkPos, CallbackInfo ci) {
        ServerTimeEngine engine = timemod$engine();
        if (engine != null) {
            engine.onContainerRemoved(chunkPos, timemod$fluid);
        }
    }
}
