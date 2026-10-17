package com.fischgesichtert.timemod.mixin;

import com.fischgesichtert.timemod.safety.CrashGuard;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Absturzschutz bei der Weltgenerierung: Scheitert ein Feature der Mod (Verwitterung, Urwald, Flussufer …),
 * fehlt es eben in diesem Chunk – statt dass das Erzeugen des Chunks das Spiel beendet.
 */
@Mixin(ConfiguredFeature.class)
abstract class CrashGuardFeatureMixin {
    @WrapOperation(method = "place", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/feature/Feature;"
        + "place(Lnet/minecraft/world/level/levelgen/feature/configurations/FeatureConfiguration;Lnet/minecraft/world/level/WorldGenLevel;"
        + "Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean timemod$guardFeature(Feature<FeatureConfiguration> feature, FeatureConfiguration config, WorldGenLevel level,
                                         ChunkGenerator generator, RandomSource random, BlockPos origin, Operation<Boolean> original) {
        try {
            return original.call(feature, config, level, generator, random, origin);
        } catch (Throwable error) {
            if (!CrashGuard.absorb("Weltgenerierung: Feature", error)) {
                throw error;
            }
            return false;
        }
    }
}
