package com.fischgesichtert.timemod.future;

import com.fischgesichtert.timemod.era.AncientTrees;
import com.fischgesichtert.timemod.era.Era;
import com.fischgesichtert.timemod.era.Eras;
import com.fischgesichtert.timemod.worldgen.FutureBiomeSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

/**
 * Alte Bäume werden riesig: In den Wäldern der Zukunft steht hier und da ein Uralter, umso öfter und
 * umso größer, je mehr Jahre vergangen sind. Der Zufall hängt nur an Seed und Chunk – wo bei +500 ein
 * Riese steht, steht bei +1000 derselbe, nur größer.
 */
public class AncientGroveFeature extends Feature<NoneFeatureConfiguration> {
    public AncientGroveFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        Era era = Eras.byLevel(level.getLevel().dimension());
        if (era == null || !era.isFuture()) {
            return false;
        }
        RandomSource random = context.random();
        double chance = WeatheringRules.get().reclaim().giantTrees().at(era.years());
        if (random.nextDouble() >= chance) {
            return false;
        }
        // Nahe der Chunkmitte: So reicht die Krone nie über die Nachbarchunks hinaus, in die geschrieben werden darf
        ChunkPos chunk = new ChunkPos(context.origin());
        int x = chunk.getMinBlockX() + 6 + random.nextInt(4);
        int z = chunk.getMinBlockZ() + 6 + random.nextInt(4);
        BlockPos ground = ground(level, x, z);
        if (ground == null) {
            return false;
        }
        Holder<Biome> biome = level.getBiome(ground);
        if (!biome.is(FutureBiomeSource.WOODED)) {
            return false;
        }
        BoundingBox area = new BoundingBox(chunk.getMinBlockX() - 16, level.getMinBuildHeight(), chunk.getMinBlockZ() - 16, chunk.getMaxBlockX() + 16,
            level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ() + 16);
        return AncientTrees.ancient(level, ground, biome, era.years(), random, area);
    }

    /** Der gewachsene Boden unter Laub und Stämmen; nicht unter Wasser. */
    @Nullable
    static BlockPos ground(WorldGenLevel level, int x, int z) {
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        for (int y = top; y > Math.max(level.getMinBuildHeight(), top - 40); y--) {
            BlockPos pos = new BlockPos(x, y - 1, z);
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.DIRT)) {
                return level.getFluidState(pos.above()).isEmpty() ? pos.above() : null;
            }
            if (!state.isAir() && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS) && !state.canBeReplaced()) {
                return null;
            }
        }
        return null;
    }
}
