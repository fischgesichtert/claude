package com.fischgesichtert.timemod.future.town;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Die Mauer rund um die Siedlung, dem Gelände folgend: Tore, wo die Hauptstraßen hinausführen, mit Zinnen,
 * ab Stufe 3 mit Türmen, in der Stadt hoch, mit Wehrgang und Kupferhauben auf den Türmen. Über Wasser bleibt
 * sie offen.
 */
public class WallPiece extends TownPiece {
    private final int centerX;
    private final int centerZ;
    private final int radius;
    private final float[] gates;
    private final float[] towers;

    public WallPiece(int centerX, int centerZ, int radius, float[] gates, float[] towers, int minY, int maxY, long seed, TownStyle style) {
        super(TownPieces.WALL, new BoundingBox(centerX - radius - 5, minY, centerZ - radius - 5, centerX + radius + 5, maxY, centerZ + radius + 5),
            Direction.NORTH, seed, style);
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = radius;
        this.gates = gates;
        this.towers = towers;
    }

    public WallPiece(CompoundTag tag) {
        super(TownPieces.WALL, tag);
        this.centerX = tag.getInt("CX");
        this.centerZ = tag.getInt("CZ");
        this.radius = tag.getInt("Radius");
        this.gates = floats(tag.getIntArray("Gates"));
        this.towers = floats(tag.getIntArray("Towers"));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putInt("CX", centerX);
        tag.putInt("CZ", centerZ);
        tag.putInt("Radius", radius);
        tag.putIntArray("Gates", ints(gates));
        tag.putIntArray("Towers", ints(towers));
    }

    private static float[] floats(int[] values) {
        float[] out = new float[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = values[i] / 10000.0F;
        }
        return out;
    }

    private static int[] ints(float[] values) {
        int[] out = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = Math.round(values[i] * 10000.0F);
        }
        return out;
    }

    private int height() {
        return style.city() ? 8 : style.stage() >= 3 ? 6 : 4;
    }

    private double thickness() {
        return style.city() ? 3.0 : 2.0;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox box,
                            ChunkPos chunkPos, BlockPos pivot) {
        int minX = Math.max(box.minX(), boundingBox.minX());
        int maxX = Math.min(box.maxX(), boundingBox.maxX());
        int minZ = Math.max(box.minZ(), boundingBox.minZ());
        int maxZ = Math.min(box.maxZ(), boundingBox.maxZ());
        double gateHalf = style.roadWidth() / 2.0 + 1.5;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                double dx = x + 0.5 - centerX;
                double dz = z + 0.5 - centerZ;
                double r = Math.sqrt(dx * dx + dz * dz);
                double angle = Math.atan2(dz, dx);
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                BlockPos ground = new BlockPos(x, top, z);
                if (!level.getFluidState(ground).isEmpty() || !natural(level.getBlockState(ground))) {
                    continue;
                }
                double tower = towerDistance(dx, dz);
                if (tower <= 3.5) {
                    tower(level, x, top, z, tower);
                    continue;
                }
                double inner = radius - thickness() / 2.0;
                if (r < inner || r >= inner + thickness()) {
                    continue;
                }
                boolean outer = r >= inner + thickness() - 1.0;
                double gate = gateDistance(angle) * radius;
                int height = height();
                for (int y = 1; y <= height; y++) {
                    if (gate <= gateHalf && y <= 4) {
                        level.setBlock(ground.above(y), OPEN, Block.UPDATE_CLIENTS);
                        continue;
                    }
                    level.setBlock(ground.above(y), stone(x, top + y, z), Block.UPDATE_CLIENTS);
                }
                if (gate <= gateHalf) {
                    // Torbogen: Pflaster bleibt, darüber der Sturz; am Rand des Tors eine Laterne
                    if (gate > gateHalf - 1.0 && outer) {
                        level.setBlock(ground.above(height + 1), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false),
                            Block.UPDATE_CLIENTS);
                    }
                    continue;
                }
                // Fundament bis zum Boden, Zinnen außen
                BlockPos.MutableBlockPos down = ground.mutable();
                while (down.getY() > level.getMinBuildHeight() + 1 && (level.getBlockState(down).canBeReplaced())) {
                    level.setBlock(down, state(style.foundation()), Block.UPDATE_CLIENTS);
                    down.move(Direction.DOWN);
                }
                if (outer && Math.floorMod((int) Math.round(angle * radius), 2) == 0) {
                    level.setBlock(ground.above(height + 1), state(style.wallCap()), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private BlockState stone(int x, int y, int z) {
        float c = chance(x, y, z, 21);
        if (style.stage() >= 3 && c < 0.12F) {
            return state(style.roadAccent() == Blocks.COBBLESTONE ? Blocks.MOSSY_STONE_BRICKS : style.foundation());
        }
        return state(style.wallStone());
    }

    /** Rundturm: gemauerter Ring, oben eine Plattform mit Zinnen, in der Stadt mit Kupferhaube. */
    private void tower(WorldGenLevel level, int x, int top, int z, double distance) {
        int height = height() + 4;
        BlockPos ground = new BlockPos(x, top, z);
        boolean ring = distance > 2.2;
        for (int y = 1; y <= height; y++) {
            level.setBlock(ground.above(y), ring || y == height ? stone(x, top + y, z) : OPEN, Block.UPDATE_CLIENTS);
        }
        BlockPos.MutableBlockPos down = ground.mutable();
        while (down.getY() > level.getMinBuildHeight() + 1 && level.getBlockState(down).canBeReplaced()) {
            level.setBlock(down, state(style.foundation()), Block.UPDATE_CLIENTS);
            down.move(Direction.DOWN);
        }
        if (style.city()) {
            // Kegelhaube aus Kupfer
            int cap = (int) Math.round(3.6 - distance);
            for (int y = 1; y <= cap + 1; y++) {
                level.setBlock(ground.above(height + y), style.roofFull((int) (seed >>> 3) + y), Block.UPDATE_CLIENTS);
            }
        } else if (ring && Math.floorMod(x + z, 2) == 0) {
            level.setBlock(ground.above(height + 1), state(style.wallCap()), Block.UPDATE_CLIENTS);
        }
    }

    private double gateDistance(double angle) {
        double best = Math.PI;
        for (float gate : gates) {
            best = Math.min(best, Math.abs(Mth.wrapDegrees(Math.toDegrees(angle - gate))) * Math.PI / 180.0);
        }
        return best;
    }

    private double towerDistance(double dx, double dz) {
        double best = Double.MAX_VALUE;
        for (float tower : towers) {
            double tx = Math.cos(tower) * radius;
            double tz = Math.sin(tower) * radius;
            best = Math.min(best, Math.sqrt((dx - tx) * (dx - tx) + (dz - tz) * (dz - tz)));
        }
        return best;
    }
}
