package com.fischgesichtert.timemod.mixin;

import com.fischgesichtert.timemod.safety.CrashGuard;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Absturzschutz für geplante Block- und Flüssigkeits-Ticks und Block-Events: Scheitert dabei etwas aus
 * TIME (etwa ein Block der Mod), fällt nur dieser eine Tick aus.
 */
@Mixin(ServerLevel.class)
abstract class CrashGuardServerLevelMixin {
    @WrapOperation(method = "tickBlock", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/block/state/BlockState;tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private void timemod$guardBlockTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, Operation<Void> original) {
        try {
            original.call(state, level, pos, random);
        } catch (Throwable error) {
            if (!CrashGuard.absorb("Block-Tick", error)) {
                throw error;
            }
        }
    }

    @WrapOperation(method = "tickFluid", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/material/FluidState;tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"))
    private void timemod$guardFluidTick(FluidState state, Level level, BlockPos pos, Operation<Void> original) {
        try {
            original.call(state, level, pos);
        } catch (Throwable error) {
            if (!CrashGuard.absorb("Flüssigkeits-Tick", error)) {
                throw error;
            }
        }
    }

    @WrapOperation(method = "doBlockEvent", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/block/state/BlockState;triggerEvent(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;II)Z"))
    private boolean timemod$guardBlockEvent(BlockState state, Level level, BlockPos pos, int type, int data, Operation<Boolean> original) {
        try {
            return original.call(state, level, pos, type, data);
        } catch (Throwable error) {
            if (!CrashGuard.absorb("Block-Event", error)) {
                throw error;
            }
            return false;
        }
    }
}
