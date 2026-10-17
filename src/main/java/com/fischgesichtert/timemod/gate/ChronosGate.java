package com.fischgesichtert.timemod.gate;

import com.fischgesichtert.timemod.registry.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Das Chronos-Tor: ein stehender Ring aus 24 Tor-Rahmen auf einem 7×7-Feld mit abgerundeten Ecken.
 * Innen bleiben 21 Felder frei – dort öffnet sich das Portal.
 */
public final class ChronosGate {
    public static final int SIZE = 7;
    /** Zeilen von unten (0) nach oben (6): F = Rahmen, P = Öffnung, . = egal. */
    private static final String[] ROWS = {
        ".FFFFF.",
        "FFPPPFF",
        "FPPPPPF",
        "FPPPPPF",
        "FPPPPPF",
        "FFPPPFF",
        ".FFFFF.",
    };
    public static final int FRAME_BLOCKS = count('F');
    public static final int OPENING_BLOCKS = count('P');

    private ChronosGate() {
    }

    private static int count(char kind) {
        int n = 0;
        for (String row : ROWS) {
            n += (int) row.chars().filter(c -> c == kind).count();
        }
        return n;
    }

    /** Ein Ring: linke untere Ecke des 7×7-Feldes und die waagrechte Achse, entlang der er steht. */
    public record Shape(BlockPos origin, Direction.Axis axis) {
        public BlockPos at(int column, int row) {
            return axis == Direction.Axis.X ? origin.offset(column, row, 0) : origin.offset(0, row, column);
        }

        /** Mittelpunkt der Öffnung (Blockmitte). */
        public Vec3 center() {
            return Vec3.atCenterOf(at(3, 3));
        }

        public BlockPos bottomCenter() {
            return at(3, 0);
        }

        public List<BlockPos> frame() {
            return cells('F');
        }

        public List<BlockPos> opening() {
            return cells('P');
        }

        private List<BlockPos> cells(char kind) {
            List<BlockPos> cells = new ArrayList<>();
            for (int row = 0; row < SIZE; row++) {
                for (int column = 0; column < SIZE; column++) {
                    if (ROWS[row].charAt(column) == kind) {
                        cells.add(at(column, row));
                    }
                }
            }
            return cells;
        }

        public boolean contains(BlockPos pos) {
            for (int row = 0; row < SIZE; row++) {
                for (int column = 0; column < SIZE; column++) {
                    if (ROWS[row].charAt(column) != '.' && at(column, row).equals(pos)) {
                        return true;
                    }
                }
            }
            return false;
        }

        public CompoundTag save() {
            CompoundTag tag = NbtUtils.writeBlockPos(origin);
            tag.putString("Axis", axis.getSerializedName());
            return tag;
        }

        @Nullable
        public static Shape load(CompoundTag tag) {
            Direction.Axis axis = Direction.Axis.byName(tag.getString("Axis"));
            return axis == null || axis == Direction.Axis.Y ? null : new Shape(NbtUtils.readBlockPos(tag), axis);
        }
    }

    public static boolean isFrame(BlockState state) {
        return state.is(ModBlocks.CHRONOS_GATE_FRAME);
    }

    /** Sucht einen vollständigen Ring, dessen Unterkante höchstens {@code radius} Blöcke entfernt ist. */
    @Nullable
    public static Shape find(Level level, BlockPos near, int radius) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    pos.set(near.getX() + dx, near.getY() + dy, near.getZ() + dz);
                    if (!isFrame(level.getBlockState(pos))) {
                        continue;
                    }
                    // Jeder Rahmenblock könnte die Mitte der untersten Reihe sein
                    for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
                        BlockPos origin = axis == Direction.Axis.X ? pos.offset(-3, 0, 0) : pos.offset(0, 0, -3);
                        Shape shape = new Shape(origin.immutable(), axis);
                        if (isFrameComplete(level, shape)) {
                            return shape;
                        }
                    }
                }
            }
        }
        return null;
    }

    public static boolean isFrameComplete(Level level, Shape shape) {
        for (BlockPos pos : shape.frame()) {
            if (!isFrame(level.getBlockState(pos))) {
                return false;
            }
        }
        return true;
    }

    /** Ob sich das Portal öffnen kann: innen nur Luft, ersetzbare Pflanzen oder schon Portal. */
    public static boolean isOpeningClear(Level level, Shape shape) {
        for (BlockPos pos : shape.opening()) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && !state.is(ModBlocks.CHRONOS_PORTAL) && !(state.canBeReplaced() && state.getFluidState().isEmpty())) {
                return false;
            }
        }
        return true;
    }
}
