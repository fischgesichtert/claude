package com.fischgesichtert.timemod.future;

import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Bäume für die Leinwand eines Chunks, Holz und Wuchs nach dem Biom: runde Eichen, schlanke Birken, spitze
 * Fichten, breite Schwarzeichen, hohe Tropenbäume, flache Akazien. Das Laub ist dauerhaft (es verwelkt nicht
 * fern vom Stamm), und die Krone bleibt im Chunk – sonst wächst hier kein Baum.
 */
final class Trees {
    enum Form {
        ROUND(2), SLENDER(2), CONE(2), BROAD(3), TALL(2), FLAT(3);

        final int reach;

        Form(int reach) {
            this.reach = reach;
        }
    }

    record Wood(Block log, Block leaves, Form form) {
        BlockState logState() {
            BlockState state = log.defaultBlockState();
            return state.hasProperty(RotatedPillarBlock.AXIS) ? state.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y) : state;
        }

        BlockState leavesState() {
            BlockState state = leaves.defaultBlockState();
            return state.hasProperty(LeavesBlock.PERSISTENT) ? state.setValue(LeavesBlock.PERSISTENT, true) : state;
        }
    }

    private Trees() {
    }

    static Wood of(Holder<Biome> biome) {
        if (biome.is(BiomeTags.IS_TAIGA) || biome.is(Biomes.SNOWY_PLAINS) || biome.is(Biomes.GROVE) || biome.is(Biomes.WINDSWEPT_FOREST)) {
            return new Wood(Blocks.SPRUCE_LOG, Blocks.SPRUCE_LEAVES, Form.CONE);
        }
        if (biome.is(BiomeTags.IS_JUNGLE)) {
            return new Wood(Blocks.JUNGLE_LOG, Blocks.JUNGLE_LEAVES, Form.TALL);
        }
        if (biome.is(Biomes.BIRCH_FOREST) || biome.is(Biomes.OLD_GROWTH_BIRCH_FOREST)) {
            return new Wood(Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES, Form.SLENDER);
        }
        if (biome.is(Biomes.DARK_FOREST)) {
            return new Wood(Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_LEAVES, Form.BROAD);
        }
        if (biome.is(BiomeTags.IS_SAVANNA)) {
            return new Wood(Blocks.ACACIA_LOG, Blocks.ACACIA_LEAVES, Form.FLAT);
        }
        if (biome.is(Biomes.CHERRY_GROVE)) {
            return new Wood(Blocks.CHERRY_LOG, Blocks.CHERRY_LEAVES, Form.ROUND);
        }
        if (biome.is(Biomes.MANGROVE_SWAMP)) {
            return new Wood(Blocks.MANGROVE_LOG, Blocks.MANGROVE_LEAVES, Form.ROUND);
        }
        return new Wood(Blocks.OAK_LOG, Blocks.OAK_LEAVES, Form.ROUND);
    }

    /**
     * Pflanzt einen Baum, dessen Stamm bei {@code y} beginnt ({@code size} 0..1: jung bis ausgewachsen).
     * Nur, wenn Stamm und Krone frei sind und ganz im Chunk liegen.
     */
    static boolean grow(Canvas canvas, int x, int y, int z, Wood wood, RandomSource random, double size) {
        Form form = wood.form();
        int trunk = form == Form.BROAD ? 2 : 1;
        int lx = x & 15;
        int lz = z & 15;
        if (lx < form.reach || lz < form.reach || lx + trunk - 1 > 15 - form.reach || lz + trunk - 1 > 15 - form.reach) {
            return false;
        }
        int bonus = size > 0.66 ? 1 : 0;
        int height = switch (form) {
            case ROUND -> 4 + random.nextInt(3) + bonus;
            case SLENDER -> 5 + random.nextInt(3) + bonus;
            case CONE -> 6 + random.nextInt(4) + 2 * bonus;
            case BROAD -> 5 + random.nextInt(2) + bonus;
            case TALL -> 8 + random.nextInt(4) + 2 * bonus;
            case FLAT -> 4 + random.nextInt(2);
        };
        if (y + height + 2 >= canvas.maxY() || !free(canvas, x, y, z, trunk, height, form.reach)) {
            return false;
        }
        BlockState log = wood.logState();
        for (int i = 0; i < height; i++) {
            for (int dx = 0; dx < trunk; dx++) {
                for (int dz = 0; dz < trunk; dz++) {
                    canvas.set(x + dx, y + i, z + dz, log);
                }
            }
        }
        BlockState leaves = wood.leavesState();
        int top = y + height - 1;
        switch (form) {
            case ROUND, TALL -> {
                layer(canvas, x, top - 2, z, trunk, 2, leaves, random);
                layer(canvas, x, top - 1, z, trunk, 2, leaves, random);
                layer(canvas, x, top, z, trunk, 1, leaves, random);
                layer(canvas, x, top + 1, z, trunk, 1, leaves, random);
            }
            case SLENDER -> {
                layer(canvas, x, top - 3, z, trunk, 1, leaves, random);
                layer(canvas, x, top - 2, z, trunk, 2, leaves, random);
                layer(canvas, x, top - 1, z, trunk, 2, leaves, random);
                layer(canvas, x, top, z, trunk, 1, leaves, random);
                layer(canvas, x, top + 1, z, trunk, 0, leaves, random);
            }
            case CONE -> {
                // Abwechselnd weite und enge Kränze, nach oben spitz
                for (int dy = 2; dy <= height; dy++) {
                    int radius = dy >= height - 1 ? 0 : (height - dy) % 2 == 0 ? 2 : 1;
                    if (dy < height - 4 && radius == 2 && random.nextInt(3) == 0) {
                        radius = 1;
                    }
                    layer(canvas, x, y + dy, z, trunk, radius, leaves, random);
                }
                canvas.set(x, y + height, z, leaves);
            }
            case BROAD -> {
                layer(canvas, x, top - 1, z, trunk, 3, leaves, random);
                layer(canvas, x, top, z, trunk, 3, leaves, random);
                layer(canvas, x, top + 1, z, trunk, 2, leaves, random);
            }
            case FLAT -> {
                layer(canvas, x, top, z, trunk, 3, leaves, random);
                layer(canvas, x, top + 1, z, trunk, 2, leaves, random);
            }
        }
        return true;
    }

    /** Ein Laubkranz um den Stamm; die Ecken fallen zufällig weg (bei Radius 0: nur die Spitze). */
    private static void layer(Canvas canvas, int x, int y, int z, int trunk, int radius, BlockState leaves, RandomSource random) {
        for (int dx = -radius; dx < trunk + radius; dx++) {
            for (int dz = -radius; dz < trunk + radius; dz++) {
                int ox = dx < 0 ? -dx : Math.max(0, dx - trunk + 1);
                int oz = dz < 0 ? -dz : Math.max(0, dz - trunk + 1);
                if (radius > 0 && ox == radius && oz == radius && random.nextInt(3) != 0) {
                    continue;
                }
                if (ox * ox + oz * oz > radius * radius + 1) {
                    continue;
                }
                BlockState current = canvas.at(x + dx, y, z + dz);
                if (current.isAir() || current.canBeReplaced()) {
                    canvas.set(x + dx, y, z + dz, leaves);
                }
            }
        }
    }

    /** Stamm frei bis über die Krone, im Kronenraum nichts Festes außer Laub. */
    private static boolean free(Canvas canvas, int x, int y, int z, int trunk, int height, int reach) {
        for (int dy = 0; dy <= height + 1; dy++) {
            boolean crown = dy >= Math.max(2, height - 4);
            int r = crown ? reach : 0;
            for (int dx = -r; dx < trunk + r; dx++) {
                for (int dz = -r; dz < trunk + r; dz++) {
                    BlockState state = canvas.at(x + dx, y + dy, z + dz);
                    if (state.isAir() || state.canBeReplaced() || (crown && state.is(BlockTags.LEAVES))) {
                        continue;
                    }
                    return false;
                }
            }
        }
        return true;
    }
}
