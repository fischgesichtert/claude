package com.fischgesichtert.timemod.gametest;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.era.Eras;
import com.fischgesichtert.timemod.future.ChangeTracking;
import com.fischgesichtert.timemod.future.Echo;
import com.fischgesichtert.timemod.future.FutureChunks;
import com.fischgesichtert.timemod.future.FutureDerivation;
import com.fischgesichtert.timemod.future.PresentChanges;
import com.fischgesichtert.timemod.future.WeatheringRule;
import com.fischgesichtert.timemod.future.WeatheringRules;
import com.fischgesichtert.timemod.registry.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/** Die Zukunft entsteht aus der Gegenwart: Verwitterung, Statik, Echos, Neuberechnung, Knochen. */
public class FutureTests implements FabricGameTest {
    private static ServerLevel era(GameTestHelper helper, int years) {
        return helper.getLevel().getServer().getLevel(Eras.byYears(years).level());
    }

    /** Ein Chunk weit weg von allen anderen Tests, sein Ursprung und die Bodenhöhe der flachen Testwelt. */
    private static BlockPos site(GameTestHelper helper, int offset) {
        BlockPos origin = helper.absolutePos(BlockPos.ZERO).offset(offset, 0, offset);
        ServerLevel present = helper.getLevel();
        ChunkPos chunk = new ChunkPos(origin);
        present.getChunk(chunk.x, chunk.z);
        int ground = present.getHeight(Heightmap.Types.WORLD_SURFACE, chunk.getMinBlockX(), chunk.getMinBlockZ());
        return new BlockPos(chunk.getMinBlockX(), ground, chunk.getMinBlockZ());
    }

    /** Hängt an jedem TIME-Block mit Werkzeugpflicht ein Abbau-Tag? Sonst droppt er nichts. */
    @GameTest(template = EMPTY_STRUCTURE)
    public void everyBlockCanBeMined(GameTestHelper helper) {
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(TimeMod.MOD_ID)) {
                continue;
            }
            BlockState state = block.defaultBlockState();
            if (!state.requiresCorrectToolForDrops() || state.getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) < 0) {
                continue;
            }
            if (!state.is(BlockTags.MINEABLE_WITH_PICKAXE) && !state.is(BlockTags.MINEABLE_WITH_AXE) && !state.is(BlockTags.MINEABLE_WITH_SHOVEL)
                && !state.is(BlockTags.MINEABLE_WITH_HOE)) {
                helper.fail(BuiltInRegistries.BLOCK.getKey(block) + " braucht Werkzeug, steht aber in keinem Abbau-Tag");
            }
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void rulesKnowWhatWasBuilt(GameTestHelper helper) {
        WeatheringRules.Resolved rules = WeatheringRules.get();
        for (Block made : new Block[] {Blocks.OAK_PLANKS, Blocks.STONE_BRICKS, Blocks.GLASS_PANE, Blocks.WHITE_WOOL, Blocks.TORCH, Blocks.COPPER_BLOCK,
            Blocks.IRON_BARS, Blocks.FARMLAND, Blocks.REDSTONE_WIRE, Blocks.RED_BED, Blocks.CHEST}) {
            if (rules.of(made.defaultBlockState()) == null) {
                helper.fail("Keine Verwitterungsregel für " + BuiltInRegistries.BLOCK.getKey(made));
            }
        }
        for (Block natural : new Block[] {Blocks.STONE, Blocks.DIRT, Blocks.OAK_LOG, Blocks.OAK_LEAVES, Blocks.DIAMOND_BLOCK, Blocks.WAXED_COPPER_BLOCK}) {
            if (rules.of(natural.defaultBlockState()) != null) {
                helper.fail("Natur und Kostbares altern nicht: " + BuiltInRegistries.BLOCK.getKey(natural));
            }
        }
        WeatheringRule timeless = rules.of(ModBlocks.CHRONOS_GATE_FRAME.defaultBlockState());
        if (timeless == null || timeless.remove().at(1000) > 0) {
            helper.fail("Das Chronos-Tor muss die Jahre überdauern");
        }
        helper.succeed();
    }

    /** Dasselbe Haus nach einem Jahr und nach tausend Jahren. */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void presentAgesIntoTheFuture(GameTestHelper helper) {
        ServerLevel present = helper.getLevel();
        BlockPos o = site(helper, 3000);
        int g = o.getY();
        for (int x = 0; x <= 8; x++) {
            for (int y = 0; y < 4; y++) {
                present.setBlockAndUpdate(o.offset(x, y, 0), Blocks.STONE_BRICKS.defaultBlockState());
            }
            for (int z = 1; z <= 3; z++) {
                present.setBlockAndUpdate(o.offset(x, 0, z), Blocks.OAK_PLANKS.defaultBlockState());
            }
        }
        present.setBlockAndUpdate(o.offset(4, 2, 0), Blocks.GLASS.defaultBlockState());
        present.setBlockAndUpdate(o.offset(1, 1, 2), Blocks.TORCH.defaultBlockState());
        present.setBlockAndUpdate(o.offset(2, 2, 1), Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH));
        present.setBlockAndUpdate(o.offset(3, 1, 2), Blocks.LANTERN.defaultBlockState());
        present.setBlockAndUpdate(o.offset(5, 0, 2), Blocks.FARMLAND.defaultBlockState());
        present.setBlockAndUpdate(o.offset(5, 1, 2), ((CropBlock) Blocks.WHEAT).getStateForAge(7));
        // Kupfer steht auf dem Boden – auf verrottetem Holz stürzte es nach tausend Jahren mit ein
        present.setBlockAndUpdate(o.offset(6, 0, 2), Blocks.COPPER_BLOCK.defaultBlockState());
        present.setBlockAndUpdate(o.offset(7, 0, 2), Blocks.WAXED_COPPER_BLOCK.defaultBlockState());
        present.setBlockAndUpdate(o.offset(4, 1, 3), Blocks.WHITE_CARPET.defaultBlockState());
        present.setBlockAndUpdate(o.offset(2, 1, 3), Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = (ChestBlockEntity) present.getBlockEntity(o.offset(2, 1, 3));
        chest.setItem(0, new ItemStack(Items.BREAD, 4));
        chest.setItem(1, new ItemStack(Items.DIAMOND, 2));
        present.setBlockAndUpdate(o.offset(1, 1, 3), Blocks.OAK_SIGN.defaultBlockState());
        ((SignBlockEntity) present.getBlockEntity(o.offset(1, 1, 3))).setText(
            ((SignBlockEntity) present.getBlockEntity(o.offset(1, 1, 3))).getFrontText().setMessage(0, Component.literal("Hallo Zukunft")), true);
        // Eine Brücke über 15 Blöcke: Nach tausend Jahren trägt Stein nur noch vier weit
        for (int y = 0; y < 4; y++) {
            present.setBlockAndUpdate(o.offset(0, y, 8), Blocks.STONE_BRICKS.defaultBlockState());
            present.setBlockAndUpdate(o.offset(14, y, 8), Blocks.STONE_BRICKS.defaultBlockState());
        }
        for (int x = 0; x <= 14; x++) {
            present.setBlockAndUpdate(o.offset(x, 4, 8), Blocks.STONE_BRICKS.defaultBlockState());
        }
        ChangeTracking.changed(present, o, true);
        ServerLevel soon = era(helper, 1);
        ServerLevel far = era(helper, 1000);
        // Fest geladen, wie rund um einen Spieler: Ein Chunk, der vor der Ableitung wieder entlädt, kommt erst beim nächsten Laden dran
        soon.setChunkForced(o.getX() >> 4, o.getZ() >> 4, true);
        far.setChunkForced(o.getX() >> 4, o.getZ() >> 4, true);
        helper.startSequence().thenWaitUntil(() -> settled(helper, o, soon, far)).thenExecute(() -> {
            // Nach einem Jahr: alles steht, nur das Licht ist aus und das Feld liegt brach
            expect(helper, soon, o.offset(1, 1, 2), ModBlocks.BURNT_TORCH, "Fackel nach einem Jahr");
            expect(helper, soon, o.offset(2, 2, 1), ModBlocks.BURNT_WALL_TORCH, "Wandfackel nach einem Jahr");
            if (soon.getBlockState(o.offset(2, 2, 1)).getValue(WallTorchBlock.FACING) != Direction.SOUTH) {
                helper.fail("Die erloschene Wandfackel muss noch in dieselbe Richtung zeigen");
            }
            expect(helper, soon, o.offset(3, 1, 2), ModBlocks.DULL_LANTERN, "Laterne nach einem Jahr");
            expect(helper, soon, o.offset(5, 0, 2), Blocks.DIRT, "Ackerboden nach einem Jahr");
            expect(helper, soon, o.offset(6, 0, 2), Blocks.COPPER_BLOCK, "Kupfer nach einem Jahr");
            expect(helper, soon, o.offset(4, 2, 0), Blocks.GLASS, "Glas nach einem Jahr");
            expect(helper, soon, o.offset(7, 4, 8), Blocks.STONE_BRICKS, "Brückenmitte nach einem Jahr");
            if (soon.getBlockState(o.offset(5, 1, 2)).is(Blocks.WHEAT)) {
                helper.fail("Nach einem Jahr steht kein reifer Weizen mehr – das Feld verwildert");
            }
            if (!(soon.getBlockEntity(o.offset(2, 1, 3)) instanceof ChestBlockEntity futureChest)) {
                helper.fail("Die Truhe muss nach einem Jahr noch dastehen");
                return;
            }
            ItemStack bread = futureChest.getItem(0);
            if (!bread.is(Items.ROTTEN_FLESH) || !Echo.is(bread) || !futureChest.getItem(1).is(Items.DIAMOND) || !Echo.is(futureChest.getItem(1))) {
                helper.fail("In der Zukunft liegt der gealterte Inhalt als Zeit-Echo: " + bread + ", " + futureChest.getItem(1));
            }
            if (!chest.getItem(1).is(Items.DIAMOND) || Echo.is(chest.getItem(1))) {
                helper.fail("Die Truhe der Gegenwart bleibt, wie sie ist");
            }
            // Nach tausend Jahren: kein Holz mehr, Kupfer grün, Wiese statt Feld, die Brücke eingestürzt
            int planks = 0;
            for (BlockPos pos : BlockPos.betweenClosed(o.offset(0, -1, 0), o.offset(15, 8, 15))) {
                if (far.getBlockState(pos).is(BlockTags.PLANKS)) {
                    planks++;
                }
            }
            if (planks > 0) {
                helper.fail("Nach tausend Jahren ist kein Brett mehr übrig, gefunden: " + planks);
            }
            expect(helper, far, o.offset(6, 0, 2), Blocks.OXIDIZED_COPPER, "Kupfer nach tausend Jahren");
            expect(helper, far, o.offset(7, 0, 2), Blocks.WAXED_COPPER_BLOCK, "Gewachstes Kupfer nach tausend Jahren");
            expect(helper, far, o.offset(5, 0, 2), Blocks.GRASS_BLOCK, "Ehemaliges Feld nach tausend Jahren");
            if (far.getBlockState(o.offset(1, 1, 2)).is(ModBlocks.BURNT_TORCH) || far.getBlockState(o.offset(4, 1, 3)).is(Blocks.WHITE_CARPET)) {
                helper.fail("Nach tausend Jahren sind Fackel und Teppich vergangen");
            }
            if (far.getBlockState(o.offset(7, 4, 8)).is(Blocks.STONE_BRICKS) || far.getBlockState(o.offset(7, 4, 8)).is(Blocks.MOSSY_STONE_BRICKS)
                || far.getBlockState(o.offset(7, 4, 8)).is(Blocks.CRACKED_STONE_BRICKS)) {
                helper.fail("Die Mitte einer 15 Blöcke weiten Brücke muss nach tausend Jahren eingestürzt sein");
            }
            if (FutureChunks.get(far).isClaimed(ChunkPos.asLong(o))) {
                helper.fail("Der Chunk darf nicht als beansprucht gelten");
            }
            soon.setChunkForced(o.getX() >> 4, o.getZ() >> 4, false);
            far.setChunkForced(o.getX() >> 4, o.getZ() >> 4, false);
        }).thenSucceed();
    }

    /** Baut man in der Gegenwart weiter, rechnet die Zukunft neu – außer man hat in der Zukunft selbst gebaut. */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void futureFollowsThePresentUntilClaimed(GameTestHelper helper) {
        ServerLevel present = helper.getLevel();
        BlockPos o = site(helper, 3200);
        present.setBlockAndUpdate(o.offset(3, 0, 3), Blocks.STONE_BRICKS.defaultBlockState());
        ChangeTracking.changed(present, o, true);
        ServerLevel future = era(helper, 5);
        future.setChunkForced(o.getX() >> 4, o.getZ() >> 4, true);
        helper.startSequence().thenWaitUntil(() -> settled(helper, o, future)).thenExecute(() -> {
            expect(helper, future, o.offset(3, 0, 3), Blocks.STONE_BRICKS, "Steinziegel in der Zukunft");
            present.setBlockAndUpdate(o.offset(5, 0, 5), Blocks.GOLD_BLOCK.defaultBlockState());
            ChangeTracking.changed(present, o.offset(5, 0, 5), true);
            long revision = PresentChanges.get(present.getServer()).revision(ChunkPos.asLong(o));
            if (!FutureChunks.get(future).needs(ChunkPos.asLong(o), revision)) {
                helper.fail("Nach einer Änderung in der Gegenwart muss die Zukunft neu berechnet werden");
            }
            LevelChunk chunk = future.getChunk(o.getX() >> 4, o.getZ() >> 4);
            FutureDerivation.derive(future, chunk);
            expect(helper, future, o.offset(5, 0, 5), Blocks.GOLD_BLOCK, "Neues aus der Gegenwart");
            ChangeTracking.changed(future, o.offset(1, 0, 1), true);
            present.setBlockAndUpdate(o.offset(6, 0, 6), Blocks.EMERALD_BLOCK.defaultBlockState());
        }).thenExecuteAfter(2, () -> {
            ChangeTracking.changed(present, o.offset(6, 0, 6), true);
            long later = PresentChanges.get(present.getServer()).revision(ChunkPos.asLong(o));
            if (FutureChunks.get(future).needs(ChunkPos.asLong(o), later)) {
                helper.fail("Wer in der Zukunft gebaut hat, dessen Chunk wird nie mehr überschrieben");
            }
            future.setChunkForced(o.getX() >> 4, o.getZ() >> 4, false);
        }).thenSucceed();
    }

    /** Zweimal dasselbe Jahr: dieselbe Ruine, Block für Block. */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void sameYearSameRuin(GameTestHelper helper) {
        ServerLevel present = helper.getLevel();
        BlockPos o = site(helper, 3400);
        for (BlockPos pos : BlockPos.betweenClosed(o.offset(1, 0, 1), o.offset(12, 5, 12))) {
            int r = (pos.getX() * 31 + pos.getY() * 17 + pos.getZ() * 7) & 7;
            Block block = r < 3 ? Blocks.STONE_BRICKS : r < 5 ? Blocks.OAK_PLANKS : r < 6 ? Blocks.GLASS : Blocks.AIR;
            present.setBlockAndUpdate(pos.immutable(), block.defaultBlockState());
        }
        ChangeTracking.changed(present, o, true);
        ServerLevel future = era(helper, 500);
        future.setChunkForced(o.getX() >> 4, o.getZ() >> 4, true);
        helper.startSequence().thenWaitUntil(() -> settled(helper, o, future)).thenExecute(() -> {
            long first = hash(future, o);
            FutureDerivation.derive(future, future.getChunk(o.getX() >> 4, o.getZ() >> 4));
            long second = hash(future, o);
            future.setChunkForced(o.getX() >> 4, o.getZ() >> 4, false);
            if (first != second) {
                helper.fail("Dasselbe Jahr muss dieselbe Ruine ergeben");
            }
        }).thenSucceed();
    }

    /** Zeit-Echos lassen sich nicht greifen: weder im Menü noch beim Aufheben noch durch Trichter. */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
    public void echoesCrumbleAtATouch(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        ItemStack echo = new ItemStack(Items.DIAMOND, 3);
        Echo.mark(echo, level);
        player.getInventory().add(echo.copy());
        if (player.getInventory().countItem(Items.DIAMOND) > 0) {
            helper.fail("Ein Echo darf nicht ins Inventar gelangen");
        }
        SimpleContainer box = new SimpleContainer(27);
        box.setItem(0, echo.copy());
        ChestMenu menu = ChestMenu.threeRows(1, player.getInventory(), box);
        menu.clicked(0, 0, ClickType.PICKUP, player);
        if (!box.getItem(0).isEmpty() || !menu.getCarried().isEmpty()) {
            helper.fail("Ein Echo im Menü zerfällt beim Greifen");
        }
        menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
        BlockPos chestPos = helper.absolutePos(new BlockPos(1, 2, 1));
        level.setBlockAndUpdate(chestPos.below(), Blocks.HOPPER.defaultBlockState());
        level.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
        ((ChestBlockEntity) level.getBlockEntity(chestPos)).setItem(0, echo.copy());
        ItemEntity dropped = new ItemEntity(level, chestPos.getX() + 3.5, chestPos.getY() + 1, chestPos.getZ() + 0.5, echo.copy());
        level.addFreshEntity(dropped);
        helper.runAtTickTime(30, () -> {
            if (!dropped.isRemoved()) {
                helper.fail("Ein Echo auf dem Boden vergeht sofort");
            }
            if (!((HopperBlockEntity) level.getBlockEntity(chestPos.below())).isEmpty()) {
                helper.fail("Ein Trichter darf kein Echo aus der Truhe ziehen");
            }
            player.discard();
            helper.succeed();
        });
    }

    /** Gehaltene Tiere: in der Zukunft Knochen, ihre Nachkommen leben verwildert in der Nähe. */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void keptAnimalsLeaveBones(GameTestHelper helper) {
        ServerLevel present = helper.getLevel();
        BlockPos o = site(helper, 3600);
        ChunkPos chunk = new ChunkPos(o);
        // Tiere sind nur in voll geladenen Chunks abfragbar – wie rund um einen Spieler
        present.setChunkForced(chunk.x, chunk.z, true);
        for (int i = 0; i <= 12; i++) {
            for (BlockPos post : new BlockPos[] {o.offset(1 + i, 0, 1), o.offset(1 + i, 0, 13), o.offset(1, 0, 1 + i), o.offset(13, 0, 1 + i)}) {
                present.setBlockAndUpdate(post, Blocks.OAK_FENCE.defaultBlockState());
            }
        }
        for (int i = 0; i < 8; i++) {
            Cow cow = EntityType.COW.create(present);
            cow.moveTo(o.getX() + 3.5 + (i % 4) * 2, o.getY(), o.getZ() + 4.5 + (i / 4) * 4, 0, 0);
            cow.setNoAi(true);
            present.addFreshEntity(cow);
        }
        ChangeTracking.changed(present, o, true);
        ServerLevel future = era(helper, 100);
        helper.startSequence().thenExecuteAfter(5, () -> {
            FutureDerivation.countAnimals(present, chunk);
            int counted = PresentChanges.get(present.getServer()).animals(chunk.toLong()).size();
            if (counted != 8) {
                helper.fail("Alle acht eingezäunten Kühe müssen gezählt sein: " + counted);
            }
            // Erst jetzt die Zukunft laden: Sie entsteht aus der Gegenwart samt Zählung
            forceAround(future, chunk, true);
        }).thenWaitUntil(() -> settled(helper, o, future)).thenWaitUntil(() -> {
            int bones = 0;
            for (BlockPos pos : BlockPos.betweenClosed(o.offset(0, -3, 0), o.offset(15, 4, 15))) {
                if (future.getBlockState(pos).is(ModBlocks.BONE_PILE)) {
                    bones++;
                }
            }
            int cows = future.getEntitiesOfClass(Cow.class, new AABB(o).inflate(40)).size();
            if (bones < 2 || cows < 1) {
                helper.fail("Erwartet: Knochen der alten Kühe und verwilderte Nachkommen, gefunden " + bones + " Knochen, " + cows + " Kühe");
            }
        }).thenExecute(() -> {
            present.setChunkForced(chunk.x, chunk.z, false);
            forceAround(future, chunk, false);
        }).thenSucceed();
    }

    private static void forceAround(ServerLevel level, ChunkPos chunk, boolean forced) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setChunkForced(chunk.x + dx, chunk.z + dz, forced);
            }
        }
    }

    /** Wartet, bis die Zukunft an dieser Stelle fertig abgeleitet ist (die Ableitung hat ein Zeitbudget je Tick). */
    private static void settled(GameTestHelper helper, BlockPos pos, ServerLevel... futures) {
        for (ServerLevel future : futures) {
            if (!FutureDerivation.isSettled(future, new ChunkPos(pos)) || !FutureChunks.get(future).isDerived(ChunkPos.asLong(pos))) {
                helper.fail("Die Zukunft wird noch abgeleitet");
            }
        }
    }

    private static void expect(GameTestHelper helper, ServerLevel level, BlockPos pos, Block block, String what) {
        if (!level.getBlockState(pos).is(block)) {
            helper.fail(what + ": erwartet " + BuiltInRegistries.BLOCK.getKey(block) + ", gefunden " + level.getBlockState(pos));
        }
    }

    private static long hash(ServerLevel level, BlockPos origin) {
        long hash = 17;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(0, -2, 0), origin.offset(15, 12, 15))) {
            hash = hash * 31 + Block.getId(level.getBlockState(pos));
        }
        return hash;
    }
}
