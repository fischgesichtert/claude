package com.fischgesichtert.timemod.gametest;

import com.fischgesichtert.timemod.block.TimeCapsuleBlock;
import com.fischgesichtert.timemod.block.entity.TimeCapsuleBlockEntity;
import com.fischgesichtert.timemod.era.Aging;
import com.fischgesichtert.timemod.era.Eras;
import com.fischgesichtert.timemod.era.Timeline;
import com.fischgesichtert.timemod.registry.ModBlocks;
import com.fischgesichtert.timemod.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Grüße an die Gegenwart: wie Dinge altern, Zeitkapseln und Zeitsaat. */
public class LetterTests implements FabricGameTest {
    private static void expect(GameTestHelper helper, ItemStack aged, net.minecraft.world.item.Item item, int count, String what) {
        if (count == 0 ? !aged.isEmpty() : !aged.is(item) || aged.getCount() != count) {
            helper.fail(what + ": " + aged);
        }
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void agingFollowsTheYears(GameTestHelper helper) {
        expect(helper, Aging.age(new ItemStack(Items.BREAD, 5), 1), Items.ROTTEN_FLESH, 5, "Brot nach einem Jahr");
        expect(helper, Aging.age(new ItemStack(Items.BREAD, 5), 1000), Items.AIR, 0, "Brot nach tausend Jahren");
        expect(helper, Aging.age(new ItemStack(Items.HONEY_BOTTLE), 500), Items.HONEY_BOTTLE, 1, "Honig verdirbt nie");
        expect(helper, Aging.age(new ItemStack(Items.IRON_INGOT, 8), 1), Items.IRON_INGOT, 8, "Eisen nach einem Jahr");
        expect(helper, Aging.age(new ItemStack(Items.IRON_INGOT, 8), 50), ModItems.RUSTY_IRON, 8, "Eisen nach fünfzig Jahren");
        expect(helper, Aging.age(new ItemStack(Items.IRON_INGOT, 8), 500), ModItems.RUSTY_IRON, 4, "Eisen nach fünfhundert Jahren");
        expect(helper, Aging.age(new ItemStack(Items.DIAMOND, 3), 245_000_000), Items.DIAMOND, 3, "Diamanten bleiben Diamanten");
        expect(helper, Aging.age(new ItemStack(Items.GOLD_INGOT, 2), 66_000_000), Items.GOLD_INGOT, 2, "Gold bleibt Gold");
        expect(helper, Aging.age(new ItemStack(Items.COPPER_BLOCK, 4), 20), Items.OXIDIZED_COPPER, 4, "Kupfer nach zwanzig Jahren");
        expect(helper, Aging.age(new ItemStack(Items.PAPER, 16), 100), Items.AIR, 0, "Papier nach hundert Jahren");
        expect(helper, Aging.age(new ItemStack(Items.OAK_PLANKS, 10), 20), Items.OAK_PLANKS, 5, "Holz nach zwanzig Jahren");
        expect(helper, Aging.age(new ItemStack(Items.STONE, 10), 1_000_000), Items.STONE, 10, "Stein überdauert");
        ItemStack sword = Aging.age(new ItemStack(Items.IRON_SWORD), 10);
        if (!sword.is(Items.IRON_SWORD) || sword.getDamageValue() == 0) {
            helper.fail("Ein Eisenschwert nach zehn Jahren muss abgenutzt sein: " + sword);
        }
        expect(helper, Aging.age(new ItemStack(Items.IRON_SWORD), 2000), Items.AIR, 0, "Eisenschwert nach zweitausend Jahren");
        ItemStack netherite = Aging.age(new ItemStack(Items.NETHERITE_SWORD), 30);
        if (!netherite.is(Items.NETHERITE_SWORD)) {
            helper.fail("Netherit hält dreißig Jahre: " + netherite);
        }
        helper.succeed();
    }

    /** Im Jahr −100 befüllt und versiegelt; in der Gegenwart an derselben Stelle, gealtert. */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void capsuleCrossesTheYears(GameTestHelper helper) {
        ServerLevel present = helper.getLevel();
        ServerLevel past = present.getServer().getLevel(Eras.byYears(-100).level());
        BlockPos pos = helper.absolutePos(BlockPos.ZERO).offset(260, 6, 260);
        past.getChunk(pos);
        present.getChunk(pos);
        past.setBlockAndUpdate(pos, ModBlocks.TIME_CAPSULE.defaultBlockState());
        TimeCapsuleBlockEntity sealed = (TimeCapsuleBlockEntity) past.getBlockEntity(pos);
        sealed.setItem(0, new ItemStack(Items.BREAD, 3));
        sealed.setItem(1, new ItemStack(Items.IRON_INGOT, 6));
        sealed.setItem(2, new ItemStack(Items.DIAMOND, 2));
        sealed.setItem(3, new ItemStack(Items.PAPER, 9));
        sealed.setItem(4, new ItemStack(Items.IRON_SWORD));
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        past.getBlockState(pos).use(past, player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        if (past.getBlockState(pos).getValue(TimeCapsuleBlock.STAGE) != TimeCapsuleBlock.Stage.SEALED || !sealed.isEmpty()) {
            helper.fail("Die Kapsel in der Vergangenheit muss versiegelt und leer sein – ihr Inhalt reist");
        }
        helper.runAtTickTime(5, () -> {
            TimeCapsuleBlockEntity found = null;
            for (BlockPos at : BlockPos.betweenClosed(pos.offset(-4, -12, -4), pos.offset(4, 4, 4))) {
                if (present.getBlockEntity(at) instanceof TimeCapsuleBlockEntity capsule) {
                    found = capsule;
                }
            }
            if (found == null || found.getBlockState().getValue(TimeCapsuleBlock.STAGE) != TimeCapsuleBlock.Stage.ANCIENT) {
                helper.fail("In der Gegenwart liegt keine gefundene Kapsel an derselben Stelle");
            }
            if (found.years() != 100 || !found.getItem(0).is(Items.ROTTEN_FLESH) || !found.getItem(1).is(ModItems.RUSTY_IRON)
                || found.getItem(1).getCount() != 3 || !found.getItem(2).is(Items.DIAMOND) || found.getItem(2).getCount() != 2
                || !found.getItem(3).isEmpty() || !found.getItem(4).is(Items.IRON_SWORD) || found.getItem(4).getDamageValue() == 0) {
                helper.fail("Der Inhalt ist nicht richtig gealtert: " + found.items());
            }
            player.discard();
            helper.succeed();
        });
    }

    /** Liegt die Stelle in der Gegenwart gerade nicht geladen, wartet die Kapsel – und erscheint, sobald jemand hinkommt. */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void capsuleWaitsForTheChunk(GameTestHelper helper) {
        ServerLevel present = helper.getLevel();
        ServerLevel past = present.getServer().getLevel(Eras.byYears(-10).level());
        BlockPos pos = helper.absolutePos(BlockPos.ZERO).offset(2200, 6, 2200);
        if (present.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null) {
            helper.fail("Die Stelle in der Gegenwart darf für diesen Test noch nicht geladen sein");
        }
        past.getChunk(pos);
        past.setBlockAndUpdate(pos, ModBlocks.TIME_CAPSULE.defaultBlockState());
        ((TimeCapsuleBlockEntity) past.getBlockEntity(pos)).setItem(0, new ItemStack(Items.GOLD_INGOT, 4));
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setShiftKeyDown(true);
        past.getBlockState(pos).use(past, player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        player.discard();
        Timeline timeline = Timeline.get(present.getServer());
        if (!timeline.has(Timeline.Kind.CAPSULE, pos)) {
            helper.fail("Die Kapsel muss auf der Zeitlinie warten, solange die Stelle nicht geladen ist");
        }
        helper.runAtTickTime(3, () -> present.getChunk(pos));
        helper.runAtTickTime(6, () -> {
            boolean found = false;
            for (BlockPos at : BlockPos.betweenClosed(pos.offset(-4, -12, -4), pos.offset(4, 4, 4))) {
                if (present.getBlockEntity(at) instanceof TimeCapsuleBlockEntity capsule && capsule.getItem(0).is(Items.GOLD_INGOT)) {
                    found = true;
                }
            }
            if (!found || timeline.has(Timeline.Kind.CAPSULE, pos)) {
                helper.fail("Nach dem Laden der Stelle muss die Kapsel dort liegen und die Zeitlinie leer sein");
            }
            helper.succeed();
        });
    }

    /** Zeitsaat im Jahr −1000: In der Gegenwart steht ein Riese – und die Truhe daneben bleibt, wie sie war. */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void timeSeedGrowsAGiant(GameTestHelper helper) {
        ServerLevel present = helper.getLevel();
        ServerLevel past = present.getServer().getLevel(Eras.byYears(-1000).level());
        BlockPos column = helper.absolutePos(BlockPos.ZERO).offset(330, 0, 330);
        present.getChunk(column);
        int groundY = present.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
        BlockPos chestPos = new BlockPos(column.getX() + 1, groundY, column.getZ());
        present.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
        ((ChestBlockEntity) present.getBlockEntity(chestPos)).setItem(0, new ItemStack(Items.EMERALD, 7));
        BlockPos sapling = new BlockPos(column.getX(), 120, column.getZ());
        past.getChunk(sapling);
        past.setBlockAndUpdate(sapling.below(), Blocks.DIRT.defaultBlockState());
        past.setBlockAndUpdate(sapling, Blocks.OAK_SAPLING.defaultBlockState());
        plant(helper, past, sapling);
        helper.runAtTickTime(5, () -> {
            int logs = count(present, column, Blocks.OAK_LOG, 24, 60);
            int leaves = count(present, column, Blocks.OAK_LEAVES, 24, 60);
            if (logs < 80 || leaves < 300) {
                helper.fail("Ein tausendjähriger Baum braucht mehr Holz und Laub: " + logs + " Stämme, " + leaves + " Blätter");
            }
            if (!(present.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) || !chest.getItem(0).is(Items.EMERALD)) {
                helper.fail("Der Baum darf die Truhe in der Gegenwart nicht zerstören");
            }
            helper.succeed();
        });
    }

    /** Zeitsaat in der Eiszeit: Nach 20.000 Jahren bleibt nur ein versteinerter Stamm – mit Bernstein. */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void deepTimeSeedLeavesAmber(GameTestHelper helper) {
        ServerLevel present = helper.getLevel();
        ServerLevel past = present.getServer().getLevel(Eras.byYears(-20_000).level());
        BlockPos column = helper.absolutePos(BlockPos.ZERO).offset(390, 0, 390);
        present.getChunk(column);
        BlockPos sapling = new BlockPos(column.getX(), 140, column.getZ());
        past.getChunk(sapling);
        past.setBlockAndUpdate(sapling.below(), Blocks.DIRT.defaultBlockState());
        past.setBlockAndUpdate(sapling, Blocks.SPRUCE_SAPLING.defaultBlockState());
        plant(helper, past, sapling);
        helper.runAtTickTime(5, () -> {
            int amber = count(present, column, ModBlocks.AMBER_BLOCK, 14, 20);
            int stone = count(present, column, Blocks.TUFF, 14, 20) + count(present, column, Blocks.CALCITE, 14, 20);
            if (amber < 1 || stone < 10 || count(present, column, Blocks.SPRUCE_LOG, 14, 20) > 0) {
                helper.fail("Erwartet: versteinerter Stamm mit Bernstein, gefunden " + stone + " Stein, " + amber + " Bernstein");
            }
            helper.succeed();
        });
    }

    private static void plant(GameTestHelper helper, ServerLevel past, BlockPos sapling) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        ItemStack seed = new ItemStack(ModItems.TIME_SEED, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, seed);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(sapling), Direction.UP, sapling, false);
        // Der öffentliche UseOnContext-Konstruktor nimmt den Spieler-Level; die Saat wird aber in der Vergangenheit gestreut
        ModItems.TIME_SEED.useOn(new UseOnContext(past, player, InteractionHand.MAIN_HAND, seed, hit) {
        });
        if (seed.getCount() != 1) {
            helper.fail("Die Zeitsaat muss verbraucht werden");
        }
        player.discard();
    }

    private static int count(ServerLevel level, BlockPos column, Block block, int radius, int height) {
        int ground = level.getMinBuildHeight();
        int n = 0;
        for (BlockPos pos : BlockPos.betweenClosed(column.getX() - radius, ground, column.getZ() - radius, column.getX() + radius,
            ground + height + 40, column.getZ() + radius)) {
            if (level.getBlockState(pos).is(block)) {
                n++;
            }
        }
        return n;
    }
}
