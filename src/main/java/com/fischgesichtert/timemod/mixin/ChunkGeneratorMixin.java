package com.fischgesichtert.timemod.mixin;

import com.fischgesichtert.timemod.future.town.TownGrowth;
import com.fischgesichtert.timemod.safety.Safety;
import com.fischgesichtert.timemod.worldgen.FutureBiomeSource;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Bauwerke der Zukunft stehen dort, wo sie heute stünden: Ihre Biom-Prüfung (und die Lage der Festungen)
 * nimmt die Biome von heute, nicht die des wärmeren, waldreicheren Klimas. Und Dörfer wachsen.
 */
@Mixin(ChunkGenerator.class)
abstract class ChunkGeneratorMixin {
    @WrapOperation(method = "tryGenerateStructure", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/structure/Structure;"
        + "generate(Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/world/level/biome/BiomeSource;"
        + "Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplateManager;J"
        + "Lnet/minecraft/world/level/ChunkPos;ILnet/minecraft/world/level/LevelHeightAccessor;Ljava/util/function/Predicate;)"
        + "Lnet/minecraft/world/level/levelgen/structure/StructureStart;"))
    private StructureStart timemod$presentAndGrowth(Structure structure, RegistryAccess access, ChunkGenerator generator, BiomeSource biomes,
                                                    RandomState random, StructureTemplateManager templates, long seed, ChunkPos pos, int references,
                                                    LevelHeightAccessor heights, Predicate<Holder<Biome>> valid, Operation<StructureStart> original,
                                                    @Local(argsOnly = true) StructureManager manager, @Local(argsOnly = true) ChunkAccess chunk,
                                                    @Local(argsOnly = true) SectionPos section) {
        if (!(biomes instanceof FutureBiomeSource future)) {
            return original.call(structure, access, generator, biomes, random, templates, seed, pos, references, heights, valid);
        }
        StructureStart start = original.call(structure, access, generator, future.base(), random, templates, seed, pos, references, heights, valid);
        if (!start.isValid()) {
            return start;
        }
        // Wächst ein Dorf nicht fehlerfrei, bleibt es eben so, wie es heute ist
        return Safety.get("Zukunft: Dorf wächst", () -> TownGrowth.grow(start, structure, future.years(), access, generator, random, heights, seed,
            manager, chunk, section), start);
    }

    @ModifyArg(method = "createState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkGeneratorStructureState;"
        + "createForNormal(Lnet/minecraft/world/level/levelgen/RandomState;JLnet/minecraft/world/level/biome/BiomeSource;"
        + "Lnet/minecraft/core/HolderLookup;)Lnet/minecraft/world/level/chunk/ChunkGeneratorStructureState;"), index = 2)
    private BiomeSource timemod$presentStrongholds(BiomeSource biomes) {
        return biomes instanceof FutureBiomeSource future ? future.base() : biomes;
    }
}
