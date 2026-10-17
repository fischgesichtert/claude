package com.fischgesichtert.timemod.mixin;

import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.TimeAware;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin implements TimeAware {
    @Unique
    private static final String TIMEMOD_KEY = "timemod:Eigenzeit";

    @Unique
    private long timemod$eigenzeit;

    @Override
    public long timemod$getEigenzeit() {
        return timemod$eigenzeit;
    }

    @Override
    public void timemod$setEigenzeit(long ticks) {
        timemod$eigenzeit = ticks;
    }

    @Inject(method = "saveWithoutMetadata", at = @At("RETURN"))
    private void timemod$save(CallbackInfoReturnable<CompoundTag> cir) {
        if (timemod$eigenzeit != 0) {
            cir.getReturnValue().putLong(TIMEMOD_KEY, timemod$eigenzeit);
        }
    }

    @Inject(method = "loadStatic", at = @At("RETURN"))
    private static void timemod$load(BlockPos pos, BlockState state, CompoundTag tag, CallbackInfoReturnable<BlockEntity> cir) {
        if (cir.getReturnValue() instanceof TimeAware aware && tag.contains(TIMEMOD_KEY)) {
            aware.timemod$setEigenzeit(tag.getLong(TIMEMOD_KEY));
        }
    }

    /** Zeitjournal: geänderte Block-Entity-Daten (Ofen, Truhe, Trichter …) am Tick-Ende sichern. */
    @Inject(method = "setChanged(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V", at = @At("HEAD"))
    private static void timemod$recordChange(Level level, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (level instanceof ServerLevel serverLevel) {
            ServerTimeEngine engine = ServerTimeEngine.of(serverLevel);
            if (engine != null) {
                engine.journal().onBlockEntityChanged(pos);
            }
        }
    }
}
