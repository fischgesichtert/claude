package com.fischgesichtert.timemod.worldgen;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Vergessenes Observatorium auf einem Berggipfel: weißer Rundbau, Kupferkuppel mit Spalt, ein
 * Messing-Teleskop, Bücherregale und das Notizbuch des Astronomen mit Hinweisen auf andere Zeitalter.
 */
public class ObservatoryPiece extends TimePiece {
    public static final ResourceLocation LOOT = TimeMod.id("chests/observatory");
    static final int SIZE = 17;
    static final int HEIGHT = 16;
    private static final int C = 8;
    private static final double WALL = 6.5;

    public ObservatoryPiece(BlockPos origin, Direction orientation, long seed) {
        super(ModStructures.OBSERVATORY_PIECE, makeBoundingBox(origin.getX(), origin.getY(), origin.getZ(), orientation, SIZE, HEIGHT, SIZE), orientation, seed);
    }

    public ObservatoryPiece(CompoundTag tag) {
        super(ModStructures.OBSERVATORY_PIECE, tag);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                double d = Math.hypot(x - C, z - C);
                if (d > 8.4) {
                    continue;
                }
                // Plattform mit Fundament, innen ein dunkler Sternboden
                BlockState floor;
                if (d < WALL - 0.5) {
                    float c = chance(x, 0, z, 1);
                    floor = c < 0.07F ? Blocks.SEA_LANTERN.defaultBlockState()
                        : c < 0.12F ? Blocks.LAPIS_BLOCK.defaultBlockState() : Blocks.BLACK_CONCRETE.defaultBlockState();
                } else {
                    floor = pick(x, 0, z, 2, Blocks.POLISHED_ANDESITE.defaultBlockState(), 0.2F, Blocks.ANDESITE.defaultBlockState(),
                        0.1F, Blocks.COBBLESTONE.defaultBlockState());
                }
                put(level, box, x, 0, z, floor);
                fillColumnDown(level, Blocks.STONE.defaultBlockState(), x, -1, z, box);
                // Innenraum frei
                for (int y = 1; y < HEIGHT; y++) {
                    double r = Math.sqrt((x - C) * (x - C) + (y - 5.0) * (y - 5.0) * (y > 5 ? 1 : 0) + (z - C) * (z - C));
                    if (r < WALL - 0.5 || (y <= 5 && d < WALL - 0.5)) {
                        put(level, box, x, y, z, AIR);
                    }
                }
                // Außenmauer
                if (d >= WALL - 0.5 && d <= WALL + 0.6) {
                    for (int y = 1; y <= 5; y++) {
                        boolean window = (y == 2 || y == 3) && windowAngle(x, z);
                        BlockState wall = window ? Blocks.GLASS.defaultBlockState() : pick(x, y, z, 3, Blocks.CALCITE.defaultBlockState(),
                            0.25F, Blocks.POLISHED_DIORITE.defaultBlockState(), 0.06F, Blocks.CRACKED_STONE_BRICKS.defaultBlockState());
                        put(level, box, x, y, z, wall);
                    }
                } else if (d > WALL + 0.6 && d <= 8.4 && chance(x, 1, z, 4) < 0.35F) {
                    put(level, box, x, 1, z, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1 + (int) (chance(x, 2, z, 5) * 3)));
                }
            }
        }
        dome(level, box);
        telescope(level, box);
        furnishings(level, box, random);
    }

    private boolean windowAngle(int x, int z) {
        double a = Math.toDegrees(Math.atan2(z - C, x - C));
        double m = ((a % 45) + 45) % 45;
        return m < 9 || m > 36;
    }

    /** Halbkugel aus gealtertem Kupfer mit Glasbändern und einem Spalt für das Teleskop. */
    private void dome(WorldGenLevel level, BoundingBox box) {
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                for (int y = 6; y < HEIGHT; y++) {
                    double r = Math.sqrt((x - C) * (x - C) + (y - 5.0) * (y - 5.0) + (z - C) * (z - C));
                    if (r < WALL - 0.5 || r > WALL + 0.6) {
                        continue;
                    }
                    boolean slit = Math.abs(x - C) <= 1 && z > C;
                    if (slit) {
                        continue;
                    }
                    double a = Math.toDegrees(Math.atan2(z - C, x - C));
                    boolean band = (((int) Math.floor((a + 360) / 30)) % 2 == 0) && y > 7 && y < 11;
                    BlockState state;
                    if (band) {
                        state = chance(x, y, z, 6) < 0.25F ? AIR : Blocks.TINTED_GLASS.defaultBlockState();
                    } else {
                        float c = chance(x, y, z, 7);
                        state = c < 0.45F ? Blocks.OXIDIZED_COPPER.defaultBlockState()
                            : c < 0.8F ? Blocks.WEATHERED_COPPER.defaultBlockState() : Blocks.EXPOSED_COPPER.defaultBlockState();
                    }
                    put(level, box, x, y, z, state);
                }
            }
        }
    }

    /** Messing-Teleskop auf Steinsockel, schräg durch den Spalt in den Himmel gerichtet. */
    private void telescope(WorldGenLevel level, BoundingBox box) {
        fill(level, box, C - 1, 1, C - 1, C + 1, 1, C + 1, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        put(level, box, C, 2, C, Blocks.POLISHED_DEEPSLATE_WALL.defaultBlockState());
        put(level, box, C, 3, C, ModBlocks.BRASS_BLOCK.defaultBlockState());
        for (int i = 0; i <= 7; i++) {
            int y = 4 + i;
            int z = C + i;
            put(level, box, C, y, z, i == 7 ? Blocks.GLASS.defaultBlockState() : ModBlocks.BRASS_BLOCK.defaultBlockState());
            if (i < 6 && i % 2 == 0) {
                put(level, box, C, y - 1, z + 1, Blocks.CHAIN.defaultBlockState());
            }
        }
    }

    private void furnishings(WorldGenLevel level, BoundingBox box, RandomSource random) {
        // Bücherregale entlang der Mauer, Kerzen, Kartentisch
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                double d = Math.hypot(x - C, z - C);
                if (d >= WALL - 1.5 && d < WALL - 0.5 && z < C - 1) {
                    for (int y = 1; y <= 3; y++) {
                        BlockState shelf = chance(x, y, z, 8) < 0.3F ? Blocks.AIR.defaultBlockState() : Blocks.BOOKSHELF.defaultBlockState();
                        put(level, box, x, y, z, shelf);
                    }
                    if (chance(x, 4, z, 9) < 0.5F && !isAir(level, box, x, 3, z)) {
                        put(level, box, x, 4, z, Blocks.CANDLE.defaultBlockState());
                    }
                }
                double r = Math.hypot(x - C, z - C);
                if (r >= 2.5 && r < 4.5 && chance(x, 1, z, 10) < 0.06F) {
                    put(level, box, x, 1, z, Blocks.COBWEB.defaultBlockState());
                }
            }
        }
        put(level, box, C + 3, 1, C - 2, Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
        put(level, box, C - 3, 1, C - 2, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.EAST).setValue(LecternBlock.HAS_BOOK, true));
        BlockPos lecternPos = getWorldPos(C - 3, 1, C - 2);
        if (box.isInside(lecternPos) && level.getBlockEntity(lecternPos) instanceof LecternBlockEntity lectern) {
            lectern.setBook(astronomerNotes());
        }
        createChest(level, box, random, C + 4, 1, C + 2, LOOT);
        createChest(level, box, random, C - 4, 1, C + 3, LOOT);
    }

    /** Notizbuch des Astronomen: übersetzbare Seiten mit Hinweisen auf vergangene und kommende Zeitalter. */
    public static ItemStack astronomerNotes() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = book.getOrCreateTag();
        tag.putString("title", "Notizen");
        tag.putString("author", "Astronom");
        ListTag pages = new ListTag();
        for (int i = 1; i <= 5; i++) {
            pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable("book.timemod.astronomer.page" + i))));
        }
        tag.put("pages", pages);
        tag.putBoolean("resolved", true);
        book.setHoverName(Component.translatable("book.timemod.astronomer.title"));
        return book;
    }
}
