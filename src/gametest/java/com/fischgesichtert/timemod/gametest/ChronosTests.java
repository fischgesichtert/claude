package com.fischgesichtert.timemod.gametest;

import com.fischgesichtert.timemod.chronos.ChronosSettings;
import com.fischgesichtert.timemod.chronos.ChronosWatchItem;
import com.fischgesichtert.timemod.energy.Chronon;
import com.fischgesichtert.timemod.item.TimeLoopItem;
import com.fischgesichtert.timemod.registry.ModItems;
import com.fischgesichtert.timemod.registry.ModRecipes;
import com.fischgesichtert.timemod.rewind.ChronoJournal;
import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.TimeFilter;
import com.fischgesichtert.timemod.time.TimeZone;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Die Chronos-Uhr: Grenzen der Meisterschaft, Zeitstopp, Scrubbing mit Rückerstattung – und dass
 * sich durch Zurückspulen nichts verdoppeln lässt (weder Gegenstände noch Chronon).
 */
public class ChronosTests implements FabricGameTest {
    private static final String ARENA = "timemod-gametest:arena";

    private static ServerPlayer player(GameTestHelper helper, BlockPos at) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 pos = Vec3.atBottomCenterOf(helper.absolutePos(at));
        player.teleportTo(pos.x, pos.y, pos.z);
        return player;
    }

    private static ItemStack watch(ChronosSettings.Mode mode, int chronon) {
        ItemStack watch = new ItemStack(ModItems.CHRONOS_WATCH);
        Chronon.set(watch, chronon);
        ChronosSettings settings = ChronosSettings.of(watch);
        settings.mode = mode;
        settings.save(watch);
        return watch;
    }

    private static ChronoJournal record(GameTestHelper helper) {
        ChronoJournal journal = ServerTimeEngine.of(helper.getLevel()).journal();
        journal.clearAnchors();
        journal.addAnchor(Vec3.atCenterOf(helper.absolutePos(new BlockPos(12, 3, 12))), 40);
        journal.setRecording(true);
        return journal;
    }

    private static void stopRecording(ChronoJournal journal) {
        journal.setRecording(false);
        journal.clearAnchors();
    }

    private static int count(ServerPlayer player, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                n += stack.getCount();
            }
        }
        return n;
    }

    /** Die Grenzen wachsen mit der Meisterschaft; was der Client schickt, wird geprüft. */
    @GameTest(template = EMPTY_STRUCTURE)
    public void masteryLimitsAndActions(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModItems.CHRONOS_WATCH);
        ChronosSettings wish = ChronosSettings.of(stack);
        wish.radius = 64;
        wish.global = true;
        wish.stopSeconds = 10;
        wish.rewindSeconds = 60;
        wish.fastIndex = 99;
        wish.save(stack);
        ChronosSettings novice = ChronosSettings.of(stack);
        if (novice.radius != 16 || novice.global || novice.stopSeconds != 3 || novice.rewindSeconds != 20
            || ChronosSettings.FAST_FACTORS[novice.fastIndex] != 6.0) {
            helper.fail("Stufe 0 erlaubt Radius 16, 3 s Stopp, 20 s zurück, 6× und nicht global – ist Radius " + novice.radius
                + ", global " + novice.global + ", " + novice.stopSeconds + " s, " + novice.rewindSeconds + " s, "
                + ChronosSettings.FAST_FACTORS[novice.fastIndex] + "×");
        }
        novice.mastery = 175;
        novice.radius = 64;
        novice.global = true;
        novice.mode = ChronosSettings.Mode.FAST;
        novice.fastIndex = 99;
        novice.save(stack);
        ChronosSettings walker = ChronosSettings.of(stack);
        if (walker.level() != 3 || walker.radius != 40 || !walker.global || walker.factor() != 2.0) {
            helper.fail("Stufe 3 („Zeitwanderer“): Radius 40, global, global höchstens 2× – ist Stufe " + walker.level() + ", Radius "
                + walker.radius + ", global " + walker.global + ", " + walker.factor() + "×");
        }
        if (walker.apply(ChronosSettings.Action.FILTER, 99)) {
            helper.fail("Ein unbekannter Filter darf nichts ändern");
        }
        walker.apply(ChronosSettings.Action.MODE, ChronosSettings.Mode.REWIND.ordinal());
        walker.rewindSeconds = 9;
        walker.apply(ChronosSettings.Action.VALUE, 2);
        if (walker.mode != ChronosSettings.Mode.REWIND || walker.rewindSeconds != 15) {
            helper.fail("Mausrad beim Zurückspulen: 9 → 10 → 15 s erwartet, ist " + walker.rewindSeconds);
        }
        walker.apply(ChronosSettings.Action.FILTER, 0);
        if ((walker.effectiveMask() & TimeFilter.ANIMALS) != 0 || (walker.effectiveMask() & TimeFilter.MONSTERS) == 0) {
            helper.fail("Der Tier-Filter muss aus dem Standard heraus abgeschaltet werden");
        }
        helper.succeed();
    }

    /** Zeitstopp kostet Chronon, bringt Meisterschaft – und Schleichen + Benutzen hebt ihn wieder auf. */
    @GameTest(template = ARENA, timeoutTicks = 40, setupTicks = 5)
    public void stopsTimeLearnsAndCancels(GameTestHelper helper) {
        ServerPlayer player = player(helper, new BlockPos(4, 2, 4));
        ItemStack watch = watch(ChronosSettings.Mode.STOP, 100_000);
        ChronosSettings settings = ChronosSettings.of(watch);
        settings.radius = 8;
        settings.stopSeconds = 2;
        settings.save(watch);
        player.setItemInHand(InteractionHand.MAIN_HAND, watch);
        int expected = ChronosWatchItem.cost(ChronosSettings.of(watch));
        watch.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        ItemStack held = player.getMainHandItem();
        if (100_000 - Chronon.stored(held) != expected) {
            helper.fail("Zeitstopp kostet " + expected + ", bezahlt: " + (100_000 - Chronon.stored(held)));
        }
        if (ChronosSettings.of(held).mastery != 3) {
            helper.fail("Ein Zeitstopp bringt 3 Meisterschaftspunkte, sind " + ChronosSettings.of(held).mastery);
        }
        ServerTimeEngine engine = ServerTimeEngine.of(helper.getLevel());
        helper.runAfterDelay(2, () -> {
            boolean stopped = engine.zones().stream().anyMatch(z -> player.getUUID().equals(z.owner()) && z.factor() == 0.0 && !z.isRewind());
            if (!stopped) {
                helper.fail("Die Uhr muss eine Stopp-Zone anlegen");
            }
            player.setShiftKeyDown(true);
            held.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            player.setShiftKeyDown(false);
            helper.runAfterDelay(2, () -> {
                for (TimeZone zone : engine.zones()) {
                    if (player.getUUID().equals(zone.owner())) {
                        helper.fail("Schleichen + Benutzen muss die eigenen Zonen aufheben");
                    }
                }
                player.discard();
                helper.succeed();
            });
        });
    }

    /**
     * Scrubbing: Die Welt läuft rückwärts, solange man hält; Loslassen hält sie dort an. Der Auslöser
     * bleibt stehen, kann nichts fallen lassen und bekommt den ungenutzten Teil erstattet.
     */
    @GameTest(template = ARENA, timeoutTicks = 140, setupTicks = 20, batch = "timemod_chronos_scrub")
    public void scrubbingStopsWhereReleased(GameTestHelper helper) {
        ServerPlayer player = player(helper, new BlockPos(4, 2, 4));
        ItemStack watch = watch(ChronosSettings.Mode.REWIND, 100_000);
        ChronosSettings settings = ChronosSettings.of(watch);
        settings.radius = 16;
        settings.rewindSeconds = 5;
        settings.save(watch);
        player.setItemInHand(InteractionHand.MAIN_HAND, watch);
        ChronoJournal journal = record(helper);
        for (int t = 1; t <= 60; t++) {
            BlockPos pos = new BlockPos(2 + t % 20, 2, 8 + t / 20);
            helper.runAtTickTime(t, () -> helper.setBlock(pos, Blocks.GOLD_BLOCK));
        }
        int[] paid = new int[1];
        Vec3[] start = new Vec3[1];
        helper.runAtTickTime(70, () -> {
            start[0] = player.position();
            player.getMainHandItem().getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            paid[0] = 100_000 - Chronon.stored(player.getMainHandItem());
            if (journal.session() == null || paid[0] <= 0) {
                helper.fail("Zurückspulen muss starten und kosten (bezahlt: " + paid[0] + ")");
            }
        });
        helper.runAtTickTime(75, () -> {
            int apples = count(player, Items.APPLE);
            if (player.drop(new ItemStack(Items.APPLE), false, true) != null || count(player, Items.APPLE) != apples + 1) {
                helper.fail("Beim Scrubbing darf der Auslöser nichts fallen lassen – der Apfel muss im Inventar bleiben");
            }
        });
        helper.runAtTickTime(80, () -> {
            player.releaseUsingItem();
            if (journal.session() != null) {
                helper.fail("Loslassen muss das Zurückspulen beenden");
            }
            int gold = 0;
            for (int t = 1; t <= 60; t++) {
                if (helper.getBlockState(new BlockPos(2 + t % 20, 2, 8 + t / 20)).is(Blocks.GOLD_BLOCK)) {
                    gold++;
                }
            }
            if (gold == 60 || gold == 0) {
                helper.fail("Nach halbem Scrubbing muss ein Teil der Goldblöcke fehlen, stehen: " + gold);
            }
            if (player.position().distanceTo(start[0]) > 0.01) {
                helper.fail("Der Auslöser bleibt beim Scrubbing stehen");
            }
            ItemStack after = player.getMainHandItem();
            int left = Chronon.stored(after);
            if (left <= 100_000 - paid[0] || left >= 100_000) {
                helper.fail("Früh loslassen erstattet einen Teil: bezahlt " + paid[0] + ", übrig " + left);
            }
            if (ChronosSettings.of(after).mastery <= 0) {
                helper.fail("Scrubbing bringt Meisterschaft");
            }
            stopRecording(journal);
            player.discard();
            helper.succeed();
        });
    }

    /**
     * Abbauen und zurückspulen verdoppelt nichts: Der Block kehrt zurück, und der Auslöser verliert ihn
     * wieder aus dem Inventar – auch wenn er selbst frei bleibt. Bezahltes Chronon kommt nicht zurück.
     */
    @GameTest(template = ARENA, timeoutTicks = 80, setupTicks = 20, batch = "timemod_chronos_dupe")
    public void minedBlocksDoNotDuplicate(GameTestHelper helper) {
        ServerPlayer player = player(helper, new BlockPos(4, 2, 4));
        ItemStack watch = watch(ChronosSettings.Mode.REWIND, 100_000);
        player.setItemInHand(InteractionHand.MAIN_HAND, watch);
        BlockPos ore = new BlockPos(6, 2, 6);
        helper.setBlock(ore, Blocks.DIAMOND_BLOCK);
        ChronoJournal journal = record(helper);
        helper.runAtTickTime(10, () -> {
            helper.setBlock(ore, Blocks.AIR);
            player.getInventory().add(new ItemStack(Items.DIAMOND_BLOCK));
        });
        int[] paid = new int[1];
        helper.runAtTickTime(20, () -> {
            player.getMainHandItem().getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            paid[0] = 100_000 - Chronon.stored(player.getMainHandItem());
        });
        helper.runAtTickTime(60, () -> {
            if (journal.session() != null) {
                helper.fail("Das Zurückspulen hätte längst fertig sein müssen");
            }
            if (!helper.getBlockState(ore).is(Blocks.DIAMOND_BLOCK)) {
                helper.fail("Der abgebaute Block muss zurückkehren");
            }
            if (count(player, Items.DIAMOND_BLOCK) != 0) {
                helper.fail("Dupe! Der Block steht wieder und liegt trotzdem im Inventar");
            }
            int left = Chronon.stored(ChronosWatchItem.held(player));
            if (left != 100_000 - paid[0]) {
                helper.fail("Bezahltes Chronon darf durchs Zurückspulen nicht zurückkommen: übrig " + left + ", erwartet " + (100_000 - paid[0]));
            }
            stopRecording(journal);
            player.discard();
            helper.succeed();
        });
    }

    /** Eine geladene Zelle, die nur den Ort wechselt (Truhe → Spieler), kehrt beim Zurückspulen voll geladen zurück. */
    @GameTest(template = ARENA, timeoutTicks = 60, setupTicks = 20, batch = "timemod_chronos_cell_move")
    public void movedCellKeepsItsCharge(GameTestHelper helper) {
        ServerPlayer player = player(helper, new BlockPos(4, 2, 4));
        BlockPos chestPos = new BlockPos(8, 2, 8);
        helper.setBlock(chestPos, Blocks.CHEST);
        ItemStack cell = new ItemStack(ModItems.POCKET_TIME_CELL);
        Chronon.set(cell, 5000);
        ((ChestBlockEntity) helper.getBlockEntity(chestPos)).setItem(0, cell);
        ChronoJournal journal = record(helper);
        helper.runAtTickTime(10, () -> {
            ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(chestPos);
            player.getInventory().add(chest.removeItemNoUpdate(0));
            chest.setChanged();
        });
        helper.runAtTickTime(20, () -> {
            journal.rewind(20, 0, null, null);
            stopRecording(journal);
            ItemStack back = ((ChestBlockEntity) helper.getBlockEntity(chestPos)).getItem(0);
            if (!back.is(ModItems.POCKET_TIME_CELL) || Chronon.stored(back) != 5000) {
                helper.fail("Die Zelle muss voll geladen in der Truhe liegen, ist " + back + " mit " + Chronon.stored(back));
            }
            if (count(player, ModItems.POCKET_TIME_CELL) != 0) {
                helper.fail("Die Zelle darf nicht zusätzlich im Inventar bleiben");
            }
            player.discard();
            helper.succeed();
        });
    }

    /** Ausgegebenes Chronon bleibt ausgegeben – auch wenn die Zelle über eine Truhe zurückkehrt. */
    @GameTest(template = ARENA, timeoutTicks = 60, setupTicks = 20, batch = "timemod_chronos_cell_spent")
    public void spentChrononStaysSpent(GameTestHelper helper) {
        ServerPlayer player = player(helper, new BlockPos(4, 2, 4));
        BlockPos chestPos = new BlockPos(8, 2, 8);
        helper.setBlock(chestPos, Blocks.CHEST);
        ItemStack cell = new ItemStack(ModItems.POCKET_TIME_CELL);
        Chronon.set(cell, 5000);
        ((ChestBlockEntity) helper.getBlockEntity(chestPos)).setItem(0, cell);
        ChronoJournal journal = record(helper);
        helper.runAtTickTime(10, () -> {
            ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(chestPos);
            player.getInventory().add(chest.removeItemNoUpdate(0));
            chest.setChanged();
        });
        helper.runAtTickTime(12, () -> {
            for (ItemStack stack : player.getInventory().items) {
                if (stack.is(ModItems.POCKET_TIME_CELL)) {
                    Chronon.extract(stack, 5000);
                }
            }
        });
        helper.runAtTickTime(20, () -> {
            journal.rewind(20, 0, null, null);
            stopRecording(journal);
            ItemStack back = ((ChestBlockEntity) helper.getBlockEntity(chestPos)).getItem(0);
            if (!back.is(ModItems.POCKET_TIME_CELL)) {
                helper.fail("Die Zelle muss in die Truhe zurück");
            }
            if (Chronon.stored(back) != 0) {
                helper.fail("Dupe! Ausgegebenes Chronon ist zurückgekommen: " + Chronon.stored(back));
            }
            player.discard();
            helper.succeed();
        });
    }

    /** Teil-Zurückspulen (Tiere + Wachstum): Getreide wächst zurück, Tote bleiben tot, Abgebautes bleibt weg. */
    @GameTest(template = ARENA, timeoutTicks = 80, setupTicks = 20, batch = "timemod_chronos_partial")
    public void partialRewindOnlyRenewables(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityType.COW, new Vec3(10.5, 2, 10.5));
        cow.setNoAi(true);
        BlockPos stone = new BlockPos(12, 2, 12);
        BlockPos wheat = new BlockPos(14, 2, 14);
        helper.setBlock(stone, Blocks.STONE);
        helper.setBlock(wheat.below(), Blocks.FARMLAND);
        helper.setBlock(wheat, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
        ChronoJournal journal = record(helper);
        helper.runAtTickTime(10, () -> {
            cow.kill();
            helper.setBlock(stone, Blocks.AIR);
            helper.setBlock(wheat, Blocks.AIR);
        });
        helper.runAtTickTime(40, () -> {
            journal.rewind(30, 0, null, null, TimeFilter.ANIMALS | TimeFilter.GROWTH);
            stopRecording(journal);
            if (helper.getLevel().getEntity(cow.getUUID()) != null) {
                helper.fail("Teil-Zurückspulen weckt keine Toten auf");
            }
            if (!helper.getBlockState(stone).isAir()) {
                helper.fail("Abgebauter Stein darf beim Teil-Zurückspulen nicht zurückkehren");
            }
            if (!helper.getBlockState(wheat).is(Blocks.WHEAT)) {
                helper.fail("Geerntetes Getreide wächst beim Zurückspulen mit Wachstums-Filter zurück");
            }
            AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(40);
            helper.getLevel().getEntitiesOfClass(ItemEntity.class, area).forEach(ItemEntity::discard);
            helper.succeed();
        });
    }
    /**
     * Die Zeitschleife fängt den Tod ab: Die Welt (und der Spieler) springen zurück, die Schleife
     * kostet 100k Chronon und lädt dann 30 Minuten nach – auch über das Zurückspulen hinweg.
     */
    @GameTest(template = ARENA, timeoutTicks = 140, setupTicks = 20, batch = "timemod_loop")
    public void timeLoopRefusesDeath(GameTestHelper helper) {
        ServerPlayer player = player(helper, new BlockPos(4, 2, 4));
        // Testspieler ticken nicht – ohne das bliebe ihr Spawnschutz für immer bestehen
        player.spawnInvulnerableTime = 0;
        Vec3 start = player.position();
        ItemStack loop = new ItemStack(ModItems.TIME_LOOP);
        Chronon.set(loop, 150_000);
        player.getInventory().setItem(2, loop);
        ChronoJournal journal = record(helper);
        StringBuilder problems = new StringBuilder();
        helper.runAtTickTime(30, () -> {
            player.teleportTo(start.x + 12, start.y, start.z + 12);
            player.hurt(helper.getLevel().damageSources().generic(), 1000.0F);
            if (player.isDeadOrDying()) {
                problems.append("Die Zeitschleife muss den Tod verhindern. ");
            }
        });
        helper.runAtTickTime(90, () -> {
            ItemStack after = player.getInventory().getItem(2);
            if (journal.session() != null) {
                problems.append("Das Zurückspulen hätte fertig sein müssen. ");
            }
            if (player.position().distanceTo(start) > 1.0) {
                problems.append("Der Spieler muss an seinen alten Ort zurück, ist bei ").append(player.position()).append(". ");
            }
            if (player.getHealth() < player.getMaxHealth() - 0.01F) {
                problems.append("Das alte Leben muss zurückkehren, ist ").append(player.getHealth()).append(". ");
            }
            if (!after.is(ModItems.TIME_LOOP) || TimeLoopItem.isReady(after, helper.getLevel().getGameTime())) {
                problems.append("Die Schleife muss nach dem Einsatz nachladen (Abklingzeit übersteht das Zurückspulen). ");
            }
            if (Chronon.stored(after) != 150_000 - TimeLoopItem.COST) {
                problems.append("Die Schleife kostet ").append(TimeLoopItem.COST).append(" Chronon, übrig: ").append(Chronon.stored(after)).append(". ");
            }
            // Ein zweiter Tod während der Abklingzeit ist endgültig
            player.hurt(helper.getLevel().damageSources().generic(), 1000.0F);
            if (!player.isDeadOrDying()) {
                problems.append("Während der Abklingzeit rettet die Schleife nicht. ");
            }
            stopRecording(journal);
            player.discard();
            if (problems.length() > 0) {
                helper.fail(problems.toString());
            }
            helper.succeed();
        });
    }

    /** Die Unendlichkeits-Zelle fasst eine Million – und übernimmt das Chronon der Zellen, aus denen sie entsteht. */
    @GameTest(template = EMPTY_STRUCTURE)
    public void infinityCellKeepsIngredientChronon(GameTestHelper helper) {
        AbstractContainerMenu dummy = new AbstractContainerMenu(null, 0) {
            @Override
            public ItemStack quickMoveStack(Player player, int index) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player player) {
                return true;
            }
        };
        CraftingContainer grid = new TransientCraftingContainer(dummy, 3, 3);
        String[] pattern = {"PNP", "CXC", "PNP"};
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                ItemStack stack = switch (pattern[row].charAt(col)) {
                    case 'P' -> {
                        ItemStack cell = new ItemStack(ModItems.PARADOX_TIME_CELL);
                        Chronon.set(cell, 60_000);
                        yield cell;
                    }
                    case 'N' -> new ItemStack(Items.NETHER_STAR);
                    case 'C' -> new ItemStack(ModItems.CHRONITE_PLATE);
                    default -> new ItemStack(ModItems.HEART_OF_TIME);
                };
                grid.setItem(row * 3 + col, stack);
            }
        }
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(ModRecipes.CLOCKMAKER, grid, helper.getLevel());
        if (recipe.isEmpty()) {
            helper.fail("Kein Rezept für die Unendlichkeits-Zelle gefunden");
            return;
        }
        ItemStack result = recipe.get().assemble(grid, helper.getLevel().registryAccess());
        if (!result.is(ModItems.INFINITY_TIME_CELL) || Chronon.capacity(result) != 1_000_000) {
            helper.fail("Ergebnis muss die Unendlichkeits-Zelle mit 1M Kapazität sein, ist " + result);
        }
        if (Chronon.stored(result) != 4 * 60_000) {
            helper.fail("Das Chronon der vier Paradox-Zellen muss übergehen: erwartet 240000, ist " + Chronon.stored(result));
        }
        helper.succeed();
    }
}
