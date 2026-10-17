package com.fischgesichtert.timemod.mixin;

import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Geplante Ticks, die gerade in einer Zeitzone liegen, werden mit dem Chunk gespeichert. */
@Mixin(ChunkSerializer.class)
public abstract class ChunkSerializerMixin {
    @WrapOperation(method = "write", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/storage/ChunkSerializer;saveTicks(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/world/level/chunk/ChunkAccess$TicksToSave;)V"))
    private static void timemod$saveZoneTicks(ServerLevel level, CompoundTag tag, ChunkAccess.TicksToSave ticks, Operation<Void> original,
                                              @Local(argsOnly = true) ChunkAccess chunk) {
        original.call(level, tag, ticks);
        ServerTimeEngine.of(level).appendSavedTicks(chunk.getPos(), tag);
    }
}
