package com.fischgesichtert.timemod.era;

import com.fischgesichtert.timemod.registry.ModBlocks;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

/**
 * Zeitsaat in der Gegenwart: Aus einem Setzling von vor ein paar Jahren ist ein Baum geworden, aus einem
 * von vor tausend Jahren ein Riese mit Wurzeln wie Mauern. Was älter als 20.000 Jahre ist, steht nur noch
 * als versteinerter Stumpf da – mit Bernstein im Stein.
 */
public final class AncientTrees {
    private static final int PETRIFIED_FROM = 20_000;
    private static final int GIANT_FROM = 50;

    private AncientTrees() {
    }

    /** Holz und Laub eines Setzlings – auch für Setzlinge anderer Mods (über ihren Namen). */
    record Wood(BlockState log, BlockState leaves) {
        static Wood of(String saplingId) {
            ResourceLocation id = ResourceLocation.tryParse(saplingId);
            if (id == null) {
                return oak();
            }
            String name = id.getPath().replace("_sapling", "").replace("_propagule", "");
            if (name.contains("azalea")) {
                return new Wood(Blocks.OAK_LOG.defaultBlockState(), leaves(name.startsWith("flowering") ? Blocks.FLOWERING_AZALEA_LEAVES
                    : Blocks.AZALEA_LEAVES));
            }
            Block log = BuiltInRegistries.BLOCK.getOptional(new ResourceLocation(id.getNamespace(), name + "_log")).orElse(null);
            Block leaves = BuiltInRegistries.BLOCK.getOptional(new ResourceLocation(id.getNamespace(), name + "_leaves")).orElse(null);
            return log == null || leaves == null ? oak() : new Wood(log.defaultBlockState(), leaves(leaves));
        }

        private static Wood oak() {
            return new Wood(Blocks.OAK_LOG.defaultBlockState(), leaves(Blocks.OAK_LEAVES));
        }

        private static BlockState leaves(Block block) {
            BlockState state = block.defaultBlockState();
            return state.hasProperty(LeavesBlock.PERSISTENT) ? state.setValue(LeavesBlock.PERSISTENT, true) : state;
        }

        BlockState log(Direction.Axis axis) {
            return log.hasProperty(RotatedPillarBlock.AXIS) ? log.setValue(RotatedPillarBlock.AXIS, axis) : log;
        }
    }

    static void grow(ServerLevel level, Timeline.Entry entry) {
        BlockPos ground = ground(level, entry.pos());
        if (ground == null) {
            return;
        }
        RandomSource random = RandomSource.create(entry.pos().asLong() ^ entry.years());
        long years = entry.years();
        if (years >= PETRIFIED_FROM) {
            petrified(level, ground, random);
            return;
        }
        Wood wood = Wood.of(entry.data().getString("Sapling"));
        if (years < GIANT_FROM && growSapling(level, ground, entry.data().getString("Sapling"), random)) {
            return;
        }
        int height = (int) Mth.clamp(13 + 9 * Math.log10(Math.max(1, years) / (double) GIANT_FROM), 12, 34);
        int trunk = years < 500 ? 2 : years < 2_000 ? 3 : 4;
        giant(level, ground, wood, height, trunk, random);
    }

    /** Junge Bäume wachsen ganz normal aus ihrem Setzling. */
    private static boolean growSapling(ServerLevel level, BlockPos ground, String saplingId, RandomSource random) {
        ResourceLocation id = ResourceLocation.tryParse(saplingId);
        Block block = id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
        if (!(block instanceof SaplingBlock sapling)) {
            return false;
        }
        level.setBlockAndUpdate(ground, sapling.defaultBlockState());
        for (int i = 0; i < 2 && level.getBlockState(ground).is(sapling); i++) {
            sapling.advanceTree(level, ground, level.getBlockState(ground), random);
        }
        if (level.getBlockState(ground).is(sapling)) {
            level.setBlockAndUpdate(ground, Blocks.AIR.defaultBlockState());
            return false;
        }
        return true;
    }

    /** Ein Riese: dicker Stamm, Brettwurzeln, weit ausladende Äste und eine Krone fast so breit, wie der Baum hoch ist. */
    private static void giant(ServerLevel level, BlockPos ground, Wood wood, int height, int trunk, RandomSource random) {
        BlockPos base = ground.offset(-(trunk - 1) / 2, 0, -(trunk - 1) / 2);
        double cx = base.getX() + trunk / 2.0;
        double cz = base.getZ() + trunk / 2.0;
        for (int y = -1; y < height; y++) {
            int width = y > height * 0.78 && trunk > 1 ? trunk - 1 : trunk;
            for (int dx = 0; dx < width; dx++) {
                for (int dz = 0; dz < width; dz++) {
                    place(level, base.offset(dx, y, dz), wood.log(Direction.Axis.Y), true);
                }
            }
        }
        // Brettwurzeln laufen schräg ins Erdreich
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (int side = 0; side < trunk; side += Math.max(1, trunk - 1)) {
                BlockPos start = edge(base, trunk, direction, side);
                int length = 2 + trunk / 2 + random.nextInt(2);
                for (int i = 1; i <= length; i++) {
                    BlockPos root = start.relative(direction, i).below(i / 2);
                    place(level, root, wood.log(direction.getAxis()), true);
                    place(level, root.below(), wood.log(Direction.Axis.Y), true);
                }
            }
        }
        // Viele lange, flach ansteigende Äste mit Laub an der Spitze und auf halber Strecke
        int branches = 4 + 2 * trunk + random.nextInt(3);
        for (int b = 0; b < branches; b++) {
            double angle = b * Mth.TWO_PI / branches + random.nextDouble() * 0.5;
            int startY = (int) (height * (0.4 + 0.4 * random.nextDouble()));
            int length = 4 + 2 * trunk + random.nextInt(4);
            double rise = 0.3 + 0.2 * random.nextDouble();
            Direction.Axis axis = Math.abs(Math.cos(angle)) > Math.abs(Math.sin(angle)) ? Direction.Axis.X : Direction.Axis.Z;
            BlockPos end = ground;
            for (int i = 1; i <= length; i++) {
                double x = cx + Math.cos(angle) * (trunk / 2.0 + i);
                double z = cz + Math.sin(angle) * (trunk / 2.0 + i);
                end = BlockPos.containing(x, ground.getY() + startY + i * rise, z);
                place(level, end, wood.log(axis), false);
                if (i == length / 2) {
                    leafBlob(level, end.above(), 2.0 + random.nextDouble(), wood, random);
                }
            }
            leafBlob(level, end, 3.0 + trunk * 0.3 + random.nextDouble() * 1.5, wood, random);
        }
        leafBlob(level, BlockPos.containing(cx, ground.getY() + height, cz), 5.0 + trunk * 1.6, wood, random);
    }

    private static BlockPos edge(BlockPos base, int trunk, Direction direction, int side) {
        return switch (direction) {
            case NORTH -> base.offset(side, 0, 0);
            case SOUTH -> base.offset(side, 0, trunk - 1);
            case WEST -> base.offset(0, 0, side);
            default -> base.offset(trunk - 1, 0, side);
        };
    }

    private static void leafBlob(ServerLevel level, BlockPos center, double radius, Wood wood, RandomSource random) {
        int r = Mth.ceil(radius);
        double flat = radius * 0.65;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    double d = (dx * dx + dz * dz) / (radius * radius) + (dy * dy) / (flat * flat);
                    if (d <= 1.0 - random.nextDouble() * 0.18) {
                        place(level, center.offset(dx, dy, dz), wood.leaves(), false);
                    }
                }
            }
        }
    }

    /**
     * Versteinert: ein niedriger Stumpf mit Borke aus Tropfstein und Jahresringen aus Kalzit und Tuff,
     * Wurzelansätze aus Stein und ein umgestürzter Stamm daneben – mit zwei bis vier Bernsteinen darin.
     */
    private static void petrified(ServerLevel level, BlockPos ground, RandomSource random) {
        BlockState bark = Blocks.DRIPSTONE_BLOCK.defaultBlockState();
        BlockState wood = Blocks.TUFF.defaultBlockState();
        BlockState core = Blocks.CALCITE.defaultBlockState();
        Map<BlockPos, BlockState> rock = new LinkedHashMap<>();
        List<BlockPos> outside = new ArrayList<>();
        int height = 2 + random.nextInt(2);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                boolean centre = dx == 0 && dz == 0;
                int top = dx != 0 && dz != 0 ? height - 1 : height;
                for (int y = -2; y < top; y++) {
                    BlockPos pos = ground.offset(dx, y, dz);
                    if (y == top - 1) {
                        // Die Schnittfläche oben: heller Kern, Ring aus Tuff
                        rock.put(pos, centre ? core : wood);
                    } else {
                        rock.put(pos, centre || random.nextInt(4) == 0 ? wood : bark);
                        if (!centre && y >= 0) {
                            outside.add(pos);
                        }
                    }
                }
            }
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos root = ground.relative(direction, 2);
            rock.put(root, bark);
            rock.put(root.below(), bark);
            outside.add(root);
        }
        // Der umgestürzte Stamm liegt etwas abseits, das abgebrochene Ende zeigt den hellen Kern
        Direction fall = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        BlockPos start = ground.relative(fall.getClockWise(), random.nextInt(3) - 1);
        int length = 5 + random.nextInt(4);
        for (int i = 3; i < 3 + length; i++) {
            BlockPos column = start.relative(fall, i);
            BlockPos log = new BlockPos(column.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ()), column.getZ());
            rock.put(log, i == 2 + length ? core : random.nextInt(4) == 0 ? wood : bark);
            outside.add(log);
        }
        List<BlockPos> shuffled = Util.toShuffledList(outside.stream(), random);
        for (BlockPos gem : shuffled.subList(0, Math.min(shuffled.size(), 2 + random.nextInt(3)))) {
            rock.put(gem, ModBlocks.AMBER_BLOCK.defaultBlockState());
        }
        rock.forEach((pos, state) -> place(level, pos, state, true));
    }

    /** Bäume ersetzen nur Natur – Häuser, Truhen und Wege der Spieler bleiben unberührt. */
    private static void place(ServerLevel level, BlockPos pos, BlockState state, boolean ground) {
        BlockState current = level.getBlockState(pos);
        if (current.hasBlockEntity() || level.isOutsideBuildHeight(pos)) {
            return;
        }
        boolean free = current.isAir() || current.canBeReplaced() || current.is(BlockTags.LEAVES) || current.is(BlockTags.REPLACEABLE_BY_TREES);
        if (free || (ground && TimeCapsules.isNatural(current))) {
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        }
    }

    /** Gewachsener Boden an dieser Stelle der Gegenwart (nie ein Dach); sonst in der Nähe; sonst nichts. */
    @Nullable
    private static BlockPos ground(ServerLevel level, BlockPos wanted) {
        for (int radius = 0; radius <= 6; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    int x = wanted.getX() + dx;
                    int z = wanted.getZ() + dz;
                    BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
                    BlockState below = level.getBlockState(top.below());
                    if (!below.isAir() && below.getFluidState().isEmpty() && TimeCapsules.isNatural(below) && !below.canBeReplaced()) {
                        return top;
                    }
                }
            }
        }
        return null;
    }
}
