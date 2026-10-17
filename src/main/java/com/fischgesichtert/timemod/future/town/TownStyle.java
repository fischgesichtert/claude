package com.fischgesichtert.timemod.future.town;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Die Baustoffe einer Siedlung – nach der Art des Dorfes (Ebene, Wüste, Savanne, Schnee, Taiga) und der
 * Stufe: erst wie heute aus Holz, dann Stein, dann Fachwerk, zuletzt eine eigene Architektur aus hellem
 * Stein, Glas und Kupfer.
 */
public record TownStyle(String type, int stage, Block foundation, Block wall, Block upper, Block pillar, Block beam, Block floor,
                        Block roof, Block roofBlock, Block window, Block door, Block fence, Block road, Block roadAccent, Block wallStone,
                        Block wallCap) {
    public static TownStyle of(String type, int stage) {
        return switch (type) {
            case "desert" -> desert(stage);
            case "savanna" -> savanna(stage);
            case "snowy" -> snowy(stage);
            case "taiga" -> taiga(stage);
            default -> plains(stage);
        };
    }

    /** Aus dem Namen der Dorf-Struktur, etwa {@code minecraft:village_desert}. */
    public static String typeOf(String structure) {
        for (String type : new String[] {"desert", "savanna", "snowy", "taiga"}) {
            if (structure.contains(type)) {
                return type;
            }
        }
        return "plains";
    }

    private static TownStyle plains(int stage) {
        return switch (stage) {
            case 0, 1 -> new TownStyle("plains", stage, Blocks.COBBLESTONE, Blocks.OAK_PLANKS, Blocks.OAK_PLANKS, Blocks.OAK_LOG,
                Blocks.STRIPPED_OAK_LOG, Blocks.OAK_PLANKS, Blocks.OAK_STAIRS, Blocks.OAK_PLANKS, Blocks.GLASS_PANE, Blocks.OAK_DOOR,
                Blocks.OAK_FENCE, Blocks.DIRT_PATH, Blocks.DIRT_PATH, Blocks.COBBLESTONE, Blocks.COBBLESTONE_WALL);
            case 2 -> new TownStyle("plains", stage, Blocks.COBBLESTONE, Blocks.STONE_BRICKS, Blocks.OAK_PLANKS, Blocks.STRIPPED_OAK_LOG,
                Blocks.STRIPPED_OAK_LOG, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_PLANKS, Blocks.GLASS_PANE, Blocks.SPRUCE_DOOR,
                Blocks.SPRUCE_FENCE, Blocks.COBBLESTONE, Blocks.GRAVEL, Blocks.COBBLESTONE, Blocks.COBBLESTONE_WALL);
            case 3 -> new TownStyle("plains", stage, Blocks.STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.WHITE_TERRACOTTA, Blocks.DARK_OAK_LOG,
                Blocks.DARK_OAK_LOG, Blocks.SPRUCE_PLANKS, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILES, Blocks.GLASS_PANE,
                Blocks.DARK_OAK_DOOR, Blocks.DARK_OAK_FENCE, Blocks.STONE_BRICKS, Blocks.COBBLESTONE, Blocks.STONE_BRICKS,
                Blocks.STONE_BRICK_WALL);
            default -> new TownStyle("plains", stage, Blocks.POLISHED_ANDESITE, Blocks.CALCITE, Blocks.SMOOTH_QUARTZ, Blocks.POLISHED_DIORITE,
                Blocks.STRIPPED_BIRCH_LOG, Blocks.BIRCH_PLANKS, Blocks.WAXED_CUT_COPPER_STAIRS, Blocks.WAXED_CUT_COPPER, Blocks.GLASS_PANE,
                Blocks.BIRCH_DOOR, Blocks.BIRCH_FENCE, Blocks.POLISHED_ANDESITE, Blocks.STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.POLISHED_ANDESITE);
        };
    }

    private static TownStyle desert(int stage) {
        return switch (stage) {
            case 0, 1 -> new TownStyle("desert", stage, Blocks.SANDSTONE, Blocks.SANDSTONE, Blocks.SANDSTONE, Blocks.CUT_SANDSTONE,
                Blocks.STRIPPED_JUNGLE_LOG, Blocks.SMOOTH_SANDSTONE, Blocks.SANDSTONE_STAIRS, Blocks.SMOOTH_SANDSTONE, Blocks.GLASS_PANE,
                Blocks.JUNGLE_DOOR, Blocks.JUNGLE_FENCE, Blocks.SMOOTH_SANDSTONE, Blocks.SAND, Blocks.SANDSTONE, Blocks.SANDSTONE_WALL);
            case 2 -> new TownStyle("desert", stage, Blocks.CUT_SANDSTONE, Blocks.SMOOTH_SANDSTONE, Blocks.SANDSTONE, Blocks.CHISELED_SANDSTONE,
                Blocks.STRIPPED_JUNGLE_LOG, Blocks.SMOOTH_SANDSTONE, Blocks.SMOOTH_SANDSTONE_STAIRS, Blocks.SMOOTH_SANDSTONE, Blocks.GLASS_PANE,
                Blocks.JUNGLE_DOOR, Blocks.JUNGLE_FENCE, Blocks.CUT_SANDSTONE, Blocks.SMOOTH_SANDSTONE, Blocks.SANDSTONE, Blocks.SANDSTONE_WALL);
            case 3 -> new TownStyle("desert", stage, Blocks.CUT_SANDSTONE, Blocks.SMOOTH_SANDSTONE, Blocks.ORANGE_TERRACOTTA, Blocks.CUT_RED_SANDSTONE,
                Blocks.CUT_RED_SANDSTONE, Blocks.SMOOTH_SANDSTONE, Blocks.SMOOTH_RED_SANDSTONE_STAIRS, Blocks.SMOOTH_RED_SANDSTONE,
                Blocks.GLASS_PANE, Blocks.JUNGLE_DOOR, Blocks.JUNGLE_FENCE, Blocks.CUT_SANDSTONE, Blocks.SMOOTH_SANDSTONE, Blocks.CUT_SANDSTONE,
                Blocks.SANDSTONE_WALL);
            default -> new TownStyle("desert", stage, Blocks.SMOOTH_SANDSTONE, Blocks.SMOOTH_QUARTZ, Blocks.SMOOTH_SANDSTONE,
                Blocks.CHISELED_SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.SMOOTH_SANDSTONE, Blocks.WAXED_CUT_COPPER_STAIRS, Blocks.WAXED_CUT_COPPER,
                Blocks.GLASS_PANE, Blocks.JUNGLE_DOOR, Blocks.JUNGLE_FENCE, Blocks.SMOOTH_SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.CUT_SANDSTONE,
                Blocks.SMOOTH_SANDSTONE);
        };
    }

    private static TownStyle savanna(int stage) {
        return switch (stage) {
            case 0, 1 -> new TownStyle("savanna", stage, Blocks.COBBLESTONE, Blocks.ACACIA_PLANKS, Blocks.ORANGE_TERRACOTTA, Blocks.ACACIA_LOG,
                Blocks.STRIPPED_ACACIA_LOG, Blocks.ACACIA_PLANKS, Blocks.ACACIA_STAIRS, Blocks.ACACIA_PLANKS, Blocks.GLASS_PANE, Blocks.ACACIA_DOOR,
                Blocks.ACACIA_FENCE, Blocks.DIRT_PATH, Blocks.COARSE_DIRT, Blocks.COBBLESTONE, Blocks.COBBLESTONE_WALL);
            case 2 -> new TownStyle("savanna", stage, Blocks.COBBLESTONE, Blocks.TERRACOTTA, Blocks.ACACIA_PLANKS, Blocks.STRIPPED_ACACIA_LOG,
                Blocks.STRIPPED_ACACIA_LOG, Blocks.ACACIA_PLANKS, Blocks.ACACIA_STAIRS, Blocks.ACACIA_PLANKS, Blocks.GLASS_PANE, Blocks.ACACIA_DOOR,
                Blocks.ACACIA_FENCE, Blocks.PACKED_MUD, Blocks.COARSE_DIRT, Blocks.MUD_BRICKS, Blocks.MUD_BRICK_WALL);
            case 3 -> new TownStyle("savanna", stage, Blocks.MUD_BRICKS, Blocks.TERRACOTTA, Blocks.ORANGE_TERRACOTTA, Blocks.ACACIA_LOG,
                Blocks.ACACIA_LOG, Blocks.ACACIA_PLANKS, Blocks.MUD_BRICK_STAIRS, Blocks.MUD_BRICKS, Blocks.GLASS_PANE, Blocks.ACACIA_DOOR,
                Blocks.ACACIA_FENCE, Blocks.MUD_BRICKS, Blocks.PACKED_MUD, Blocks.MUD_BRICKS, Blocks.MUD_BRICK_WALL);
            default -> new TownStyle("savanna", stage, Blocks.POLISHED_GRANITE, Blocks.SMOOTH_QUARTZ, Blocks.WHITE_TERRACOTTA, Blocks.ACACIA_LOG,
                Blocks.STRIPPED_ACACIA_LOG, Blocks.ACACIA_PLANKS, Blocks.WAXED_CUT_COPPER_STAIRS, Blocks.WAXED_CUT_COPPER, Blocks.GLASS_PANE,
                Blocks.ACACIA_DOOR, Blocks.ACACIA_FENCE, Blocks.POLISHED_GRANITE, Blocks.MUD_BRICKS, Blocks.MUD_BRICKS, Blocks.POLISHED_GRANITE);
        };
    }

    private static TownStyle snowy(int stage) {
        return switch (stage) {
            case 0, 1 -> new TownStyle("snowy", stage, Blocks.COBBLESTONE, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_LOG,
                Blocks.STRIPPED_SPRUCE_LOG, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_PLANKS, Blocks.GLASS_PANE, Blocks.SPRUCE_DOOR,
                Blocks.SPRUCE_FENCE, Blocks.DIRT_PATH, Blocks.PACKED_ICE, Blocks.COBBLESTONE, Blocks.COBBLESTONE_WALL);
            case 2 -> new TownStyle("snowy", stage, Blocks.COBBLESTONE, Blocks.STONE_BRICKS, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_LOG,
                Blocks.STRIPPED_SPRUCE_LOG, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_PLANKS, Blocks.GLASS_PANE, Blocks.SPRUCE_DOOR,
                Blocks.SPRUCE_FENCE, Blocks.COBBLESTONE, Blocks.GRAVEL, Blocks.STONE_BRICKS, Blocks.STONE_BRICK_WALL);
            case 3 -> new TownStyle("snowy", stage, Blocks.STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_LOG,
                Blocks.STRIPPED_DARK_OAK_LOG, Blocks.SPRUCE_PLANKS, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILES, Blocks.GLASS_PANE,
                Blocks.SPRUCE_DOOR, Blocks.SPRUCE_FENCE, Blocks.STONE_BRICKS, Blocks.ANDESITE, Blocks.STONE_BRICKS, Blocks.STONE_BRICK_WALL);
            default -> new TownStyle("snowy", stage, Blocks.POLISHED_ANDESITE, Blocks.STONE_BRICKS, Blocks.SMOOTH_QUARTZ, Blocks.STRIPPED_SPRUCE_LOG,
                Blocks.STRIPPED_SPRUCE_LOG, Blocks.SPRUCE_PLANKS, Blocks.WAXED_CUT_COPPER_STAIRS, Blocks.WAXED_CUT_COPPER, Blocks.GLASS_PANE,
                Blocks.SPRUCE_DOOR, Blocks.SPRUCE_FENCE, Blocks.POLISHED_ANDESITE, Blocks.STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.POLISHED_ANDESITE);
        };
    }

    private static TownStyle taiga(int stage) {
        return switch (stage) {
            case 0, 1 -> new TownStyle("taiga", stage, Blocks.MOSSY_COBBLESTONE, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_LOG,
                Blocks.STRIPPED_SPRUCE_LOG, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_PLANKS, Blocks.GLASS_PANE, Blocks.SPRUCE_DOOR,
                Blocks.SPRUCE_FENCE, Blocks.DIRT_PATH, Blocks.COARSE_DIRT, Blocks.COBBLESTONE, Blocks.COBBLESTONE_WALL);
            case 2 -> new TownStyle("taiga", stage, Blocks.COBBLESTONE, Blocks.COBBLESTONE, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_LOG,
                Blocks.STRIPPED_SPRUCE_LOG, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_PLANKS, Blocks.GLASS_PANE, Blocks.SPRUCE_DOOR,
                Blocks.SPRUCE_FENCE, Blocks.COBBLESTONE, Blocks.GRAVEL, Blocks.MOSSY_COBBLESTONE, Blocks.MOSSY_COBBLESTONE_WALL);
            case 3 -> new TownStyle("taiga", stage, Blocks.DEEPSLATE_BRICKS, Blocks.COBBLED_DEEPSLATE, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_LOG,
                Blocks.STRIPPED_SPRUCE_LOG, Blocks.SPRUCE_PLANKS, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILES, Blocks.GLASS_PANE,
                Blocks.SPRUCE_DOOR, Blocks.SPRUCE_FENCE, Blocks.DEEPSLATE_BRICKS, Blocks.COBBLED_DEEPSLATE, Blocks.DEEPSLATE_BRICKS,
                Blocks.DEEPSLATE_BRICK_WALL);
            default -> new TownStyle("taiga", stage, Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_BRICKS, Blocks.SMOOTH_QUARTZ, Blocks.STRIPPED_SPRUCE_LOG,
                Blocks.STRIPPED_SPRUCE_LOG, Blocks.SPRUCE_PLANKS, Blocks.WAXED_CUT_COPPER_STAIRS, Blocks.WAXED_CUT_COPPER, Blocks.GLASS_PANE,
                Blocks.SPRUCE_DOOR, Blocks.SPRUCE_FENCE, Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_TILES, Blocks.DEEPSLATE_BRICKS,
                Blocks.POLISHED_DEEPSLATE_WALL);
        };
    }

    /** Für Kupferdächer: mal neu, mal angelaufen, mal grün (gewachst – sie bleiben, wie sie sind). */
    public BlockState roofStairs(int variant) {
        if (roof == Blocks.WAXED_CUT_COPPER_STAIRS) {
            return switch (Math.floorMod(variant, 4)) {
                case 1 -> Blocks.WAXED_EXPOSED_CUT_COPPER_STAIRS.defaultBlockState();
                case 2 -> Blocks.WAXED_WEATHERED_CUT_COPPER_STAIRS.defaultBlockState();
                case 3 -> Blocks.WAXED_OXIDIZED_CUT_COPPER_STAIRS.defaultBlockState();
                default -> roof.defaultBlockState();
            };
        }
        return roof.defaultBlockState();
    }

    public BlockState roofFull(int variant) {
        if (roofBlock == Blocks.WAXED_CUT_COPPER) {
            return switch (Math.floorMod(variant, 4)) {
                case 1 -> Blocks.WAXED_EXPOSED_CUT_COPPER.defaultBlockState();
                case 2 -> Blocks.WAXED_WEATHERED_CUT_COPPER.defaultBlockState();
                case 3 -> Blocks.WAXED_OXIDIZED_CUT_COPPER.defaultBlockState();
                default -> roofBlock.defaultBlockState();
            };
        }
        return roofBlock.defaultBlockState();
    }

    /** Ab Stufe 2 sind Straßen gepflastert und breit genug für Karren. */
    public int roadWidth() {
        return stage >= 4 ? 5 : stage >= 2 ? 3 : 1;
    }

    /** Wachstumsstufen ab 4: die Stadt. */
    public boolean city() {
        return stage >= 4;
    }
}
