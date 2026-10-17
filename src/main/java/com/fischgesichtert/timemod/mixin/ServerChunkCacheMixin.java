package com.fischgesichtert.timemod.mixin;

import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.TimeFilter;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Bei global eingefrorener Welt ruhen auch Zufalls-Ticks, Spawnen und Spawner. */
@Mixin(ServerChunkCache.class)
public abstract class ServerChunkCacheMixin {
    @Shadow
    @Final
    ServerLevel level;

    @WrapWithCondition(method = "tickChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;tickChunk(Lnet/minecraft/world/level/chunk/LevelChunk;I)V"))
    private boolean timemod$tickChunk(ServerLevel level, LevelChunk chunk, int randomTickSpeed) {
        return ServerTimeEngine.of(level).worldAdvances();
    }

    @WrapWithCondition(method = "tickChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/NaturalSpawner;spawnForChunk(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/world/level/NaturalSpawner$SpawnState;ZZZ)V"))
    private boolean timemod$spawn(ServerLevel level, LevelChunk chunk, NaturalSpawner.SpawnState state, boolean friendlies, boolean enemies, boolean persistent) {
        return ServerTimeEngine.of(level).worldSystemSteps(TimeFilter.WORLD_EVENTS) > 0;
    }

    @WrapWithCondition(method = "tickChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunk;incrementInhabitedTime(J)V"))
    private boolean timemod$inhabitedTime(LevelChunk chunk, long ticks) {
        return ServerTimeEngine.of(level).worldAdvances();
    }

    @WrapOperation(method = "tickChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;tickCustomSpawners(ZZ)V"))
    private void timemod$customSpawners(ServerLevel level, boolean enemies, boolean friendlies, Operation<Void> original) {
        int steps = ServerTimeEngine.of(level).worldSystemSteps(TimeFilter.WORLD_EVENTS);
        for (int i = 0; i < steps; i++) {
            original.call(level, enemies, friendlies);
        }
    }
}
