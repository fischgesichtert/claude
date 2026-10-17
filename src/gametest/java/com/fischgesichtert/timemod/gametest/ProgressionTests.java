package com.fischgesichtert.timemod.gametest;

import com.fischgesichtert.timemod.block.ChroniteOreBlock;
import com.fischgesichtert.timemod.block.entity.TimeCondenserBlockEntity;
import com.fischgesichtert.timemod.block.entity.TimeFieldProjectorBlockEntity;
import com.fischgesichtert.timemod.energy.Chronon;
import com.fischgesichtert.timemod.entity.ThrownChronoKnife;
import com.fischgesichtert.timemod.item.GrowthHourglassItem;
import com.fischgesichtert.timemod.item.WindUpItem;
import com.fischgesichtert.timemod.menu.ClockmakerMenu;
import com.fischgesichtert.timemod.registry.ModBlocks;
import com.fischgesichtert.timemod.registry.ModEntities;
import com.fischgesichtert.timemod.registry.ModItems;
import com.fischgesichtert.timemod.registry.ModRecipes;
import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.TimeFilter;
import com.fischgesichtert.timemod.time.ZoneShape;
import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Stufe I und II: Werkzeuge, Maschinen, Chronit-Erz und Rezepte. */
public class ProgressionTests implements FabricGameTest {
    private static final String ARENA = "timemod-gametest:arena";

    private static int cookTime(FurnaceBlockEntity furnace) {
        return furnace.saveWithoutMetadata().getInt("CookTime");
    }

    private static FurnaceBlockEntity litFurnace(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.FURNACE);
        FurnaceBlockEntity furnace = (FurnaceBlockEntity) helper.getBlockEntity(pos);
        furnace.setItem(0, new ItemStack(Items.RAW_IRON, 4));
        furnace.setItem(1, new ItemStack(Items.COAL, 4));
        return furnace;
    }

    /** Die Wachstums-Sanduhr lässt einen einzelnen Block 4× so schnell laufen und kostet Federspannung. */
    @GameTest(template = ARENA, timeoutTicks = 80, setupTicks = 5)
    public void growthHourglassSpeedsUpOneBlock(GameTestHelper helper) {
        BlockPos fastPos = new BlockPos(8, 2, 8);
        BlockPos normalPos = new BlockPos(12, 2, 8);
        FurnaceBlockEntity fast = litFurnace(helper, fastPos);
        FurnaceBlockEntity normal = litFurnace(helper, normalPos);

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack hourglass = new ItemStack(ModItems.GROWTH_HOURGLASS);
        WindUpItem.setTension(hourglass, WindUpItem.MAX_TENSION);
        player.setItemInHand(InteractionHand.MAIN_HAND, hourglass);
        BlockPos abs = helper.absolutePos(fastPos);
        UseOnContext context = new UseOnContext(player, InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
        hourglass.getItem().useOn(context);
        int expectedTension = player.getAbilities().instabuild ? WindUpItem.MAX_TENSION : WindUpItem.MAX_TENSION - GrowthHourglassItem.COST;
        if (WindUpItem.tension(hourglass) != expectedTension) {
            helper.fail("Die Sanduhr muss " + GrowthHourglassItem.COST + " Spannung kosten (jetzt " + WindUpItem.tension(hourglass) + ")");
        }

        helper.runAtTickTime(21, () -> {
            int a = cookTime(fast);
            int b = cookTime(normal);
            if (b < 10 || a != 4 * b) {
                helper.fail("Der Ofen unter der Sanduhr muss genau 4× so weit sein: " + a + " gegenüber " + b);
            }
        });
        helper.runAtTickTime(60, () -> {
            if (fast.getItem(2).getCount() < 1) {
                helper.fail("Nach 60 Ticks (= 240 Ofen-Ticks) muss der schnelle Ofen ein Barren fertig haben");
            }
            if (!normal.getItem(2).isEmpty()) {
                helper.fail("Der normale Ofen darf noch nichts fertig haben");
            }
            player.discard();
            helper.succeed();
        });
    }

    /** Der Zeitfeld-Projektor hält eine Zone, bezahlt sie mit Chronon und schaltet bei Redstone ab. */
    @GameTest(template = ARENA, timeoutTicks = 80, setupTicks = 5)
    public void projectorZoneRunsOnChrononAndRedstone(GameTestHelper helper) {
        BlockPos projectorPos = new BlockPos(10, 2, 8);
        helper.setBlock(projectorPos, ModBlocks.TIME_FIELD_PROJECTOR);
        TimeFieldProjectorBlockEntity projector = (TimeFieldProjectorBlockEntity) helper.getBlockEntity(projectorPos);
        projector.setBuffer(10_000);
        FurnaceBlockEntity inside = litFurnace(helper, new BlockPos(12, 2, 8));
        FurnaceBlockEntity outside = litFurnace(helper, new BlockPos(20, 2, 8));
        ServerLevel level = helper.getLevel();
        ServerTimeEngine engine = ServerTimeEngine.of(level);
        Vec3 insideCenter = Vec3.atCenterOf(helper.absolutePos(new BlockPos(12, 2, 8)));

        helper.runAtTickTime(10, () -> {
            double factor = engine.factorAt(insideCenter.x, insideCenter.y, insideCenter.z, TimeFilter.MACHINES);
            if (factor != projector.factor()) {
                helper.fail("Im Projektorfeld muss Faktor " + projector.factor() + " gelten, gefunden " + factor);
            }
            if (projector.buffer() >= 10_000) {
                helper.fail("Der Projektor muss Chronon verbrauchen");
            }
        });
        helper.runAtTickTime(30, () -> {
            if (cookTime(inside) <= cookTime(outside)) {
                helper.fail("Ein Ofen im Projektorfeld muss schneller sein: " + cookTime(inside) + " gegenüber " + cookTime(outside));
            }
            helper.setBlock(projectorPos.east(), Blocks.REDSTONE_BLOCK);
        });
        helper.runAtTickTime(34, () -> {
            double factor = engine.factorAt(insideCenter.x, insideCenter.y, insideCenter.z, TimeFilter.MACHINES);
            if (factor != 1.0) {
                helper.fail("Mit Redstone-Signal muss das Feld aus sein, Faktor ist " + factor);
            }
            int buffer = projector.buffer();
            helper.runAfterDelay(10, () -> {
                if (projector.buffer() != buffer) {
                    helper.fail("Ein abgeschalteter Projektor darf nichts verbrauchen");
                }
                helper.succeed();
            });
        });
    }

    /** Chronit-Erz flackert über geplante Ticks – in einer Stopp-Zone (Moment-Linse) bleibt es stehen. */
    @GameTest(template = ARENA, timeoutTicks = 170, setupTicks = 5)
    public void chroniteOreFreezesInStoppedTime(GameTestHelper helper) {
        BlockPos frozenPos = new BlockPos(8, 2, 8);
        BlockPos freePos = new BlockPos(14, 2, 8);
        helper.setBlock(frozenPos, ModBlocks.DEEPSLATE_CHRONITE_ORE);
        helper.setBlock(freePos, ModBlocks.DEEPSLATE_CHRONITE_ORE);

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack lens = new ItemStack(ModItems.MOMENT_LENS);
        WindUpItem.setTension(lens, WindUpItem.MAX_TENSION);
        player.setItemInHand(InteractionHand.MAIN_HAND, lens);
        BlockPos abs = helper.absolutePos(frozenPos);
        lens.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)));

        int[] flips = new int[2];
        boolean[] last = {false, false};
        helper.onEachTick(() -> {
            boolean a = helper.getBlockState(frozenPos).getValue(ChroniteOreBlock.PHASED);
            boolean b = helper.getBlockState(freePos).getValue(ChroniteOreBlock.PHASED);
            if (a != last[0]) {
                flips[0]++;
                last[0] = a;
            }
            if (b != last[1]) {
                flips[1]++;
                last[1] = b;
            }
        });
        // Erster Wechsel kommt nach 20–79 Ticks (onPlace); die Linse hält 60 Ticks
        helper.runAtTickTime(58, () -> {
            if (flips[0] != 0) {
                helper.fail("Das Erz unter der Moment-Linse darf nicht flackern (" + flips[0] + " Wechsel)");
            }
        });
        helper.runAtTickTime(82, () -> {
            if (flips[1] == 0) {
                helper.fail("Freies Chronit-Erz muss spätestens nach 80 Ticks flackern");
            }
        });
        helper.runAtTickTime(145, () -> {
            if (flips[0] == 0) {
                helper.fail("Nach Ablauf der Moment-Linse muss das Erz wieder flackern");
            }
            player.discard();
            helper.succeed();
        });
    }

    /** Ein geworfenes Chrono-Messer bleibt in gestoppter Zeit in der Luft hängen und fliegt danach weiter. */
    @GameTest(template = ARENA, timeoutTicks = 60, setupTicks = 20)
    public void knifeHangsInStoppedTime(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 start = Vec3.atCenterOf(helper.absolutePos(new BlockPos(6, 6, 8)));
        ThrownChronoKnife knife = new ThrownChronoKnife(ModEntities.CHRONO_KNIFE, level);
        knife.setPos(start.x, start.y, start.z);
        knife.setDeltaMovement(0.8, 0.0, 0.0);
        level.addFreshEntity(knife);
        ServerTimeEngine.of(level).addZone(new ZoneShape.Sphere(start, 5), 0.0, TimeFilter.DEFAULT, null, 20);

        helper.runAtTickTime(15, () -> {
            if (knife.position().distanceTo(start) > 1.0E-6) {
                helper.fail("Das Messer muss im Zeitstopp stillstehen, ist aber " + knife.position().distanceTo(start) + " weit geflogen");
            }
            if (knife.getDeltaMovement().x != 0.8) {
                helper.fail("Der Schwung muss erhalten bleiben");
            }
        });
        helper.runAtTickTime(28, () -> {
            if (knife.position().x - start.x < 2.0) {
                helper.fail("Nach dem Stopp muss das Messer weiterfliegen");
            }
            knife.discard();
            helper.succeed();
        });
    }

    /** Der Zeitkondensator verbrennt Kristalle zu Chronon und lädt damit eine eingelegte Zeitzelle. */
    @GameTest(template = ARENA, timeoutTicks = 60, setupTicks = 5)
    public void condenserChargesTimeCell(GameTestHelper helper) {
        BlockPos pos = new BlockPos(10, 2, 8);
        helper.setBlock(pos, ModBlocks.TIME_CONDENSER);
        TimeCondenserBlockEntity condenser = (TimeCondenserBlockEntity) helper.getBlockEntity(pos);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CHRONITE_CRYSTAL, 3));
        condenser.interact(player, InteractionHand.MAIN_HAND);
        if (condenser.fuel().getCount() != 3 || !player.getMainHandItem().isEmpty()) {
            helper.fail("Kristalle müssen in den Kondensator wandern");
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.POCKET_TIME_CELL));
        condenser.interact(player, InteractionHand.MAIN_HAND);
        if (condenser.charging().isEmpty()) {
            helper.fail("Die Zeitzelle muss eingelegt sein");
        }
        helper.runAtTickTime(30, () -> {
            int charged = Chronon.stored(condenser.charging());
            int total = charged + condenser.buffer();
            if (charged <= 0) {
                helper.fail("Die Zeitzelle muss geladen werden");
            }
            if (total % 50 != 0 || total > 30 * 50 || total < 20 * 50) {
                helper.fail("Der Kondensator muss 50 Chronon pro Tick erzeugen: " + total + " nach ~30 Ticks");
            }
            if (condenser.fuel().getCount() != 2) {
                helper.fail("Ein Kristall reicht für 2000 Chronon – es darf erst einer verbraucht sein");
            }
            player.discard();
            helper.succeed();
        });
    }

    /** Präzisionsrezepte gehen nur an der Uhrmacher-Werkbank, nicht an der normalen Werkbank. */
    @GameTest(template = EMPTY_STRUCTURE)
    public void clockmakerRecipesOnlyAtTheBench(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ClockmakerMenu menu = new ClockmakerMenu(0, player.getInventory());
        TransientCraftingContainer grid = new TransientCraftingContainer(menu, 3, 3);
        grid.setItem(1, new ItemStack(ModItems.BRASS_NUGGET));
        grid.setItem(3, new ItemStack(Items.GLASS_PANE));
        grid.setItem(4, new ItemStack(ModItems.CLOCKWORK));
        grid.setItem(5, new ItemStack(Items.GLASS_PANE));
        grid.setItem(7, new ItemStack(ModItems.BRASS_NUGGET));

        var bench = level.getRecipeManager().getRecipeFor(ModRecipes.CLOCKMAKER, grid, level);
        if (bench.isEmpty() || !bench.get().assemble(grid, level.registryAccess()).is(ModItems.POCKET_WATCH)) {
            helper.fail("Die Uhrmacher-Werkbank muss aus diesem Muster eine Taschenuhr machen");
        }
        if (level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).isPresent()) {
            helper.fail("Die normale Werkbank darf keine Taschenuhr herstellen");
        }
        player.discard();
        helper.succeed();
    }

    /** Alle Rezept-Dateien müssen geladen sein (ein Tippfehler im JSON würde sonst nur still geloggt). */
    @GameTest(template = EMPTY_STRUCTURE)
    public void allRecipesAndLootTablesLoad(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        Set<String> files = server.getResourceManager()
            .listResources("recipes", id -> id.getNamespace().equals("timemod") && id.getPath().endsWith(".json")).keySet().stream()
            .map(id -> id.getPath().substring("recipes/".length(), id.getPath().length() - ".json".length()))
            .collect(Collectors.toSet());
        Set<String> loaded = server.getRecipeManager().getRecipeIds()
            .filter(id -> id.getNamespace().equals("timemod")).map(ResourceLocation::getPath).collect(Collectors.toSet());
        files.removeAll(loaded);
        if (!files.isEmpty()) {
            helper.fail("Nicht geladene Rezepte: " + files);
        }
        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (!id.getNamespace().equals("timemod")) {
                continue;
            }
            if (block.getLootTable().equals(BuiltInLootTables.EMPTY)) {
                continue;
            }
            LootTable table = server.getLootData().getLootTable(block.getLootTable());
            if (table == LootTable.EMPTY) {
                helper.fail("Block ohne Beute-Tabelle: " + id);
            }
        }
        helper.succeed();
    }

    /** Der Projektor selbst läuft immer in normaler Zeit, auch wenn er in einer fremden Zone steht. */
    @GameTest(template = ARENA, timeoutTicks = 40, setupTicks = 5)
    public void projectorIgnoresForeignStopZones(GameTestHelper helper) {
        BlockPos projectorPos = new BlockPos(10, 2, 8);
        helper.setBlock(projectorPos, ModBlocks.TIME_FIELD_PROJECTOR);
        TimeFieldProjectorBlockEntity projector = (TimeFieldProjectorBlockEntity) helper.getBlockEntity(projectorPos);
        projector.setBuffer(10_000);
        BlockPos abs = helper.absolutePos(projectorPos);
        ServerTimeEngine.of(helper.getLevel()).addZone(new ZoneShape.Box(new AABB(abs).inflate(1)), 0.0, TimeFilter.ALL_BLOCKS, null, 100);
        helper.runAtTickTime(20, () -> {
            BlockState state = helper.getBlockState(projectorPos);
            if (projector.buffer() >= 10_000 || !state.getValue(com.fischgesichtert.timemod.block.TimeFieldProjectorBlock.ACTIVE)) {
                helper.fail("Der Projektor muss auch in gestoppter Zeit arbeiten");
            }
            helper.succeed();
        });
    }

    /** Die Rückblick-Uhr fährt den Weg der letzten Sekunden rückwärts ab und stellt das Leben wieder her. */
    @GameTest(template = ARENA, timeoutTicks = 100, setupTicks = 5)
    public void recallWatchJumpsBackInTime(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 p0 = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(6, 2, 8)));
        Vec3 p1 = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(12, 2, 8)));
        Vec3 p2 = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(18, 2, 10)));
        player.teleportTo(p0.x, p0.y, p0.z);
        float fullHealth = player.getHealth();
        helper.runAtTickTime(20, () -> player.teleportTo(p1.x, p1.y, p1.z));
        helper.runAtTickTime(40, () -> {
            player.teleportTo(p2.x, p2.y, p2.z);
            player.setHealth(fullHealth - 7.0F);
        });
        helper.runAtTickTime(60, () -> {
            ItemStack watch = new ItemStack(ModItems.RECALL_WATCH);
            WindUpItem.setTension(watch, WindUpItem.MAX_TENSION);
            player.setItemInHand(InteractionHand.MAIN_HAND, watch);
            watch.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            if (!player.getCooldowns().isOnCooldown(ModItems.RECALL_WATCH)) {
                helper.fail("Die Rückblick-Uhr muss danach abkühlen");
            }
        });
        helper.runAtTickTime(66, () -> {
            if (player.position().distanceTo(p2) < 0.01 || player.position().distanceTo(p0) < 0.01) {
                helper.fail("Der Rücksprung muss den Weg sichtbar zurücklaufen, nicht teleportieren");
            }
        });
        helper.runAtTickTime(75, () -> {
            if (player.position().distanceTo(p0) > 0.01) {
                helper.fail("Nach dem Rücksprung muss der Spieler wieder am Startpunkt stehen: " + player.position() + " statt " + p0);
            }
            if (player.getHealth() != fullHealth) {
                helper.fail("Das Leben muss wie vor 5 Sekunden sein: " + player.getHealth() + " statt " + fullHealth);
            }
            player.discard();
            helper.succeed();
        });
    }

    /** Alle Advancements laden (ein Fehler im JSON würde sonst nur geloggt). */
    @GameTest(template = EMPTY_STRUCTURE)
    public void allAdvancementsLoad(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        Set<String> files = server.getResourceManager()
            .listResources("advancements", id -> id.getNamespace().equals("timemod") && id.getPath().endsWith(".json")).keySet().stream()
            .map(id -> id.getPath().substring("advancements/".length(), id.getPath().length() - ".json".length()))
            .collect(Collectors.toSet());
        Set<String> loaded = server.getAdvancements().getAllAdvancements().stream().map(a -> a.getId())
            .filter(id -> id.getNamespace().equals("timemod")).map(ResourceLocation::getPath).collect(Collectors.toSet());
        if (files.isEmpty()) {
            helper.fail("Keine Advancement-Dateien gefunden");
        }
        files.removeAll(loaded);
        if (!files.isEmpty()) {
            helper.fail("Nicht geladene Advancements: " + files);
        }
        helper.succeed();
    }

    /** Neue Spieler bekommen die Chronik – aber nur einmal. */
    @GameTest(template = EMPTY_STRUCTURE)
    public void newPlayersGetTheChronikOnce(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        int count = player.getInventory().countItem(ModItems.CHRONIK);
        if (count != 1) {
            helper.fail("Beim ersten Betreten muss genau eine Chronik im Inventar liegen, gefunden: " + count);
        }
        com.fischgesichtert.timemod.item.ChronikGift.give(player);
        if (player.getInventory().countItem(ModItems.CHRONIK) != 1) {
            helper.fail("Die Chronik darf nur einmal geschenkt werden");
        }
        player.discard();
        helper.succeed();
    }
}
