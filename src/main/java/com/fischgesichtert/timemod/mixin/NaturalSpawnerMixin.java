package com.fischgesichtert.timemod.mixin;

import com.fischgesichtert.timemod.era.EraSpawns;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Die tiefe Vergangenheit kennt keine Vanilla-Wesen – weder beim Spawnen noch bei der Chunk-Erzeugung. */
@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {
    @Inject(method = "mobsAt", at = @At("RETURN"), cancellable = true)
    private static void timemod$eraLife(ServerLevel level, StructureManager structureManager, ChunkGenerator generator, MobCategory category,
                                        BlockPos pos, @Nullable Holder<Biome> biome,
                                        CallbackInfoReturnable<WeightedRandomList<MobSpawnSettings.SpawnerData>> cir) {
        cir.setReturnValue(EraSpawns.filter(level.dimension(), cir.getReturnValue()));
    }

    @ModifyExpressionValue(method = "spawnMobsForChunkGeneration", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/biome/MobSpawnSettings;getMobs(Lnet/minecraft/world/entity/MobCategory;)Lnet/minecraft/util/random/WeightedRandomList;"))
    private static WeightedRandomList<MobSpawnSettings.SpawnerData> timemod$eraChunkLife(WeightedRandomList<MobSpawnSettings.SpawnerData> spawns,
                                                                                       @Local(argsOnly = true) ServerLevelAccessor accessor) {
        return EraSpawns.filter(accessor.getLevel().dimension(), spawns);
    }
}
