package com.fischgesichtert.timemod.mixin;

import com.fischgesichtert.timemod.worldgen.FutureBiomeSource;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Minecraft prüft das Biom am Startpunkt eines Bauwerks über die Biomquelle des Generators. In der Zukunft gilt
 * dafür das Biom von heute: Dörfer, Tempel und Außenposten stehen dort, wo sie heute stünden.
 */
@Mixin(Structure.class)
abstract class StructureMixin {
    @WrapOperation(method = "isValidBiome", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/chunk/ChunkGenerator;getBiomeSource()Lnet/minecraft/world/level/biome/BiomeSource;"))
    private static BiomeSource timemod$presentBiomes(ChunkGenerator generator, Operation<BiomeSource> original) {
        BiomeSource source = original.call(generator);
        return source instanceof FutureBiomeSource future ? future.base() : source;
    }
}
