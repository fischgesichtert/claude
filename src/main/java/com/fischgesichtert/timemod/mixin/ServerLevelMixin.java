package com.fischgesichtert.timemod.mixin;

import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.TimeFilter;
import com.fischgesichtert.timemod.time.TimeLevel;
import com.fischgesichtert.timemod.time.TimeTicks;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.raid.Raids;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.ticks.LevelTicks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Einhängepunkte der Zeit-Engine in den Welt-Tick des Servers. */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Unique
    private ServerTimeEngine timemod$engine() {
        return (ServerTimeEngine) ((TimeLevel) this).timemod$getEngine();
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void timemod$createEngine(CallbackInfo ci) {
        ServerLevel self = (ServerLevel) (Object) this;
        ((TimeLevel) self).timemod$setEngine(new ServerTimeEngine(self));
        ((TimeTicks) self.getBlockTicks()).timemod$bind(self, false);
        ((TimeTicks) self.getFluidTicks()).timemod$bind(self, true);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void timemod$tickStart(BooleanSupplier hasTime, CallbackInfo ci) {
        timemod$engine().onLevelTickStart(hasTime);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void timemod$tickEnd(BooleanSupplier hasTime, CallbackInfo ci) {
        timemod$engine().onLevelTickEnd();
    }

    // ---------------- globale Welt-Systeme ----------------

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;advanceWeatherCycle()V"))
    private void timemod$weather(ServerLevel level, Operation<Void> original) {
        int steps = timemod$engine().worldSystemSteps(TimeFilter.WEATHER);
        for (int i = 0; i < steps; i++) {
            original.call(level);
        }
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;tickTime()V"))
    private void timemod$time(ServerLevel level, Operation<Void> original) {
        ServerTimeEngine engine = timemod$engine();
        if (!engine.worldAdvances()) {
            return;
        }
        original.call(level);
        int extraDays = engine.worldSystemSteps(TimeFilter.DAYTIME) - 1;
        if (extraDays != 0 && level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
            level.setDayTime(level.getDayTime() + extraDays);
        }
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/ticks/LevelTicks;tick(JILjava/util/function/BiConsumer;)V", ordinal = 0))
    private void timemod$blockTicks(LevelTicks<?> ticks, long time, int max, BiConsumer<?, ?> ticker, Operation<Void> original) {
        ServerTimeEngine engine = timemod$engine();
        if (engine.worldAdvances()) {
            original.call(ticks, time, max, ticker);
        }
        engine.afterWorldBlockTicks();
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/ticks/LevelTicks;tick(JILjava/util/function/BiConsumer;)V", ordinal = 1))
    private void timemod$fluidTicks(LevelTicks<?> ticks, long time, int max, BiConsumer<?, ?> ticker, Operation<Void> original) {
        ServerTimeEngine engine = timemod$engine();
        if (engine.worldAdvances()) {
            original.call(ticks, time, max, ticker);
        }
        engine.afterWorldFluidTicks();
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raids;tick()V"))
    private void timemod$raids(Raids raids, Operation<Void> original) {
        int steps = timemod$engine().worldSystemSteps(TimeFilter.WORLD_EVENTS);
        for (int i = 0; i < steps; i++) {
            original.call(raids);
        }
    }

    @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/dimension/end/EndDragonFight;tick()V"))
    private boolean timemod$dragonFight(EndDragonFight fight) {
        return timemod$engine().worldAdvances();
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;tickBlockEntities()V"))
    private void timemod$entityPhase(BooleanSupplier hasTime, CallbackInfo ci) {
        timemod$engine().onEntityPhase();
    }

    // ---------------- Entities ----------------

    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void timemod$beforeEntityTick(Entity entity, CallbackInfo ci) {
        if (!timemod$engine().beforeEntityTick(entity)) {
            ci.cancel();
        }
    }

    @Inject(method = "tickNonPassenger", at = @At("RETURN"))
    private void timemod$afterEntityTick(Entity entity, CallbackInfo ci) {
        timemod$engine().afterEntityTick();
    }

    @Inject(method = "tickPassenger", at = @At("HEAD"), cancellable = true)
    private void timemod$passengerTick(Entity vehicle, Entity passenger, CallbackInfo ci) {
        if (!timemod$engine().allowPassengerTick(passenger)) {
            ci.cancel();
        }
    }

    // ---------------- Zufalls-Ticks, Niederschlag, Blitze ----------------

    @WrapWithCondition(method = "tickChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;randomTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private boolean timemod$randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        return timemod$engine().allowRandomTick(pos, state.getBlock());
    }

    @WrapWithCondition(method = "tickChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;randomTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private boolean timemod$fluidRandomTick(FluidState state, Level level, BlockPos pos, RandomSource random) {
        return timemod$engine().allowEnvironment(pos);
    }

    @WrapOperation(method = "tickChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean timemod$precipitation(ServerLevel level, BlockPos pos, BlockState state, Operation<Boolean> original) {
        return timemod$engine().allowEnvironment(pos) && original.call(level, pos, state);
    }

    @WrapWithCondition(method = "tickChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;handlePrecipitation(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/biome/Biome$Precipitation;)V"))
    private boolean timemod$handlePrecipitation(Block block, BlockState state, Level level, BlockPos pos, Biome.Precipitation precipitation) {
        return timemod$engine().allowEnvironment(pos);
    }

    @WrapOperation(method = "tickChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean timemod$lightning(ServerLevel level, Entity entity, Operation<Boolean> original) {
        return timemod$engine().allowEnvironment(entity.blockPosition()) && original.call(level, entity);
    }

    // ---------------- Block-Events (Kolben, Notenblöcke, Truhen …) ----------------

    @WrapOperation(method = "runBlockEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;shouldTickBlocksAt(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean timemod$blockEvent(ServerLevel level, BlockPos pos, Operation<Boolean> original) {
        return original.call(level, pos) && timemod$engine().allowBlockEvent(pos);
    }
}
