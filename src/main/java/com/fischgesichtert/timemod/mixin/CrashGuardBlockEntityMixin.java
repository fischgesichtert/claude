package com.fischgesichtert.timemod.mixin;

import com.fischgesichtert.timemod.safety.CrashGuard;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Absturzschutz für Block-Entity-Ticks (Server und Client), wie {@link CrashGuardLevelMixin} für Entities. */
@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity")
abstract class CrashGuardBlockEntityMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BlockEntityTicker;"
        + "tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;"
        + "Lnet/minecraft/world/level/block/entity/BlockEntity;)V"))
    private void timemod$guardBlockEntityTick(BlockEntityTicker<BlockEntity> ticker, Level level, BlockPos pos, BlockState state,
                                              BlockEntity blockEntity, Operation<Void> original) {
        try {
            original.call(ticker, level, pos, state, blockEntity);
        } catch (Throwable error) {
            if (!CrashGuard.absorbBlockEntity(blockEntity, error)) {
                throw error;
            }
        }
    }
}
