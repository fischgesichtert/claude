package com.fischgesichtert.timemod.future;

import com.fischgesichtert.timemod.era.Era;
import com.fischgesichtert.timemod.era.Eras;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraft.world.level.material.Fluids;

/**
 * Flüsse arbeiten: In der Zukunft liegen in seichten Flussabschnitten Sand- und Kiesbänke, manche ragen
 * aus dem Wasser, und niedrige Ufer sind zu Sandstränden abgetragen. Nur in Fluss-Biomen, nur in
 * Zukunfts-Epochen; die Bänke wachsen mit den Jahren an denselben Stellen.
 */
public class RiverBanksFeature extends Feature<NoneFeatureConfiguration> {
    private static final long SALT = 0x5A_4DBA_4CL;
    private static volatile Noise noise;

    public RiverBanksFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        Era era = Eras.byLevel(level.getLevel().dimension());
        if (era == null || !era.isFuture()) {
            return false;
        }
        double strength = WeatheringRules.get().reclaim().sandbanks().at(era.years());
        if (strength <= 0) {
            return false;
        }
        Noise field = noise(level.getSeed());
        int surface = level.getSeaLevel() - 1;
        ChunkPos chunk = new ChunkPos(context.origin());
        boolean changed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                if (!level.getBiome(pos.set(x, surface, z)).is(BiomeTags.IS_RIVER)) {
                    continue;
                }
                changed |= shape(level, x, z, surface, field.at(x, z), strength);
            }
        }
        return changed;
    }

    /**
     * Eine Spalte am Fluss: im seichten Wasser eine Bank, am niedrigen Ufer ein Sandstrand. {@code value} ist das
     * Ablagerungs-Rauschen (0..1), {@code strength} die Stärke der Epoche; {@code surface} die Höhe der Wasserlinie.
     */
    public static boolean shape(WorldGenLevel level, int x, int z, int surface, double value, double strength) {
        if (level.getFluidState(new BlockPos(x, surface, z)).is(Fluids.WATER)) {
            return sandbank(level, x, z, surface, value, strength);
        }
        return erode(level, x, z, surface, value, strength);
    }

    /** Seichtes Wasser: Der Grund wächst auf, bei starken Bänken bis über die Wasserlinie. */
    private static boolean sandbank(WorldGenLevel level, int x, int z, int surface, double value, double strength) {
        int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
        int depth = surface - floor + 1;
        double threshold = 1.0 - strength;
        if (depth < 1 || depth > 3 || value < threshold) {
            return false;
        }
        BlockState below = level.getBlockState(new BlockPos(x, floor - 1, z));
        if (!below.isFaceSturdy(level, new BlockPos(x, floor - 1, z), Direction.UP)) {
            return false;
        }
        // Auf Kiesgrund wächst eine Kiesbank, sonst eine aus Sand
        BlockState bank = below.is(Blocks.GRAVEL) ? Blocks.GRAVEL.defaultBlockState() : Blocks.SAND.defaultBlockState();
        // Eine Bank, die aus dem Wasser ragt, braucht tiefes Wasser dafür nicht – nur viel Zeit
        int top = value > threshold + 0.25 && depth <= 2 ? surface : surface - 1;
        for (int y = floor; y <= top; y++) {
            level.setBlock(new BlockPos(x, y, z), bank, Block.UPDATE_CLIENTS);
        }
        // Die obere Hälfte von hohem Seegras darf nicht über der Bank schweben
        BlockPos above = new BlockPos(x, top + 1, z);
        BlockState rest = level.getBlockState(above);
        if (rest.getBlock() instanceof DoublePlantBlock && rest.hasProperty(DoublePlantBlock.HALF)
            && rest.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER) {
            level.setBlock(above, rest.getFluidState().isEmpty() ? Blocks.AIR.defaultBlockState() : Blocks.WATER.defaultBlockState(),
                Block.UPDATE_CLIENTS);
        }
        return top >= floor;
    }

    /** Niedrige Ufer direkt am Wasser: Der Rasen ist weggespült, darunter liegt Sand. */
    private static boolean erode(WorldGenLevel level, int x, int z, int surface, double value, double strength) {
        if (value < 1.0 - strength * 0.8) {
            return false;
        }
        int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1;
        if (ground < surface || ground > surface + 1 || !nextToWater(level, x, ground, z, surface)) {
            return false;
        }
        BlockPos top = new BlockPos(x, ground, z);
        if (!level.getBlockState(top).is(BlockTags.DIRT) || level.getBlockState(top.above()).is(BlockTags.LOGS)) {
            return false;
        }
        clearPlants(level, top.above());
        if (ground == surface + 1) {
            // Eine Stufe über dem Wasser: Die Kante bricht ab, darunter wird es sandig
            level.setBlock(top, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(top.below(), Blocks.SAND.defaultBlockState(), Block.UPDATE_CLIENTS);
        } else {
            level.setBlock(top, Blocks.SAND.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        return true;
    }

    private static boolean nextToWater(WorldGenLevel level, int x, int y, int z, int surface) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            int nx = x + direction.getStepX();
            int nz = z + direction.getStepZ();
            if (level.getFluidState(new BlockPos(nx, surface, nz)).is(Fluids.WATER)) {
                return true;
            }
        }
        return false;
    }

    private static void clearPlants(WorldGenLevel level, BlockPos pos) {
        BlockState plant = level.getBlockState(pos);
        if (plant.isAir() || !plant.canBeReplaced() && !plant.is(BlockTags.FLOWERS) && !plant.is(BlockTags.SAPLINGS)) {
            return;
        }
        if (plant.getBlock() instanceof DoublePlantBlock && level.getBlockState(pos.above()).is(plant.getBlock())) {
            level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    private static Noise noise(long seed) {
        Noise current = noise;
        if (current == null || current.seed != seed) {
            current = new Noise(seed);
            noise = current;
        }
        return current;
    }

    /** Zwischen 0 und 1: wo sich Sand ablagert (seed-fest, für alle Epochen dieselben Stellen). */
    private static final class Noise {
        final long seed;
        final SimplexNoise coarse;
        final SimplexNoise fine;

        Noise(long seed) {
            this.seed = seed;
            RandomSource random = RandomSource.create(seed ^ SALT);
            this.coarse = new SimplexNoise(random);
            this.fine = new SimplexNoise(random);
        }

        double at(int x, int z) {
            double value = 0.7 * coarse.getValue(x / 22.0, z / 22.0) + 0.3 * fine.getValue(x / 7.0, z / 7.0);
            return Mth.clamp(0.5 + value * 0.6, 0.0, 1.0);
        }
    }
}
