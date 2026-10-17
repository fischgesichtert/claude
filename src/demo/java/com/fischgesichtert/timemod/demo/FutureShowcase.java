package com.fischgesichtert.timemod.demo;

import com.fischgesichtert.timemod.demo.mixin.MouseHandlerInvoker;
import com.fischgesichtert.timemod.era.Eras;
import com.fischgesichtert.timemod.future.ChangeTracking;
import com.fischgesichtert.timemod.future.FutureDerivation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Vorführung „Die Zukunft entsteht aus der Gegenwart“: ein kleiner Hof – Haus, Brunnen mit Kupferdach,
 * Weide mit Tieren, Feld, Steinbrücke – und dieselbe Kamera in +5, +50, +500 und +1000 Jahren.
 */
final class FutureShowcase {
    private static final Logger LOGGER = LoggerFactory.getLogger("TIME-Demo");
    private static final int[] ERAS = {5, 50, 500, 1000};
    private static final int MIN_X = -14;
    private static final int MAX_X = 26;
    private static final int MIN_Z = -8;
    private static final int MAX_Z = 16;

    private static volatile BlockPos origin = BlockPos.ZERO;

    private FutureShowcase() {
    }

    static void write(DemoDirector d) {
        d.wait(20)
            .command("gamemode spectator @a")
            .command("gamerule sendCommandFeedback false")
            .command("gamerule announceAdvancements false")
            .command("gamerule doMobSpawning false")
            .command("gamerule doDaylightCycle false")
            .command("gamerule doWeatherCycle false")
            .command("weather clear")
            .command("time set 4500")
            .client(mc -> mc.options.renderDistance().set(8))
            .wait(20)
            .server(player -> {
                int[][] around = {{0, 0}, {-14, 0}, {26, 0}, {6, -8}, {6, 16}, {-14, 16}, {26, -8}};
                DemoSites.Site site = DemoSites.flatPlains(player.server.overworld(), around, pos -> true);
                origin = site.pos().offset(-4, 0, 0);
                LOGGER.info("Bühne für die Zukunft: {} (Höhenunterschied {})", origin, site.spread());
                camera(player, player.server.overworld());
            })
            .wait(120)
            .server(FutureShowcase::buildHomestead)
            .wait(30)
            .server(FutureShowcase::animals)
            .wait(60)
            .server(player -> {
                ServerLevel present = player.server.overworld();
                for (int cx = (origin.getX() + MIN_X) >> 4; cx <= (origin.getX() + MAX_X) >> 4; cx++) {
                    for (int cz = (origin.getZ() + MIN_Z) >> 4; cz <= (origin.getZ() + MAX_Z) >> 4; cz++) {
                        FutureDerivation.countAnimals(present, new ChunkPos(cx, cz));
                    }
                }
            })
            .wait(40).screenshot("f0_gegenwart");
        for (int i = 0; i < ERAS.length; i++) {
            int years = ERAS[i];
            // Der erste Besuch in einer neuen Dimension braucht länger (alles wird zum ersten Mal erzeugt)
            d.server(player -> camera(player, player.server.getLevel(Eras.byYears(years).level())))
                .wait(i == 0 ? 520 : 300).screenshot(String.format("f%d_plus%d", i + 1, years));
            if (years == 50) {
                chest(d);
            }
            if (years == 500) {
                d.server(player -> look(player, player.server.getLevel(Eras.byYears(500).level()), at(6.5, 1.0, 4.5), at(1.0, 1.2, 1.5)))
                    .wait(80).screenshot("f6_innen_plus500")
                    .server(player -> camera(player, player.server.getLevel(Eras.byYears(500).level())))
                    .wait(20);
            }
        }
        d.server(player -> look(player, player.server.getLevel(Eras.byYears(1000).level()), at(-7.0, 7.0, 18.0), at(-7.0, 3.0, 10.0)))
            .wait(80).screenshot("f7_bruecke_plus1000");
        d.wait(5).quit();
    }

    /** Fünfzig Jahre später: die alte Truhe – mit Echos von allem, was die Zeit übrig ließ. */
    private static void chest(DemoDirector d) {
        d.server(player -> {
                ServerLevel level = player.server.getLevel(Eras.byYears(50).level());
                look(player, level, at(5.5, 1.2, 1.5), at(7.5, 0.5, 1.5));
                player.setGameMode(GameType.CREATIVE);
            })
            .wait(40)
            .server(player -> {
                BlockPos chest = origin.offset(7, 0, 1);
                ServerLevel level = player.serverLevel();
                level.getBlockState(chest).use(level, player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(chest), Direction.WEST, chest, false));
            })
            .client(mc -> mc.options.hideGui = false)
            .wait(20)
            // Auf die Spitzhacke zeigen: rechts davon ist alles zerfallen, der Tooltip verdeckt nichts
            .client(mc -> hover(mc, 176, 168, 8 + 6 * 18 + 8, 18 + 8))
            .wait(10).screenshot("f5_truhe_plus50")
            .client(mc -> {
                if (mc.player != null) {
                    mc.player.closeContainer();
                }
                mc.options.hideGui = true;
            })
            .server(player -> {
                player.setGameMode(GameType.SPECTATOR);
                camera(player, player.serverLevel());
            })
            .wait(20);
    }

    private static Vec3 at(double x, double y, double z) {
        return new Vec3(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
    }

    /** Dieselbe Kamera in jeder Zeit: südlich über dem Hof, Blick nach Norden. */
    private static void camera(ServerPlayer player, ServerLevel level) {
        Vec3 eye = at(5.5, 15.0, 33.5);
        player.teleportTo(level, eye.x, eye.y, eye.z, 180.0F, 26.0F);
    }

    private static void look(ServerPlayer player, ServerLevel level, Vec3 eye, Vec3 target) {
        double dx = target.x - eye.x;
        double dz = target.z - eye.z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(Math.atan2(eye.y + player.getEyeHeight() - target.y, Math.sqrt(dx * dx + dz * dz)));
        player.teleportTo(level, eye.x, eye.y, eye.z, yaw, pitch);
    }

    private static void hover(Minecraft mc, int imageWidth, int imageHeight, int x, int y) {
        if (mc.screen == null) {
            return;
        }
        double scale = mc.getWindow().getGuiScale();
        int left = (mc.screen.width - imageWidth) / 2;
        int top = (mc.screen.height - imageHeight) / 2;
        ((MouseHandlerInvoker) mc.mouseHandler).timemod$onMove(mc.getWindow().getWindow(), (left + x) * scale, (top + y) * scale);
    }

    /** Der Hof in der Gegenwart. Gesetzt ohne Nachbar-Updates; Zäune, Scheiben und Treppen verbinden sich danach. */
    private static void buildHomestead(ServerPlayer player) {
        ServerLevel level = player.server.overworld();
        Builder b = new Builder(level, origin);
        b.fill(MIN_X, 0, MIN_Z, MAX_X, 16, MAX_Z, Blocks.AIR.defaultBlockState());
        b.fill(MIN_X, -3, MIN_Z, MAX_X, -2, MAX_Z, Blocks.DIRT.defaultBlockState());
        b.fill(MIN_X, -1, MIN_Z, MAX_X, -1, MAX_Z, Blocks.GRASS_BLOCK.defaultBlockState());
        // Haus: Fundament, Dielen, Steinziegelwände mit Eichenstämmen an den Ecken
        b.fill(0, -1, 0, 8, -1, 6, Blocks.COBBLESTONE.defaultBlockState());
        b.fill(1, -1, 1, 7, -1, 5, Blocks.OAK_PLANKS.defaultBlockState());
        for (int y = 0; y <= 3; y++) {
            for (int x = 0; x <= 8; x++) {
                b.set(x, y, 0, Blocks.STONE_BRICKS.defaultBlockState());
                b.set(x, y, 6, Blocks.STONE_BRICKS.defaultBlockState());
            }
            for (int z = 1; z <= 5; z++) {
                b.set(0, y, z, Blocks.STONE_BRICKS.defaultBlockState());
                b.set(8, y, z, Blocks.STONE_BRICKS.defaultBlockState());
            }
            for (int[] corner : new int[][] {{0, 0}, {8, 0}, {0, 6}, {8, 6}}) {
                b.set(corner[0], y, corner[1], Blocks.OAK_LOG.defaultBlockState());
            }
        }
        for (int y = 1; y <= 2; y++) {
            for (int[] window : new int[][] {{2, 6}, {6, 6}, {0, 3}, {8, 3}}) {
                b.set(window[0], y, window[1], Blocks.GLASS_PANE.defaultBlockState());
            }
            b.set(4, y, 0, Blocks.IRON_BARS.defaultBlockState());
        }
        BlockState door = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
        b.set(4, 0, 6, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        b.set(4, 1, 6, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        // Satteldach aus Fichtentreppen, Giebel aus Fichtenbrettern
        for (int k = 0; k <= 3; k++) {
            for (int x = -1; x <= 9; x++) {
                b.set(x, 4 + k, -1 + k, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH));
                b.set(x, 4 + k, 7 - k, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
            }
        }
        for (int x = -1; x <= 9; x++) {
            b.set(x, 7, 3, Blocks.SPRUCE_PLANKS.defaultBlockState());
        }
        for (int x : new int[] {0, 8}) {
            b.fill(x, 4, 1, x, 4, 5, Blocks.SPRUCE_PLANKS.defaultBlockState());
            b.fill(x, 5, 2, x, 5, 4, Blocks.SPRUCE_PLANKS.defaultBlockState());
            b.set(x, 6, 3, Blocks.SPRUCE_PLANKS.defaultBlockState());
        }
        // Drinnen: Bett, Truhe, Werkbank, Ofen, Amboss, Bücher, Teppich, Licht
        BlockState bed = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH);
        b.set(1, 0, 2, bed.setValue(BedBlock.PART, BedPart.FOOT));
        b.set(1, 0, 1, bed.setValue(BedBlock.PART, BedPart.HEAD));
        b.set(7, 0, 1, Blocks.CHEST.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
        b.set(7, 0, 3, Blocks.CRAFTING_TABLE.defaultBlockState());
        b.set(7, 0, 4, Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
        b.set(5, 0, 1, Blocks.ANVIL.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
        b.fill(1, 0, 4, 1, 1, 5, Blocks.BOOKSHELF.defaultBlockState());
        b.fill(3, 0, 2, 5, 0, 4, Blocks.RED_CARPET.defaultBlockState());
        b.set(6, 0, 5, Blocks.POTTED_POPPY.defaultBlockState());
        b.set(1, 2, 3, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.EAST));
        b.set(7, 2, 2, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.WEST));
        b.set(4, 6, 3, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        // Draußen: Fackeln am Eingang, Schild, Weg zur Tür, Laternenpfahl
        b.set(3, 2, 7, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH));
        b.set(5, 2, 7, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH));
        b.set(2, 1, 7, Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, Direction.SOUTH));
        b.fill(4, -1, 7, 4, -1, 13, Blocks.DIRT_PATH.defaultBlockState());
        b.set(-1, 0, 8, Blocks.OAK_FENCE.defaultBlockState());
        b.set(-1, 1, 8, Blocks.OAK_FENCE.defaultBlockState());
        b.set(-1, 2, 8, Blocks.LANTERN.defaultBlockState());
        // Brunnen mit Kupferdach (die Mitte gewachst – sie bleibt blank)
        for (int x = 11; x <= 13; x++) {
            for (int z = -5; z <= -3; z++) {
                if (x != 12 || z != -4) {
                    b.set(x, 0, z, Blocks.COBBLESTONE_WALL.defaultBlockState());
                }
            }
        }
        b.set(12, 0, -4, Blocks.WATER_CAULDRON.defaultBlockState().setValue(BlockStateProperties.LEVEL_CAULDRON, 3));
        for (int[] post : new int[][] {{11, -5}, {13, -5}, {11, -3}, {13, -3}}) {
            b.set(post[0], 1, post[1], Blocks.OAK_FENCE.defaultBlockState());
            b.set(post[0], 2, post[1], Blocks.OAK_FENCE.defaultBlockState());
        }
        b.fill(10, 3, -6, 14, 3, -2, Blocks.CUT_COPPER_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        b.set(12, 3, -4, Blocks.WAXED_CUT_COPPER_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        // Weide
        for (int x = 12; x <= 22; x++) {
            b.set(x, 0, 2, Blocks.OAK_FENCE.defaultBlockState());
            b.set(x, 0, 12, Blocks.OAK_FENCE.defaultBlockState());
        }
        for (int z = 3; z <= 11; z++) {
            b.set(12, 0, z, Blocks.OAK_FENCE.defaultBlockState());
            b.set(22, 0, z, Blocks.OAK_FENCE.defaultBlockState());
        }
        b.set(14, 0, 10, Blocks.HAY_BLOCK.defaultBlockState());
        b.set(20, 0, 10, Blocks.WATER_CAULDRON.defaultBlockState().setValue(BlockStateProperties.LEVEL_CAULDRON, 3));
        // Feld mit Wassergraben
        for (int x = -10; x <= -2; x++) {
            for (int z = 0; z <= 6; z++) {
                if (x == -6) {
                    b.set(x, -1, z, Blocks.WATER.defaultBlockState());
                } else {
                    b.set(x, -1, z, Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7));
                    b.set(x, 0, z, ((CropBlock) Blocks.WHEAT).getStateForAge(7));
                }
            }
        }
        // Steinbrücke über einen Bach: elf Blöcke weit – nach tausend Jahren trägt das nicht mehr
        b.fill(-11, -1, 9, -3, -1, 11, Blocks.WATER.defaultBlockState());
        for (int y = 0; y <= 3; y++) {
            b.set(-12, y, 10, Blocks.STONE_BRICKS.defaultBlockState());
            b.set(-2, y, 10, Blocks.STONE_BRICKS.defaultBlockState());
        }
        b.fill(-12, 4, 10, -2, 4, 10, Blocks.STONE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE));
        b.fill(-12, 5, 10, -2, 5, 10, Blocks.STONE_BRICK_WALL.defaultBlockState());
        b.finish();
        // Truheninhalt und Schild
        if (level.getBlockEntity(origin.offset(7, 0, 1)) instanceof ChestBlockEntity chest) {
            ItemStack[] stock = {new ItemStack(Items.BREAD, 12), new ItemStack(Items.APPLE, 5), new ItemStack(Items.IRON_INGOT, 16),
                new ItemStack(Items.DIAMOND, 4), new ItemStack(Items.GOLD_INGOT, 6), new ItemStack(Items.OAK_PLANKS, 32),
                new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.BOOK, 3), new ItemStack(Items.TORCH, 24), new ItemStack(Items.WHITE_WOOL, 8)};
            for (int i = 0; i < stock.length; i++) {
                chest.setItem(i, stock[i]);
            }
        }
        if (level.getBlockEntity(origin.offset(2, 1, 7)) instanceof SignBlockEntity sign) {
            sign.setText(sign.getFrontText().setMessage(0, Component.literal("Familie Zeit")).setMessage(1, Component.literal("Willkommen!"))
                .setMessage(2, Component.literal("Erbaut im Jahr 0")), true);
        }
        for (int cx = (origin.getX() + MIN_X) >> 4; cx <= (origin.getX() + MAX_X) >> 4; cx++) {
            for (int cz = (origin.getZ() + MIN_Z) >> 4; cz <= (origin.getZ() + MAX_Z) >> 4; cz++) {
                ChangeTracking.changed(level, new ChunkPos(cx, cz).getWorldPosition(), true);
            }
        }
    }

    private static void animals(ServerPlayer player) {
        ServerLevel level = player.server.overworld();
        EntityType<?>[] types = {EntityType.COW, EntityType.COW, EntityType.COW, EntityType.SHEEP, EntityType.SHEEP, EntityType.PIG};
        for (int i = 0; i < types.length; i++) {
            if (types[i].create(level) instanceof Mob mob) {
                mob.moveTo(origin.getX() + 15.5 + (i % 3) * 2.5, origin.getY(), origin.getZ() + 5.5 + (i / 3) * 3.0, i * 60.0F, 0.0F);
                mob.setPersistenceRequired();
                level.addFreshEntity(mob);
            }
        }
    }

    /** Setzt Blöcke ohne Nachbar-Updates (Türen, Betten, Wasser bleiben wie gebaut) und verbindet zuletzt Zäune & Co. */
    private static final class Builder {
        private final ServerLevel level;
        private final BlockPos origin;
        private final List<BlockPos> connect = new ArrayList<>();

        Builder(ServerLevel level, BlockPos origin) {
            this.level = level;
            this.origin = origin;
        }

        void set(int x, int y, int z, BlockState state) {
            BlockPos pos = origin.offset(x, y, z);
            level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            Block block = state.getBlock();
            if (block instanceof FenceBlock || block instanceof IronBarsBlock || block instanceof WallBlock || block instanceof StairBlock) {
                connect.add(pos);
            }
        }

        void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    for (int x = x0; x <= x1; x++) {
                        set(x, y, z, state);
                    }
                }
            }
        }

        void finish() {
            for (BlockPos pos : connect) {
                BlockState updated = Block.updateFromNeighbourShapes(level.getBlockState(pos), level, pos);
                level.setBlock(pos, updated, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }
}
