package com.fischgesichtert.timemod.gametest;

import com.fischgesichtert.timemod.block.TemporalWorkbenchBlock;
import com.fischgesichtert.timemod.block.entity.TemporalWorkbenchBlockEntity;
import com.fischgesichtert.timemod.energy.Chronon;
import com.fischgesichtert.timemod.item.ModeChrononToolItem;
import com.fischgesichtert.timemod.item.StasisWatchItem;
import com.fischgesichtert.timemod.menu.TemporalWorkbenchMenu;
import com.fischgesichtert.timemod.module.Modules;
import com.fischgesichtert.timemod.registry.ModBlocks;
import com.fischgesichtert.timemod.registry.ModItems;
import com.fischgesichtert.timemod.rewind.ChronoJournal;
import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.TimeFilter;
import com.fischgesichtert.timemod.time.TimeZone;
import com.fischgesichtert.timemod.time.ZoneShape;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Stufe III: Module verändern Werkzeuge, die Temporal-Werkbank baut sie ein – ohne Verdopplung. */
public class ModuleTests implements FabricGameTest {
    private static final String ARENA = "timemod-gametest:arena";

    private static ItemStack withModules(ItemStack tool, ItemStack... modules) {
        Modules.write(tool, List.of(modules));
        return tool;
    }

    /** Radius- und Filtermodul in der Stillstand-Uhr: größere Kugel, nur Monster, teurer. */
    @GameTest(template = EMPTY_STRUCTURE)
    public void modulesShapeTheStasisZone(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        ItemStack watch = withModules(new ItemStack(ModItems.STASIS_WATCH), new ItemStack(ModItems.RADIUS_MODULE), new ItemStack(ModItems.FILTER_MONSTERS));
        Chronon.set(watch, 4000);
        player.setItemInHand(InteractionHand.MAIN_HAND, watch);
        ServerTimeEngine engine = ServerTimeEngine.of(helper.getLevel());
        int before = engine.zones().size();
        watch.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        if (engine.zones().size() != before + 1) {
            helper.fail("Die Stillstand-Uhr muss eine Zone erzeugen");
        }
        TimeZone zone = engine.zones().get(engine.zones().size() - 1);
        double radius = ((ZoneShape.Sphere) zone.shape()).radius();
        if (Math.abs(radius - StasisWatchItem.RADIUS * 1.5) > 1e-9) {
            helper.fail("Ein Radiusmodul muss den Radius auf 150 % bringen, ist " + radius);
        }
        if (zone.mask() != TimeFilter.MONSTERS) {
            helper.fail("Mit Monster-Filter darf die Zone nur Monster anhalten (Maske " + zone.mask() + ")");
        }
        int expectedCost = (int) Math.ceil(StasisWatchItem.COST * 1.5 * 1.5);
        if (Chronon.stored(watch) != 4000 - expectedCost) {
            helper.fail("Kosten mit Radiusmodul: erwartet " + expectedCost + ", übrig " + Chronon.stored(watch));
        }
        engine.removeZone(zone.id());
        player.discard();
        helper.succeed();
    }

    /** Stärkemodule schalten im Zeitraffer 6× und 8× frei; ohne sie fällt der Modus sauber zurück. */
    @GameTest(template = EMPTY_STRUCTURE)
    public void strengthUnlocksFactors(GameTestHelper helper) {
        ItemStack accelerator = withModules(new ItemStack(ModItems.TIME_ACCELERATOR),
            new ItemStack(ModItems.STRENGTH_MODULE), new ItemStack(ModItems.STRENGTH_MODULE));
        ModeChrononToolItem item = (ModeChrononToolItem) accelerator.getItem();
        accelerator.getOrCreateTag().putInt("Mode", 4);
        if (item.factor(accelerator) != 8.0) {
            helper.fail("Mit zwei Stärkemodulen muss Modus 5 = 8× sein, ist " + item.factor(accelerator));
        }
        Modules.write(accelerator, List.of());
        double fallback = item.factor(accelerator);
        if (fallback < 2.0 || fallback > 4.0) {
            helper.fail("Ohne Module muss der Faktor wieder in 2–4× liegen, ist " + fallback);
        }
        ItemStack pendulum = withModules(new ItemStack(ModItems.SLOW_PENDULUM), new ItemStack(ModItems.STRENGTH_MODULE));
        pendulum.getOrCreateTag().putInt("Mode", 2);
        if (((ModeChrononToolItem) pendulum.getItem()).factor(pendulum) != 0.1) {
            helper.fail("Das Pendel mit Stärkemodul muss 0,1× können");
        }
        helper.succeed();
    }

    /** Die Werkbank erwacht erst mit allen Säulen und schläft wieder ein, wenn eine fehlt. */
    @GameTest(template = ARENA, timeoutTicks = 80, setupTicks = 5)
    public void workbenchNeedsAllPillars(GameTestHelper helper) {
        BlockPos center = new BlockPos(10, 2, 8);
        helper.setBlock(center, ModBlocks.TEMPORAL_WORKBENCH);
        for (BlockPos offset : TemporalWorkbenchBlock.PILLARS) {
            if (!offset.equals(TemporalWorkbenchBlock.PILLARS[7])) {
                helper.setBlock(center.offset(offset), ModBlocks.CHRONO_PILLAR);
            }
        }
        helper.runAtTickTime(15, () -> {
            if (helper.getBlockState(center).getValue(TemporalWorkbenchBlock.FORMED)) {
                helper.fail("Mit 7 von 8 Säulenblöcken darf die Werkbank nicht erwachen");
            }
            helper.setBlock(center.offset(TemporalWorkbenchBlock.PILLARS[7]), ModBlocks.CHRONO_PILLAR);
        });
        helper.runAtTickTime(30, () -> {
            if (!helper.getBlockState(center).getValue(TemporalWorkbenchBlock.FORMED)) {
                helper.fail("Mit allen Säulen muss die Werkbank erwachen");
            }
            helper.setBlock(center.offset(TemporalWorkbenchBlock.PILLARS[0]), Blocks.AIR);
        });
        helper.runAtTickTime(45, () -> {
            if (helper.getBlockState(center).getValue(TemporalWorkbenchBlock.FORMED)) {
                helper.fail("Ohne Säule muss die Werkbank wieder einschlafen");
            }
            helper.succeed();
        });
    }

    /** Ein- und Ausbauen über echte Klicks: Das Modul existiert immer genau einmal. */
    @GameTest(template = ARENA, timeoutTicks = 60, setupTicks = 5)
    public void workbenchMovesModulesWithoutDuplicates(GameTestHelper helper) {
        BlockPos center = new BlockPos(10, 2, 8);
        helper.setBlock(center, ModBlocks.TEMPORAL_WORKBENCH);
        for (BlockPos offset : TemporalWorkbenchBlock.PILLARS) {
            helper.setBlock(center.offset(offset), ModBlocks.CHRONO_PILLAR);
        }
        helper.runAtTickTime(15, () -> {
            TemporalWorkbenchBlockEntity workbench = (TemporalWorkbenchBlockEntity) helper.getBlockEntity(center);
            ServerPlayer player = helper.makeMockServerPlayerInLevel();
            Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(center.north(2)));
            player.teleportTo(at.x, at.y, at.z);
            TemporalWorkbenchMenu menu = new TemporalWorkbenchMenu(1, player.getInventory(), workbench.toolContainer(),
                ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(center)), workbench);
            player.containerMenu = menu;

            // Werkzeug per Umschalt-Klick aus dem Inventar einlegen
            player.getInventory().setItem(0, new ItemStack(ModItems.STASIS_WATCH));
            player.getInventory().setItem(1, new ItemStack(ModItems.RADIUS_MODULE));
            player.getInventory().setItem(2, new ItemStack(ModItems.STRENGTH_MODULE));
            player.getInventory().setItem(3, new ItemStack(ModItems.FILTER_MONSTERS, 2));
            int hotbar = 4 + 27;
            menu.clicked(hotbar, 0, ClickType.QUICK_MOVE, player);
            if (!menu.tool().is(ModItems.STASIS_WATCH)) {
                helper.fail("Das Werkzeug muss im Werkzeugplatz liegen");
            }
            if (menu.activeModuleSlots() != 3) {
                helper.fail("Die Stillstand-Uhr hat 3 Modulplätze");
            }
            // Radiusmodul per Umschalt-Klick einbauen
            menu.clicked(hotbar + 1, 0, ClickType.QUICK_MOVE, player);
            if (Modules.effects(menu.tool()).radius() != 1 || !player.getInventory().getItem(1).isEmpty()) {
                helper.fail("Das Radiusmodul muss eingebaut und aus dem Inventar verschwunden sein");
            }
            // Stärke versteht die Uhr nicht
            menu.clicked(hotbar + 2, 0, ClickType.QUICK_MOVE, player);
            if (Modules.effects(menu.tool()).strength() != 0 || player.getInventory().getItem(2).isEmpty()) {
                helper.fail("Ein Stärkemodul darf in die Stillstand-Uhr nicht hinein");
            }
            // Zwei gleiche Filter: nur einer passt
            menu.clicked(hotbar + 3, 0, ClickType.QUICK_MOVE, player);
            if (player.getInventory().getItem(3).getCount() != 1 || Modules.effects(menu.tool()).filter() != TimeFilter.MONSTERS) {
                helper.fail("Derselbe Filter darf nur einmal eingebaut werden");
            }
            // Radiusmodul mit der Maus herausnehmen und wieder ablegen
            menu.clicked(1, 0, ClickType.PICKUP, player);
            if (!menu.getCarried().is(ModItems.RADIUS_MODULE) || Modules.effects(menu.tool()).radius() != 0) {
                helper.fail("Herausnehmen muss das Modul aus dem Werkzeug entfernen");
            }
            menu.clicked(1, 0, ClickType.PICKUP, player);
            if (!menu.getCarried().isEmpty() || Modules.effects(menu.tool()).radius() != 1) {
                helper.fail("Ablegen muss das Modul wieder einbauen");
            }
            // Werkzeug herausnehmen: Module bleiben im Werkzeug, die Plätze werden leer
            menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
            ItemStack back = player.getInventory().getItem(0).is(ModItems.STASIS_WATCH) ? player.getInventory().getItem(0)
                : findInInventory(player, new ItemStack(ModItems.STASIS_WATCH));
            if (back.isEmpty() || Modules.effects(back).radius() != 1 || Modules.effects(back).filter() != TimeFilter.MONSTERS) {
                helper.fail("Das herausgenommene Werkzeug muss seine Module behalten");
            }
            if (!menu.getSlot(1).getItem().isEmpty() || menu.activeModuleSlots() != 0) {
                helper.fail("Ohne Werkzeug müssen die Modulplätze leer und gesperrt sein");
            }
            int radiusModules = count(player, ModItems.RADIUS_MODULE.getDefaultInstance()) + Modules.effects(back).radius();
            if (radiusModules != 1) {
                helper.fail("Es darf genau ein Radiusmodul geben, gezählt: " + radiusModules);
            }
            menu.removed(player);
            player.discard();
            helper.succeed();
        });
    }

    /**
     * Teil-Zurückspulen (nur Tiere) bewegt Lebende zurück, weckt aber keine Toten auf (ihre Beute ist
     * schon gefallen) und lässt Gebautes stehen.
     */
    @GameTest(template = ARENA, timeoutTicks = 80, setupTicks = 20, batch = "timemod_rewind_filter")
    public void filteredRewindOnlyTouchesChosenCategory(GameTestHelper helper) {
        Pig pig = helper.spawn(EntityType.PIG, new Vec3(12.5, 2, 8.5));
        pig.setNoAi(true);
        Pig walker = helper.spawn(EntityType.PIG, new Vec3(16.5, 2, 12.5));
        walker.setNoAi(true);
        Vec3 walkerStart = walker.position();
        ChronoJournal journal = ServerTimeEngine.of(helper.getLevel()).journal();
        journal.clearAnchors();
        journal.addAnchor(Vec3.atCenterOf(helper.absolutePos(new BlockPos(12, 3, 8))), 24);
        journal.setRecording(true);
        BlockPos built = new BlockPos(14, 2, 8);
        helper.runAtTickTime(5, () -> helper.setBlock(built, Blocks.GOLD_BLOCK));
        helper.runAtTickTime(6, () -> walker.teleportTo(walkerStart.x + 4, walkerStart.y, walkerStart.z));
        helper.runAtTickTime(7, pig::kill);
        helper.runAtTickTime(40, () -> {
            if (helper.getLevel().getEntity(pig.getUUID()) != null) {
                helper.fail("Das Schwein sollte nach der Todesanimation verschwunden sein");
            }
            journal.rewind(36, 0, null, null, TimeFilter.ANIMALS);
            journal.setRecording(false);
            journal.clearAnchors();
            if (helper.getLevel().getEntity(pig.getUUID()) != null) {
                helper.fail("Teil-Zurückspulen darf keine Toten aufwecken (sonst ließe sich Beute verdoppeln)");
            }
            if (walker.position().distanceTo(walkerStart) > 0.5) {
                helper.fail("Das lebende Schwein muss an seinen Ausgangspunkt zurück, ist bei " + walker.position());
            }
            if (!helper.getBlockState(built).is(Blocks.GOLD_BLOCK)) {
                helper.fail("Mit Tier-Filter darf der gebaute Block nicht verschwinden");
            }
            walker.discard();
            helper.succeed();
        });
    }

    /** Wer Gegenstände wählt, spult alles Gegenständliche zurück: Das Schwein lebt, seine Beute ist weg, der Block auch. */
    @GameTest(template = ARENA, timeoutTicks = 80, setupTicks = 20, batch = "timemod_rewind_filter_full")
    public void itemBoundFilterRewindsEverything(GameTestHelper helper) {
        Pig pig = helper.spawn(EntityType.PIG, new Vec3(12.5, 2, 8.5));
        pig.setNoAi(true);
        ChronoJournal journal = ServerTimeEngine.of(helper.getLevel()).journal();
        journal.clearAnchors();
        journal.addAnchor(Vec3.atCenterOf(helper.absolutePos(new BlockPos(12, 3, 8))), 24);
        journal.setRecording(true);
        BlockPos built = new BlockPos(14, 2, 8);
        helper.runAtTickTime(5, () -> helper.setBlock(built, Blocks.GOLD_BLOCK));
        helper.runAtTickTime(7, pig::kill);
        helper.runAtTickTime(40, () -> {
            AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(40);
            if (helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, e -> e.getItem().is(Items.PORKCHOP)).isEmpty()) {
                helper.fail("Der Test ist wertlos: Das Schwein hat nichts fallen gelassen");
            }
            journal.rewind(36, 0, null, null, TimeFilter.ITEMS);
            journal.setRecording(false);
            journal.clearAnchors();
            if (!(helper.getLevel().getEntity(pig.getUUID()) instanceof Pig revived) || !revived.isAlive()) {
                helper.fail("Beim vollen Zurückspulen kehrt das Schwein zurück");
                return;
            }
            if (!helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, e -> e.getItem().is(Items.PORKCHOP)).isEmpty()) {
                helper.fail("Die Beute muss mit zurückgespult werden – sonst gäbe es sie doppelt");
            }
            if (helper.getBlockState(built).is(Blocks.GOLD_BLOCK)) {
                helper.fail("Gegenstände ziehen Blöcke mit: Der gebaute Block muss verschwinden");
            }
            revived.discard();
            helper.succeed();
        });
    }

    private static ItemStack findInInventory(ServerPlayer player, ItemStack like) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(like.getItem())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static int count(ServerPlayer player, ItemStack like) {
        int n = player.containerMenu.getCarried().is(like.getItem()) ? player.containerMenu.getCarried().getCount() : 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(like.getItem())) {
                n += stack.getCount();
            }
        }
        return n;
    }
}
