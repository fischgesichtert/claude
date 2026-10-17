package com.fischgesichtert.timemod.future.town;

import com.fischgesichtert.timemod.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Ein Gebäude der gewachsenen Siedlung, ganz aus Code: Fundament bis zum Boden, Stockwerke mit Eckpfosten,
 * Fenstern und Balken, ein Satteldach (in der Wüste flach) und eine Einrichtung je nach Zweck – Betten und
 * Arbeitsplätze für die Dorfbewohner, Bänke in der Kirche, Regale in der Bibliothek, Zifferblätter am Uhrturm.
 *
 * <p>Örtliche Koordinaten: Die Front mit der Tür liegt bei z = 0 und zeigt nach „Süden“ (−z); davor zwei
 * Reihen Vorgarten mit Weg, ringsum ein Block für den Dachüberstand.
 */
public class BuildingPiece extends TownPiece {
    public enum Kind {
        HOUSE, WORKSHOP, TAVERN, CHURCH, LIBRARY, TOWN_HALL, CLOCK_TOWER, MILL
    }

    static final int STOREY = 4;
    private static final int FRONT = 2;
    /** Arbeitsplätze: Daran erkennen Dorfbewohner ihren Beruf. */
    private static final Block[] JOBS = {Blocks.COMPOSTER, Blocks.SMOKER, Blocks.CARTOGRAPHY_TABLE, Blocks.BREWING_STAND, Blocks.BLAST_FURNACE,
        Blocks.BARREL, Blocks.FLETCHING_TABLE, Blocks.CAULDRON, Blocks.LECTERN, Blocks.STONECUTTER, Blocks.LOOM, Blocks.SMITHING_TABLE,
        Blocks.GRINDSTONE};

    private final Kind kind;
    private final int width;
    private final int depth;
    private final int floors;

    /**
     * @param corner Ecke des Grundstücks (mit Vorgarten und Dachüberstand) mit der kleinsten x/z
     * @param floorY Höhe des Erdgeschoss-Bodens (der oberste Geländeblock wird zum Boden)
     * @param facing wohin die Tür zeigt
     */
    public BuildingPiece(Kind kind, BlockPos corner, int floorY, Direction facing, int width, int depth, int floors, long seed, TownStyle style) {
        super(TownPieces.BUILDING, makeBoundingBox(corner.getX(), floorY, corner.getZ(), facing.getOpposite(), width + 2,
            height(kind, width, floors), depth + FRONT + 1), facing.getOpposite(), seed, style);
        this.kind = kind;
        this.width = width;
        this.depth = depth;
        this.floors = floors;
    }

    public BuildingPiece(CompoundTag tag) {
        super(TownPieces.BUILDING, tag);
        this.kind = Kind.values()[tag.getInt("Kind")];
        this.width = tag.getInt("Width");
        this.depth = tag.getInt("Depth");
        this.floors = tag.getInt("Floors");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putInt("Kind", kind.ordinal());
        tag.putInt("Width", width);
        tag.putInt("Depth", depth);
        tag.putInt("Floors", floors);
    }

    /** Außenmaß quer zur Front samt Dachüberstand. */
    public static int lotWidth(int width) {
        return width + 2;
    }

    /** Außenmaß in die Tiefe samt Vorgarten und Dachüberstand. */
    public static int lotDepth(int depth) {
        return depth + FRONT + 1;
    }

    static int height(Kind kind, int width, int floors) {
        int walls = kind == Kind.CLOCK_TOWER ? 24 : floors * STOREY;
        int extra = switch (kind) {
            case CHURCH -> 12;
            case CLOCK_TOWER -> 8;
            case TOWN_HALL -> 8;
            default -> 0;
        };
        return walls + width / 2 + 3 + extra;
    }

    // ------------------------------------------------------------------ Gebäude-Koordinaten (ohne Vorgarten/Überstand)

    private void set(WorldGenLevel level, BoundingBox box, int i, int y, int k, BlockState state) {
        put(level, box, i + 1, y, k + FRONT, state);
    }

    private BlockState get(WorldGenLevel level, BoundingBox box, int i, int y, int k) {
        return getBlock(level, i + 1, y, k + FRONT, box);
    }

    private int variant() {
        return (int) (seed & 0x7FFFFFFF);
    }

    /** Ein Bewohner an einer Stelle in Gebäude-Koordinaten. */
    private void resident(WorldGenLevel level, BoundingBox box, int i, int y, int k, boolean child) {
        villager(level, box, i + 1, y, k + FRONT, child);
    }

    private int top() {
        return kind == Kind.CLOCK_TOWER ? 24 : floors * STOREY;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox box,
                            ChunkPos chunkPos, BlockPos pivot) {
        int top = top();
        // Platz schaffen: Gelände und Pflanzen weg, auch für Dachüberstand und Vorgarten
        for (int y = 1; y <= top + width / 2 + 2; y++) {
            for (int i = -1; i <= width; i++) {
                for (int k = -FRONT; k <= depth; k++) {
                    set(level, box, i, y, k, OPEN);
                }
            }
        }
        // Fundament bis zum gewachsenen Boden, Böden, Vorgarten mit Weg zur Tür
        for (int i = 0; i < width; i++) {
            for (int k = 0; k < depth; k++) {
                boolean edge = i == 0 || k == 0 || i == width - 1 || k == depth - 1;
                set(level, box, i, 0, k, edge ? state(style.foundation()) : state(style.floor()));
                fillColumnDown(level, state(style.foundation()), i + 1, -1, k + FRONT, box);
            }
        }
        for (int i = -1; i <= width; i++) {
            for (int k = -FRONT; k < 0; k++) {
                boolean path = Math.abs(i - width / 2) <= (style.stage() >= 2 ? 1 : 0);
                set(level, box, i, 0, k, path ? state(style.road()) : groundCover(level, box, i, k));
                fillColumnDown(level, state(Blocks.DIRT), i + 1, -1, k + FRONT, box);
            }
        }
        if (kind == Kind.CLOCK_TOWER) {
            clockTower(level, box);
            return;
        }
        walls(level, box, top);
        if (style.type().equals("desert") && style.stage() < 4) {
            flatRoof(level, box, top);
        } else {
            gableRoof(level, box, top);
        }
        door(level, box);
        switch (kind) {
            case CHURCH -> church(level, box, top);
            case LIBRARY -> library(level, box);
            case TOWN_HALL -> townHall(level, box, top);
            case TAVERN -> tavern(level, box);
            case MILL -> mill(level, box, top);
            default -> house(level, box);
        }
    }

    /** Gras im Vorgarten, ab Stufe 3 mit Blumen. */
    private BlockState groundCover(WorldGenLevel level, BoundingBox box, int i, int k) {
        return style.type().equals("desert") ? state(Blocks.SAND) : state(Blocks.GRASS_BLOCK);
    }

    private void walls(WorldGenLevel level, BoundingBox box, int top) {
        boolean tall = kind == Kind.CHURCH || kind == Kind.LIBRARY;
        for (int y = 1; y <= top; y++) {
            int storey = (y - 1) / STOREY;
            boolean plate = y % STOREY == 0;
            for (int i = 0; i < width; i++) {
                for (int k = 0; k < depth; k++) {
                    boolean sideX = i == 0 || i == width - 1;
                    boolean sideZ = k == 0 || k == depth - 1;
                    if (!sideX && !sideZ) {
                        // Zwischendecken (die Kirche bleibt eine hohe Halle)
                        if (plate && y < top && kind != Kind.CHURCH) {
                            set(level, box, i, y, k, state(style.floor()));
                        }
                        continue;
                    }
                    BlockState wall;
                    if (sideX && sideZ) {
                        wall = style.pillar().defaultBlockState();
                    } else if (plate) {
                        wall = beam(sideX ? Direction.Axis.Z : Direction.Axis.X);
                    } else {
                        wall = storey == 0 ? state(style.wall()) : state(style.upper());
                    }
                    int along = sideZ ? i : k;
                    int length = sideZ ? width : depth;
                    int level1 = (y - 1) % STOREY;
                    boolean windowRow = level1 == 1 || (level1 == 2 && (tall || style.city()));
                    boolean windowColumn = along >= 2 && along <= length - 3 && (along % 2 == 0) && !(k == 0 && Math.abs(i - width / 2) <= 1);
                    if (!(sideX && sideZ) && !plate && windowRow && windowColumn) {
                        wall = window(sideX);
                    }
                    set(level, box, i, y, k, wall);
                }
            }
        }
        // Dachboden über dem obersten Stockwerk (für Laternen an der Decke)
        for (int i = 1; i < width - 1; i++) {
            for (int k = 1; k < depth - 1; k++) {
                if (kind != Kind.CHURCH) {
                    set(level, box, i, top, k, state(style.floor()));
                }
            }
        }
    }

    private BlockState beam(Direction.Axis axis) {
        BlockState beam = style.beam().defaultBlockState();
        return beam.hasProperty(RotatedPillarBlock.AXIS) ? beam.setValue(RotatedPillarBlock.AXIS, axis) : beam;
    }

    private BlockState window(boolean sideX) {
        BlockState pane = style.window().defaultBlockState();
        if (pane.hasProperty(BlockStateProperties.NORTH)) {
            pane = sideX ? pane.setValue(BlockStateProperties.NORTH, true).setValue(BlockStateProperties.SOUTH, true)
                : pane.setValue(BlockStateProperties.EAST, true).setValue(BlockStateProperties.WEST, true);
        }
        return pane;
    }

    private void door(WorldGenLevel level, BoundingBox box) {
        BlockState door = style.door().defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
        int i = width / 2;
        set(level, box, i, 1, 0, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(level, box, i, 2, 0, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        // Licht am Eingang: Fackeln, in der Stadt eine Laterne auf einem Pfosten
        if (style.city()) {
            set(level, box, i + 2, 1, -1, state(style.fence()));
            set(level, box, i + 2, 2, -1, Blocks.LANTERN.defaultBlockState());
        } else {
            set(level, box, i - 1, 2, -1, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH));
        }
    }

    /** Satteldach, First von vorn nach hinten; die Giebel füllt das Obergeschoss-Material. */
    private void gableRoof(WorldGenLevel level, BoundingBox box, int top) {
        int variant = variant();
        BlockState east = style.roofStairs(variant).setValue(StairBlock.FACING, Direction.EAST);
        BlockState west = style.roofStairs(variant).setValue(StairBlock.FACING, Direction.WEST);
        BlockState gable = state(style.upper());
        for (int j = 0; ; j++) {
            int y = top + 1 + j;
            int left = j - 1;
            int right = width - j;
            if (left > right) {
                break;
            }
            for (int k = -1; k <= depth; k++) {
                if (left == right) {
                    set(level, box, left, y, k, style.roofFull(variant));
                    continue;
                }
                set(level, box, left, y, k, east);
                set(level, box, right, y, k, west);
                if (k == 0 || k == depth - 1) {
                    for (int i = left + 1; i < right; i++) {
                        boolean vent = j == 1 && i == width / 2 && width >= 7;
                        set(level, box, i, y, k, vent ? window(false) : gable);
                    }
                }
            }
            if (left + 1 == right) {
                // Gerade Breite: der First ist eine Doppelreihe Platten
                break;
            }
        }
    }

    private void flatRoof(WorldGenLevel level, BoundingBox box, int top) {
        for (int i = 0; i < width; i++) {
            for (int k = 0; k < depth; k++) {
                boolean edge = i == 0 || k == 0 || i == width - 1 || k == depth - 1;
                set(level, box, i, top + 1, k, edge ? state(style.wall()) : style.roofFull(variant()));
                if (edge && (i + k) % 2 == 0) {
                    set(level, box, i, top + 2, k, state(style.wall()));
                }
            }
        }
    }

    // ------------------------------------------------------------------ Einrichtung

    private void house(WorldGenLevel level, BoundingBox box) {
        int floorsUsed = floors;
        for (int f = 0; f < floorsUsed; f++) {
            int y = f * STOREY + 1;
            bed(level, box, 1, y, depth - 3);
            if (width >= 7) {
                set(level, box, 2, y, depth - 2, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
            }
            hangingLantern(level, box, width / 2, f * STOREY + 3, depth / 2);
            set(level, box, width / 2, y, depth / 2, carpet());
        }
        // Arbeitsplatz unten vorn rechts, Treppenleiter hinten rechts
        Block job = kind == Kind.WORKSHOP || chance(1, 1, 1, 5) < 0.6F ? JOBS[Math.floorMod(variant() >> 3, JOBS.length)] : Blocks.CRAFTING_TABLE;
        set(level, box, width - 2, 1, 1, job.defaultBlockState());
        set(level, box, 1, 1, 1, Blocks.POTTED_POPPY.defaultBlockState());
        ladder(level, box);
        resident(level, box, width / 2 + 1, 1, 2, false);
        if (floors > 1 || chance(2, 2, 2, 6) < 0.3F) {
            resident(level, box, width / 2 - 1, 1, depth / 2, chance(3, 3, 3, 7) < 0.4F);
        }
    }

    private void tavern(WorldGenLevel level, BoundingBox box) {
        // Unten Schankraum: Theke aus Fässern, Tische aus Zaun und Druckplatte; oben Gästezimmer
        for (int i = 1; i < width - 1; i++) {
            set(level, box, i, 1, depth - 3, i == width / 2 ? OPEN : Blocks.BARREL.defaultBlockState());
        }
        for (int[] t : new int[][] {{2, 2}, {width - 3, 2}, {2, 4}, {width - 3, 4}}) {
            set(level, box, t[0], 1, t[1], state(style.fence()));
            set(level, box, t[0], 2, t[1], Blocks.OAK_PRESSURE_PLATE.defaultBlockState());
        }
        hangingLantern(level, box, width / 2, 3, 2);
        hangingLantern(level, box, width / 2, 3, depth - 4);
        for (int f = 1; f < floors; f++) {
            int y = f * STOREY + 1;
            bed(level, box, 1, y, depth - 3);
            bed(level, box, width - 2, y, depth - 3);
            hangingLantern(level, box, width / 2, y + 2, depth / 2);
        }
        ladder(level, box);
        resident(level, box, width / 2, 1, depth - 2, false);
        resident(level, box, 3, 1, 3, false);
    }

    private void church(WorldGenLevel level, BoundingBox box, int top) {
        // Bänke Richtung Altar, Altar mit Braustand (der Kleriker), Kerzenlicht
        for (int k = 2; k < depth - 4; k += 2) {
            for (int i = 1; i < width - 1; i++) {
                if (Math.abs(i - width / 2) > 0) {
                    set(level, box, i, 1, k, state(style.roof() == Blocks.WAXED_CUT_COPPER_STAIRS ? Blocks.BIRCH_STAIRS : Blocks.SPRUCE_STAIRS)
                        .setValue(StairBlock.FACING, Direction.SOUTH));
                }
            }
        }
        for (int i = 1; i < width - 1; i++) {
            set(level, box, i, 1, depth - 2, state(Blocks.SMOOTH_QUARTZ));
            set(level, box, i, 1, depth - 3, Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState());
        }
        set(level, box, width / 2, 2, depth - 2, Blocks.BREWING_STAND.defaultBlockState());
        set(level, box, width / 2 - 2, 2, depth - 2, Blocks.CANDLE.defaultBlockState().setValue(BlockStateProperties.CANDLES, 3));
        set(level, box, width / 2 + 2, 2, depth - 2, Blocks.CANDLE.defaultBlockState().setValue(BlockStateProperties.CANDLES, 3));
        for (int k = 3; k < depth - 3; k += 4) {
            hangingLantern(level, box, width / 2, top - 1, k);
        }
        // Glockenturm über dem Eingang
        int c = width / 2;
        int base = top + width / 2 + 1;
        for (int y = top + 1; y <= base + 8; y++) {
            for (int i = c - 1; i <= c + 1; i++) {
                for (int k = 0; k <= 2; k++) {
                    boolean edge = i != c || k != 1;
                    boolean arch = y >= base + 4 && y <= base + 6 && (i == c || k == 1);
                    if (edge && !arch) {
                        set(level, box, i, y, k, (i != c && k != 1) ? style.pillar().defaultBlockState() : state(style.wall()));
                    } else if (!edge && y < base + 4) {
                        set(level, box, i, y, k, state(style.wall()));
                    }
                }
            }
        }
        set(level, box, c, base + 6, 1, Blocks.BELL.defaultBlockState().setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING));
        for (int i = c - 1; i <= c + 1; i++) {
            for (int k = 0; k <= 2; k++) {
                set(level, box, i, base + 7, k, state(style.wall()));
            }
        }
        spire(level, box, c, base + 8, 1);
        resident(level, box, width / 2, 1, depth - 4, false);
    }

    /** Eine Turmspitze: Kupfer oder Dachmaterial, oben ein Blitzableiter. */
    private void spire(WorldGenLevel level, BoundingBox box, int i, int y, int k) {
        int variant = variant();
        for (int dy = 0; dy < 3; dy++) {
            set(level, box, i, y + dy, k, style.roofFull(variant + dy));
        }
        set(level, box, i - 1, y, k, style.roofStairs(variant).setValue(StairBlock.FACING, Direction.EAST));
        set(level, box, i + 1, y, k, style.roofStairs(variant).setValue(StairBlock.FACING, Direction.WEST));
        set(level, box, i, y, k - 1, style.roofStairs(variant).setValue(StairBlock.FACING, Direction.NORTH));
        set(level, box, i, y, k + 1, style.roofStairs(variant).setValue(StairBlock.FACING, Direction.SOUTH));
        set(level, box, i, y + 3, k, Blocks.LIGHTNING_ROD.defaultBlockState());
    }

    private void library(WorldGenLevel level, BoundingBox box) {
        for (int f = 0; f < floors; f++) {
            int y0 = f * STOREY + 1;
            for (int y = y0; y < y0 + 3; y++) {
                for (int i = 1; i < width - 1; i++) {
                    shelf(level, box, i, y, depth - 2);
                }
                for (int k = 2; k < depth - 2; k++) {
                    if (k % 4 != 0) {
                        shelf(level, box, 1, y, k);
                        shelf(level, box, width - 2, y, k);
                    }
                }
            }
            set(level, box, width / 2 - 2, y0, depth / 2, Blocks.LECTERN.defaultBlockState());
            set(level, box, width / 2 + 2, y0, depth / 2, Blocks.LECTERN.defaultBlockState());
            hangingLantern(level, box, width / 2, y0 + 2, depth / 2 - 2);
            hangingLantern(level, box, width / 2, y0 + 2, depth / 2 + 2);
        }
        ladder(level, box);
        resident(level, box, width / 2, 1, 2, false);
        resident(level, box, width / 2, 1, depth / 2 + 1, false);
    }

    private void shelf(WorldGenLevel level, BoundingBox box, int i, int y, int k) {
        if (get(level, box, i, y, k).isAir()) {
            set(level, box, i, y, k, chance(i, y, k, 31) < 0.15F ? Blocks.CHISELED_BOOKSHELF.defaultBlockState() : Blocks.BOOKSHELF.defaultBlockState());
        }
    }

    private void townHall(WorldGenLevel level, BoundingBox box, int top) {
        // Sitzungssaal mit langem Tisch, Banner an der Front, Glocke unter einem Türmchen
        for (int k = 3; k < depth - 3; k++) {
            set(level, box, width / 2, 1, k, state(Blocks.DARK_OAK_PLANKS));
            set(level, box, width / 2 - 1, 1, k, Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
            set(level, box, width / 2 + 1, 1, k, Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
        }
        for (int i = 2; i < width - 2; i += 2) {
            set(level, box, i, STOREY, -1, Blocks.RED_WALL_BANNER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
        }
        for (int f = 0; f < floors; f++) {
            hangingLantern(level, box, width / 2, f * STOREY + 3, depth / 2);
        }
        int c = width / 2;
        int y = top + width / 2 + 1;
        set(level, box, c, y, depth / 2, state(style.wall()));
        set(level, box, c, y + 1, depth / 2, Blocks.BELL.defaultBlockState().setValue(BellBlock.ATTACHMENT, BellAttachType.FLOOR)
            .setValue(BellBlock.FACING, Direction.SOUTH));
        for (int[] post : new int[][] {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
            set(level, box, c + post[0], y + 1, depth / 2 + post[1], state(style.fence()));
        }
        for (int di = -1; di <= 1; di++) {
            for (int dk = -1; dk <= 1; dk++) {
                set(level, box, c + di, y + 2, depth / 2 + dk, state(style.wall()));
            }
        }
        spire(level, box, c, y + 3, depth / 2);
        ladder(level, box);
        resident(level, box, 2, 1, 2, false);
        resident(level, box, width - 3, 1, 2, false);
    }

    /** Windmühle: Mahlstein drinnen, vorn das Flügelkreuz mit Segeln. */
    private void mill(WorldGenLevel level, BoundingBox box, int top) {
        set(level, box, width / 2, 1, depth / 2, Blocks.GRINDSTONE.defaultBlockState());
        set(level, box, 1, 1, depth - 2, Blocks.COMPOSTER.defaultBlockState());
        set(level, box, width - 2, 1, depth - 2, Blocks.HAY_BLOCK.defaultBlockState());
        hangingLantern(level, box, width / 2, 3, depth / 2 - 1);
        int c = width / 2;
        int hub = top + 1;
        set(level, box, c, hub, -1, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
        int arm = Math.min(width / 2 + 1, Math.min(5, hub - 3));
        for (int d = 1; d <= arm; d++) {
            set(level, box, c, hub + d, -1, state(Blocks.SPRUCE_FENCE));
            set(level, box, c, hub - d, -1, state(Blocks.SPRUCE_FENCE));
            set(level, box, c + d, hub, -1, state(Blocks.SPRUCE_FENCE));
            set(level, box, c - d, hub, -1, state(Blocks.SPRUCE_FENCE));
            if (d >= 2) {
                // Segel gegen den Uhrzeigersinn versetzt
                set(level, box, c + 1, hub + d, -1, state(Blocks.WHITE_WOOL));
                set(level, box, c - 1, hub - d, -1, state(Blocks.WHITE_WOOL));
                set(level, box, c + d, hub - 1, -1, state(Blocks.WHITE_WOOL));
                set(level, box, c - d, hub + 1, -1, state(Blocks.WHITE_WOOL));
            }
        }
        resident(level, box, c - 1, 1, 2, false);
    }

    /** Uhrturm der Stadt: schlanker Turm, vier Zifferblätter, Glocke und Kupferspitze. */
    private void clockTower(WorldGenLevel level, BoundingBox box) {
        int top = 24;
        for (int y = 1; y <= top; y++) {
            for (int i = 0; i < width; i++) {
                for (int k = 0; k < depth; k++) {
                    boolean edge = i == 0 || k == 0 || i == width - 1 || k == depth - 1;
                    boolean corner = (i == 0 || i == width - 1) && (k == 0 || k == depth - 1);
                    if (edge) {
                        set(level, box, i, y, k, corner ? style.pillar().defaultBlockState() : y % 8 == 0 ? beam(i == 0 || i == width - 1
                            ? Direction.Axis.Z : Direction.Axis.X) : state(style.wall()));
                    } else if (y % 8 == 0) {
                        set(level, box, i, y, k, state(style.floor()));
                    }
                }
            }
        }
        // Zifferblätter (Quarz, Messingring, Zeiger aus Schwarzstein) an allen vier Seiten
        int cy = 19;
        int c = width / 2;
        for (int dy = -2; dy <= 2; dy++) {
            for (int d = -2; d <= 2; d++) {
                double r = Math.sqrt(d * d + dy * dy);
                BlockState face = r <= 1.6 ? state(Blocks.QUARTZ_BLOCK) : r <= 2.9 ? ModBlocks.BRASS_BLOCK.defaultBlockState() : null;
                if (face == null) {
                    continue;
                }
                set(level, box, c + d, cy + dy, -1, face);
                set(level, box, c - d, cy + dy, depth, face);
                set(level, box, -1, cy + dy, c - d, face);
                set(level, box, width, cy + dy, c + d, face);
            }
        }
        BlockState hand = state(Blocks.POLISHED_BLACKSTONE);
        for (int[] h : new int[][] {{0, 1}, {1, 0}}) {
            set(level, box, c + h[0], cy + h[1], -1, hand);
            set(level, box, c - h[0], cy + h[1], depth, hand);
            set(level, box, -1, cy + h[1], c - h[0], hand);
            set(level, box, width, cy + h[1], c + h[0], hand);
        }
        door(level, box);
        ladder(level, box);
        // Offene Glockenstube und Spitze
        for (int i = 0; i < width; i++) {
            for (int k = 0; k < depth; k++) {
                boolean edge = i == 0 || k == 0 || i == width - 1 || k == depth - 1;
                boolean corner = (i == 0 || i == width - 1) && (k == 0 || k == depth - 1);
                for (int y = top + 1; y <= top + 3; y++) {
                    set(level, box, i, y, k, corner ? style.pillar().defaultBlockState() : OPEN);
                }
                set(level, box, i, top + 4, k, edge ? state(style.wall()) : state(style.floor()));
            }
        }
        set(level, box, c, top + 3, c, Blocks.BELL.defaultBlockState().setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING));
        int variant = variant();
        for (int j = 0; j <= width / 2; j++) {
            for (int i = j - 1; i <= width - j; i++) {
                for (int k = j - 1; k <= depth - j; k++) {
                    boolean ring = i == j - 1 || i == width - j || k == j - 1 || k == depth - j;
                    if (ring) {
                        set(level, box, i, top + 5 + j, k, style.roofFull(variant + j));
                    }
                }
            }
        }
        set(level, box, c, top + 6 + width / 2, c, Blocks.LIGHTNING_ROD.defaultBlockState());
    }

    // ------------------------------------------------------------------ Bausteine

    private void bed(WorldGenLevel level, BoundingBox box, int i, int y, int k) {
        BlockState bed = (style.city() ? Blocks.LIGHT_BLUE_BED : Blocks.RED_BED).defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH);
        set(level, box, i, y, k, bed.setValue(BedBlock.PART, BedPart.FOOT));
        set(level, box, i, y, k + 1, bed.setValue(BedBlock.PART, BedPart.HEAD));
    }

    private void hangingLantern(WorldGenLevel level, BoundingBox box, int i, int y, int k) {
        set(level, box, i, y, k, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
    }

    private BlockState carpet() {
        return switch (style.type()) {
            case "desert" -> Blocks.ORANGE_CARPET.defaultBlockState();
            case "savanna" -> Blocks.YELLOW_CARPET.defaultBlockState();
            case "snowy" -> Blocks.LIGHT_BLUE_CARPET.defaultBlockState();
            case "taiga" -> Blocks.BROWN_CARPET.defaultBlockState();
            default -> style.city() ? Blocks.CYAN_CARPET.defaultBlockState() : Blocks.RED_CARPET.defaultBlockState();
        };
    }

    /** Eine Leiter innen an der rechten Wand durch alle Stockwerke (mit Loch in den Decken). */
    private void ladder(WorldGenLevel level, BoundingBox box) {
        int top = top();
        if (top <= STOREY) {
            return;
        }
        // Ungerade Reihe: dort ist nie ein Fenster, die Leiter hält an der Wand
        int k = (depth / 2) | 1;
        int last = kind == Kind.CLOCK_TOWER ? top : top - 1;
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST);
        for (int y = 1; y <= last; y++) {
            set(level, box, width - 2, y, k, ladder);
        }
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public String toString() {
        return "Building[" + kind + " " + width + "x" + depth + "x" + floors + " " + style.type() + "/" + style.stage() + "]";
    }

    /** Für die Halbstufen der Stadtmauer und Brücken: eine untere Platte. */
    static BlockState bottomSlab(Block slab) {
        BlockState state = slab.defaultBlockState();
        return state.hasProperty(SlabBlock.TYPE) ? state.setValue(SlabBlock.TYPE, SlabType.BOTTOM) : state;
    }
}
