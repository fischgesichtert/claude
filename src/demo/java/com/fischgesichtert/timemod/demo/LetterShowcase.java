package com.fischgesichtert.timemod.demo;

import com.fischgesichtert.timemod.block.entity.TimeCapsuleBlockEntity;
import com.fischgesichtert.timemod.era.Eras;
import com.fischgesichtert.timemod.registry.ModBlocks;
import com.fischgesichtert.timemod.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Vorführung „Grüße an die Gegenwart“: Im Frühmittelalter eine Zeitkapsel befüllen und versiegeln und
 * Zeitsaat auf einen Setzling streuen – heute steht an derselben Stelle ein Riese, an seinen Wurzeln die
 * gealterte Kapsel. Dazu ein versteinerter Stumpf aus der Eiszeit.
 */
final class LetterShowcase {
    private static final Logger LOGGER = LoggerFactory.getLogger("TIME-Demo");
    private static final int PAST = -1000;
    private static final int ICE_AGE = -20_000;

    // Vom Server gesetzt, vom Client für die Kamerafahrt gelesen
    private static volatile BlockPos capsule = BlockPos.ZERO;
    private static volatile BlockPos sapling = BlockPos.ZERO;
    private static volatile BlockPos stump = BlockPos.ZERO;
    private static volatile int ground;

    private LetterShowcase() {
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
            .command("time set 5000")
            .client(mc -> mc.options.renderDistance().set(10))
            .wait(20)
            .server(LetterShowcase::chooseSite)
            .wait(10)
            .server(LetterShowcase::setUpPast)
            .wait(160)
            // Die Kapsel im Frühmittelalter: befüllt, gleich wird sie versiegelt
            .server(player -> {
                if (player.serverLevel().getBlockEntity(capsule) instanceof TimeCapsuleBlockEntity box) {
                    player.openMenu(box);
                }
            })
            .client(mc -> mc.options.hideGui = false)
            .wait(30).screenshot("l1_kapsel_befuellt")
            .client(mc -> {
                if (mc.player != null) {
                    mc.player.closeContainer();
                }
            })
            .wait(10)
            .server(player -> {
                ServerLevel past = player.serverLevel();
                player.setShiftKeyDown(true);
                past.getBlockState(capsule).use(past, player, InteractionHand.MAIN_HAND, hit(capsule));
                player.setShiftKeyDown(false);
            })
            .wait(14).screenshot("l2_kapsel_versiegelt")
            .wait(50)
            .server(player -> {
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TIME_SEED, 3));
                ModItems.TIME_SEED.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(sapling)));
                player.swing(InteractionHand.MAIN_HAND, true);
            })
            .wait(12).screenshot("l3_zeitsaat")
            .wait(40)
            // Dieselbe Stelle, tausend Jahre später
            .client(mc -> mc.options.hideGui = true)
            .server(player -> {
                player.setGameMode(GameType.SPECTATOR);
                Vec3 eye = orbitPoint(90);
                player.teleportTo(player.server.overworld(), eye.x, eye.y, eye.z, 180.0F, -10.0F);
            })
            .wait(200).screenshot("l4_gegenwart_riese");
        orbit(d);
        d.server(LetterShowcase::closeUp)
            .wait(60).screenshot("l5_kapsel_an_den_wurzeln")
            .server(player -> {
                BlockPos found = findCapsule(player.server.overworld());
                if (found != null) {
                    player.setGameMode(GameType.CREATIVE);
                    player.server.overworld().getBlockState(found).use(player.server.overworld(), player, InteractionHand.MAIN_HAND, hit(found));
                }
            })
            .client(mc -> mc.options.hideGui = false)
            .wait(30).screenshot("l6_kapsel_gefunden")
            .client(mc -> {
                if (mc.player != null) {
                    mc.player.closeContainer();
                }
                mc.options.hideGui = true;
            })
            .wait(10)
            .server(LetterShowcase::plantInTheIceAge)
            .wait(10)
            .server(player -> {
                player.setGameMode(GameType.SPECTATOR);
                look(player, Vec3.atBottomCenterOf(stump).add(5.5, 3.0, 6.5), Vec3.atBottomCenterOf(stump).add(0, 1.5, 0));
            })
            .wait(60).screenshot("l7_versteinert");
        d.wait(5).quit();
    }

    /** Langsam um den Riesen herum, von Süden nach Westen. */
    private static void orbit(DemoDirector d) {
        d.record("seq_riese", 2);
        for (int i = 0; i <= 100; i++) {
            double angle = 90 + 120 * i / 100.0;
            d.wait(1).client(mc -> {
                if (mc.player != null) {
                    Vec3 eye = orbitPoint(angle);
                    Vec3 centre = Vec3.atBottomCenterOf(sapling.atY(ground));
                    float yaw = (float) Math.toDegrees(Math.atan2(-(centre.x - eye.x), centre.z - eye.z));
                    mc.player.moveTo(eye.x, eye.y, eye.z, yaw, -10.0F);
                }
            });
        }
        d.stopRecording();
    }

    private static Vec3 orbitPoint(double degrees) {
        double angle = Math.toRadians(degrees);
        return new Vec3(sapling.getX() + 0.5 + Math.cos(angle) * 32, ground + 8, sapling.getZ() + 0.5 + Math.sin(angle) * 32);
    }

    /** Eine Ebene in der Gegenwart, die im Frühmittelalter Wald war – mit freiem Waldboden, wo Kapsel und Setzling hinkommen. */
    private static void chooseSite(ServerPlayer player) {
        ServerLevel past = player.server.getLevel(Eras.byYears(PAST).level());
        int[][] around = {{0, 0}, {-6, 0}, {14, 0}, {4, 10}, {4, -10}, {4, 32}, {4, -30}, {9, -24}};
        DemoSites.Site site = DemoSites.flatPlains(player.server.overworld(), around, pos -> clearForestFloor(past, pos));
        LOGGER.info("Bühne für die Zeitkapsel: {} (Höhenunterschied ringsum {})", site.pos(), site.spread());
        ground = site.pos().getY();
        capsule = site.pos();
        sapling = site.pos().east(4);
        stump = site.pos().offset(4, 0, -30);
    }

    /** Im Frühmittelalter: eine Lichtung – zwischen Standort, Kapsel und Setzling kein Baumstamm, überall Waldboden. */
    private static boolean clearForestFloor(ServerLevel past, BlockPos site) {
        for (BlockPos column : new BlockPos[] {site, site.east(4), site.offset(2, 0, 5)}) {
            BlockPos top = surface(past, column);
            if (!past.getBlockState(top.below()).is(BlockTags.DIRT)) {
                return false;
            }
        }
        BlockPos floor = surface(past, site);
        for (BlockPos pos : BlockPos.betweenClosed(floor.offset(-1, -1, -1), floor.offset(5, 3, 6))) {
            if (past.getBlockState(pos).is(BlockTags.LOGS) || past.getBlockState(pos).is(Blocks.MUSHROOM_STEM)) {
                return false;
            }
        }
        return true;
    }

    private static void setUpPast(ServerPlayer player) {
        ServerLevel past = player.server.getLevel(Eras.byYears(PAST).level());
        BlockPos box = surface(past, capsule);
        BlockPos seedling = surface(past, sapling);
        capsule = box;
        sapling = seedling;
        past.setBlockAndUpdate(box, ModBlocks.TIME_CAPSULE.defaultBlockState());
        if (past.getBlockEntity(box) instanceof TimeCapsuleBlockEntity contents) {
            ItemStack letter = new ItemStack(Items.WRITTEN_BOOK);
            letter.getOrCreateTag().putString("title", "An die Zukunft");
            letter.getOrCreateTag().putString("author", player.getGameProfile().getName());
            contents.setItem(0, letter);
            contents.setItem(1, new ItemStack(Items.BREAD, 6));
            contents.setItem(2, new ItemStack(Items.IRON_INGOT, 12));
            contents.setItem(3, new ItemStack(Items.IRON_SWORD));
            contents.setItem(4, new ItemStack(Items.COPPER_BLOCK, 4));
            contents.setItem(5, new ItemStack(Items.DIAMOND, 3));
            contents.setItem(6, new ItemStack(Items.GOLD_INGOT, 5));
            contents.setItem(7, new ItemStack(Items.HONEY_BOTTLE));
            contents.setItem(8, new ItemStack(Items.OAK_PLANKS, 16));
        }
        if (!past.getBlockState(seedling.below()).is(BlockTags.DIRT)) {
            past.setBlockAndUpdate(seedling.below(), Blocks.GRASS_BLOCK.defaultBlockState());
        }
        past.setBlockAndUpdate(seedling, Blocks.OAK_SAPLING.defaultBlockState());
        player.setGameMode(GameType.CREATIVE);
        // Fünf Schritte südlich, mittig zwischen Kapsel und Setzling
        BlockPos stand = surface(past, box.offset(2, 0, 5));
        player.teleportTo(past, stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 180.0F, 22.0F);
    }

    /** Nah an der gefundenen Kapsel, der Stamm des Riesen dahinter. */
    private static void closeUp(ServerPlayer player) {
        BlockPos found = findCapsule(player.server.overworld());
        if (found == null) {
            return;
        }
        Vec3 target = Vec3.atBottomCenterOf(found).add(0, 0.4, 0);
        look(player, Vec3.atBottomCenterOf(found).add(-2.5, 2.2, 3.5), target);
    }

    private static BlockPos findCapsule(ServerLevel present) {
        for (BlockPos pos : BlockPos.betweenClosed(capsule.offset(-4, -12, -4), capsule.offset(4, 6, 4))) {
            if (present.getBlockEntity(pos) instanceof TimeCapsuleBlockEntity) {
                return pos.immutable();
            }
        }
        return null;
    }

    /** Zeitsaat vor 20.000 Jahren auf einen Fichtensetzling – heute bleibt ein versteinerter Stumpf mit Bernstein. */
    private static void plantInTheIceAge(ServerPlayer player) {
        ServerLevel ice = player.server.getLevel(Eras.byYears(ICE_AGE).level());
        ice.getChunk(stump.getX() >> 4, stump.getZ() >> 4);
        BlockPos seedling = surface(ice, stump);
        ice.setBlockAndUpdate(seedling.below(), Blocks.DIRT.defaultBlockState());
        ice.setBlockAndUpdate(seedling, Blocks.SPRUCE_SAPLING.defaultBlockState());
        ItemStack seed = new ItemStack(ModItems.TIME_SEED);
        ModItems.TIME_SEED.useOn(new UseOnContext(ice, player, InteractionHand.MAIN_HAND, seed, hit(seedling)) {
        });
        ServerLevel present = player.server.overworld();
        stump = surface(present, stump).below();
    }

    private static void look(ServerPlayer player, Vec3 eye, Vec3 target) {
        double dx = target.x - eye.x;
        double dz = target.z - eye.z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(Math.atan2(eye.y + player.getEyeHeight() - target.y, Math.sqrt(dx * dx + dz * dz)));
        player.teleportTo(player.server.overworld(), eye.x, eye.y, eye.z, yaw, pitch);
    }

    private static BlockPos surface(ServerLevel level, BlockPos column) {
        level.getChunk(column.getX() >> 4, column.getZ() >> 4);
        return new BlockPos(column.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ()), column.getZ());
    }

    private static BlockHitResult hit(BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }
}
