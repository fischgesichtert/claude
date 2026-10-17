package com.fischgesichtert.timemod.mixin;

import com.fischgesichtert.timemod.safety.CrashGuard;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Absturzschutz für Bauwerke der Mod: Ein Teil, der sich nicht bauen lässt, bleibt weg (siehe {@link CrashGuardFeatureMixin}). */
@Mixin(StructureStart.class)
abstract class CrashGuardStructureStartMixin {
    @WrapOperation(method = "placeInChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/structure/StructurePiece;"
        + "postProcess(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/chunk/ChunkGenerator;"
        + "Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/level/levelgen/structure/BoundingBox;Lnet/minecraft/world/level/ChunkPos;"
        + "Lnet/minecraft/core/BlockPos;)V"))
    private void timemod$guardPiece(StructurePiece piece, WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
                                    RandomSource random, BoundingBox box, ChunkPos chunk, BlockPos pivot, Operation<Void> original) {
        try {
            original.call(piece, level, structures, generator, random, box, chunk, pivot);
        } catch (Throwable error) {
            if (!CrashGuard.absorb("Weltgenerierung: Bauwerk", error)) {
                throw error;
            }
        }
    }
}
