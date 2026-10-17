package com.fischgesichtert.timemod.future.town;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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
 * Eine Straße entlang eines Linienzugs: folgt dem Gelände, wird über Wasser zur Brücke mit Geländer und
 * Pfeilern; ab Stufe 2 gepflastert und mit Laternen, in der Stadt eine Allee mit Laternen auf beiden Seiten.
 */
public class RoadPiece extends TownPiece {
    private final int[] xs;
    private final int[] zs;
    private final boolean closed;
    private final int width;

    public RoadPiece(int[] xs, int[] zs, boolean closed, int width, int minY, int maxY, long seed, TownStyle style) {
        super(TownPieces.ROAD, bounds(xs, zs, width, minY, maxY), Direction.NORTH, seed, style);
        this.xs = xs;
        this.zs = zs;
        this.closed = closed;
        this.width = width;
    }

    public RoadPiece(CompoundTag tag) {
        super(TownPieces.ROAD, tag);
        this.xs = tag.getIntArray("Xs");
        this.zs = tag.getIntArray("Zs");
        this.closed = tag.getBoolean("Closed");
        this.width = tag.getInt("Width");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putIntArray("Xs", xs);
        tag.putIntArray("Zs", zs);
        tag.putBoolean("Closed", closed);
        tag.putInt("Width", width);
    }

    private static BoundingBox bounds(int[] xs, int[] zs, int width, int minY, int maxY) {
        int margin = width / 2 + 2;
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (int i = 0; i < xs.length; i++) {
            minX = Math.min(minX, xs[i]);
            maxX = Math.max(maxX, xs[i]);
            minZ = Math.min(minZ, zs[i]);
            maxZ = Math.max(maxZ, zs[i]);
        }
        return new BoundingBox(minX - margin, minY, minZ - margin, maxX + margin, maxY, maxZ + margin);
    }

    private int segments() {
        return closed ? xs.length : xs.length - 1;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox box,
                            ChunkPos chunkPos, BlockPos pivot) {
        double half = width / 2.0 + 0.01;
        boolean lamps = style.stage() >= 2;
        int spacing = style.city() ? 9 : 13;
        int minX = Math.max(box.minX(), boundingBox.minX());
        int maxX = Math.min(box.maxX(), boundingBox.maxX());
        int minZ = Math.max(box.minZ(), boundingBox.minZ());
        int maxZ = Math.min(box.maxZ(), boundingBox.maxZ());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                double[] near = nearest(x + 0.5, z + 0.5);
                double distance = near[0];
                if (distance > half + 1.0) {
                    continue;
                }
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                pos.set(x, top, z);
                boolean water = !level.getFluidState(pos).isEmpty();
                int along = (int) Math.floor(near[1]);
                if (distance <= half) {
                    if (water) {
                        bridge(level, box, pos, x, z, top, distance > half - 1.0 && width > 1, along);
                    } else {
                        pave(level, box, pos, x, top, z, distance, half);
                    }
                } else if (lamps && !water && Math.floorMod(along, spacing) == 0 && (style.city() || near[2] > 0)) {
                    lamp(level, box, x, top + 1, z);
                }
            }
        }
    }

    /** Pflaster (Stadt: Bordstein am Rand), darüber frei; Pflanzen und Schnee weichen. */
    private void pave(WorldGenLevel level, BoundingBox box, BlockPos.MutableBlockPos pos, int x, int top, int z, double distance, double half) {
        BlockState current = level.getBlockState(pos);
        if (current.hasBlockEntity() || !natural(current)) {
            return;
        }
        BlockState road;
        if (style.city() && distance > half - 1.0 && width >= 5) {
            road = state(style.wallStone());
        } else if (chance(x, top, z, 11) < (style.stage() >= 2 ? 0.25F : 0.15F)) {
            road = state(style.roadAccent());
        } else {
            road = state(style.road());
        }
        if (road.is(Blocks.DIRT_PATH) && !level.getBlockState(pos.above()).isAir() && !level.getBlockState(pos.above()).canBeReplaced()) {
            road = state(Blocks.COARSE_DIRT);
        }
        level.setBlock(pos.immutable(), road, Block.UPDATE_CLIENTS);
        for (int y = 1; y <= 2; y++) {
            BlockPos above = pos.above(y);
            BlockState there = level.getBlockState(above);
            if (!there.isAir() && there.canBeReplaced()) {
                level.setBlock(above, OPEN, Block.UPDATE_CLIENTS);
            }
        }
    }

    /** Über Wasser: Fahrbahn eine Höhe über dem Spiegel, Geländer am Rand, alle sieben Blöcke ein Pfeiler. */
    private void bridge(WorldGenLevel level, BoundingBox box, BlockPos.MutableBlockPos pos, int x, int z, int top, boolean edge, int along) {
        int deck = top + 1;
        BlockPos at = new BlockPos(x, deck, z);
        if (!box.isInside(at)) {
            return;
        }
        Block surface = style.stage() >= 2 ? style.wallStone() : style.city() ? style.road() : Blocks.SPRUCE_PLANKS;
        level.setBlock(at, surface.defaultBlockState(), Block.UPDATE_CLIENTS);
        if (edge) {
            level.setBlock(at.above(), state(style.stage() >= 2 ? style.wallCap() : style.fence()), Block.UPDATE_CLIENTS);
            if (Math.floorMod(along, 7) == 0) {
                BlockPos.MutableBlockPos down = at.mutable().move(Direction.DOWN);
                while (down.getY() > level.getMinBuildHeight() + 1 && (level.getBlockState(down).isAir()
                    || !level.getFluidState(down).isEmpty())) {
                    level.setBlock(down, state(style.foundation()), Block.UPDATE_CLIENTS);
                    down.move(Direction.DOWN);
                }
                if (style.stage() >= 2) {
                    level.setBlock(at.above(2), Blocks.LANTERN.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    /** Ein Laternenpfahl am Straßenrand; in der Stadt doppelt hoch. */
    private void lamp(WorldGenLevel level, BoundingBox box, int x, int y, int z) {
        BlockPos base = new BlockPos(x, y, z);
        if (!box.isInside(base) || !level.getBlockState(base).isAir() || !level.getBlockState(base.below()).isSolid()) {
            return;
        }
        int height = style.city() ? 3 : 2;
        level.setBlock(base, state(style.city() ? style.wallCap() : style.fence()), Block.UPDATE_CLIENTS);
        for (int i = 1; i < height; i++) {
            level.setBlock(base.above(i), state(style.fence()), Block.UPDATE_CLIENTS);
        }
        level.setBlock(base.above(height), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false), Block.UPDATE_CLIENTS);
    }

    /** Abstand zum Linienzug, Bogenlänge am nächsten Punkt und Seite (+1 links, −1 rechts). */
    private double[] nearest(double px, double pz) {
        double best = Double.MAX_VALUE;
        double arc = 0;
        double side = 1;
        double walked = 0;
        for (int i = 0; i < segments(); i++) {
            int j = (i + 1) % xs.length;
            double ax = xs[i] + 0.5;
            double az = zs[i] + 0.5;
            double dx = xs[j] - xs[i];
            double dz = zs[j] - zs[i];
            double length = Math.sqrt(dx * dx + dz * dz);
            if (length < 1e-6) {
                continue;
            }
            double t = Math.max(0, Math.min(1, ((px - ax) * dx + (pz - az) * dz) / (length * length)));
            double cx = ax + t * dx;
            double cz = az + t * dz;
            double d = Math.sqrt((px - cx) * (px - cx) + (pz - cz) * (pz - cz));
            if (d < best) {
                best = d;
                arc = walked + t * length;
                side = Math.signum(dx * (pz - az) - dz * (px - ax));
            }
            walked += length;
        }
        return new double[] {best, arc, side};
    }
}
