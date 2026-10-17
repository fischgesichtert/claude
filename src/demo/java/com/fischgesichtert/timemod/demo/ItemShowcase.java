package com.fischgesichtert.timemod.demo;

import com.fischgesichtert.timemod.demo.mixin.CreativeScreenInvoker;
import com.fischgesichtert.timemod.demo.mixin.MouseHandlerInvoker;
import com.fischgesichtert.timemod.registry.ModItems;
import com.fischgesichtert.timemod.registry.ModTab;
import java.util.List;
import net.fabricmc.fabric.impl.client.itemgroup.CreativeGuiExtensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Vorführung der Stufe-I/II-Inhalte: Werkstatt mit allen Blöcken, Leuchtrahmen-Wand mit allen
 * Items, Kreativ-Reiter mit Tooltip, Uhrmacher-Werkbank mit Rezept und Werkzeuge in der Hand.
 */
final class ItemShowcase {
    private static final String[] ROW_TOOLS = {"pocket_watch", "growth_hourglass", "brake_spring", "moment_lens", "recall_watch",
        "stasis_watch", "time_accelerator", "slow_pendulum", "rewind_spool", "chrono_knife", "pocket_time_cell"};
    private static final String[] ROW_PARTS = {"chronite_time_cell", "brass_ingot", "brass_nugget", "brass_gear", "mainspring",
        "balance_wheel", "clockwork", "raw_chronite", "chronite_crystal", "time_dust", "chronite_seed"};
    private static final BlockPos BENCH = new BlockPos(5, 100, 3);

    private ItemShowcase() {
    }

    static void write(DemoDirector d) {
        d.wait(20)
            .command("gamemode spectator @a")
            .command("forceload add -32 -32 32 32")
            .command("tp @a 0 104 -8 0 15")
            .wait(60)
            .command("gamerule sendCommandFeedback false")
            .command("gamerule doMobSpawning false")
            .command("gamerule doDaylightCycle false")
            .command("gamerule doWeatherCycle false")
            .command("weather clear")
            .command("time set 5500")
            .command("kill @e[type=!player]");
        buildWorkshop(d);

        // Werkstatt im Überblick (läuft ein paar Sekunden: Erz flackert, Kondensator dreht)
        d.command("tp @a 0.5 102.6 -5.5 0 18").wait(40).screenshot("items_01_werkstatt")
            .record("seq_items_werkstatt", 1).wait(80).stopRecording();
        // Die Wand mit allen Items
        d.command("tp @a 0.5 100.4 1.2 0 0").wait(20).screenshot("items_02_wand");
        // Nahaufnahme Maschinen
        d.command("tp @a 2.5 101.2 0.2 0 30").wait(20).screenshot("items_03_maschinen");

        // Kreativ-Reiter TIME mit Tooltip über der Stasis-Uhr
        d.command("gamemode creative @a").command("tp @a 0.5 101 -5.5 0 10").wait(10)
            .client(mc -> mc.options.hideGui = false)
            .client(ItemShowcase::openTimeTab).wait(10)
            .client(mc -> hover(mc, 195, 136, 9 + 5 * 18 + 8, 18 + 8)).wait(6)
            .client(mc -> mc.getToasts().clear())
            .screenshot("items_04_kreativ")
            .client(mc -> hover(mc, 195, 136, 9 + 2 * 18 + 8, 18 + 1 * 18 + 8)).wait(6)
            .screenshot("items_05_kreativ_zelle")
            .client(mc -> mc.setScreen(null)).wait(5);

        // Uhrmacher-Werkbank mit Taschenuhr-Rezept (Menü schließt sich, wenn man weiter als 8 Blöcke weg ist)
        d.command("tp @a 5.5 100 -1.5 0 30").wait(5).server(player -> {
            var level = player.serverLevel();
            player.openMenu(level.getBlockState(BENCH).getMenuProvider(level, BENCH));
            AbstractContainerMenu menu = player.containerMenu;
            List<ItemStack> grid = List.of(ItemStack.EMPTY, new ItemStack(ModItems.BRASS_NUGGET), ItemStack.EMPTY,
                new ItemStack(Items.GLASS_PANE), new ItemStack(ModItems.CLOCKWORK), new ItemStack(Items.GLASS_PANE),
                ItemStack.EMPTY, new ItemStack(ModItems.BRASS_NUGGET), ItemStack.EMPTY);
            for (int i = 0; i < 9; i++) {
                menu.getSlot(1 + i).set(grid.get(i));
            }
            menu.broadcastChanges();
        }).wait(10)
            .client(mc -> hover(mc, 176, 166, 124 + 8, 35 + 8)).wait(6)
            .client(mc -> mc.getToasts().clear())
            .screenshot("items_06_uhrmacher")
            .client(mc -> mc.setScreen(null)).wait(5);

        // Werkzeuge in der Hand (mit Hotbar)
        String[] hotbar = {"stasis_watch", "rewind_spool", "time_accelerator", "pocket_watch", "chrono_knife", "growth_hourglass",
            "slow_pendulum", "moment_lens", "chronite_time_cell"};
        for (int i = 0; i < hotbar.length; i++) {
            d.command("item replace entity @a hotbar." + i + " with timemod:" + hotbar[i]);
        }
        d.command("tp @a 0.5 100 -3.5 0 25").wait(10);
        for (int i = 0; i < 4; i++) {
            int slot = i;
            d.client(mc -> {
                mc.player.getInventory().selected = slot;
                mc.getToasts().clear();
            }).wait(14).screenshot("items_1" + i + "_hand");
        }
        d.wait(5).quit();
    }

    private static void buildWorkshop(DemoDirector d) {
        d.command("fill -9 99 -9 9 99 9 dark_oak_planks")
            .command("fill -9 99 -9 9 99 -9 stripped_spruce_log[axis=x]")
            .command("fill -9 100 -9 9 110 9 air")
            // Rückwand mit Zeitglas-Fenstern
            .command("fill -8 99 7 8 105 7 spruce_planks")
            .command("fill -8 105 7 8 105 7 stripped_spruce_log[axis=x]")
            .command("fill -8 99 7 -8 105 7 stripped_spruce_log")
            .command("fill 8 99 7 8 105 7 stripped_spruce_log")
            .command("fill -7 104 7 7 104 7 timemod:time_glass")
            // Werkbank-Reihe
            .command("setblock 5 100 3 timemod:clockmaker_bench[facing=north]")
            .command("setblock 3 100 3 timemod:time_condenser")
            .command("setblock 1 100 3 timemod:time_field_projector")
            .command("setblock -1 100 3 timemod:brass_block")
            .command("setblock -3 100 3 timemod:time_glass")
            .command("setblock -5 100 3 timemod:deepslate_chronite_ore")
            .command("setblock -6 100 4 timemod:deepslate_chronite_ore")
            .command("setblock -5 101 3 timemod:deepslate_chronite_ore")
            .command("setblock -7 100 3 timemod:deepslate_chronite_ore")
            // Beet mit Zeitsand (rieselt – also mit Unterbau) und Chronit-Sprossen aller Stufen
            .command("fill -3 98 0 3 98 0 dark_oak_planks")
            .command("fill -3 99 0 3 99 0 timemod:time_sand")
            .command("setblock 3 100 0 timemod:chronite_sprout[age=0]")
            .command("setblock 1 100 0 timemod:chronite_sprout[age=1]")
            .command("setblock -1 100 0 timemod:chronite_sprout[age=2]")
            .command("setblock -3 100 0 timemod:chronite_sprout[age=3]")
            .command("setblock -7 100 1 lantern")
            .command("setblock 8 100 1 lantern");
        d.wait(5)
            .command("data merge block 3 100 3 {Fuel:{id:\"timemod:chronite_crystal\",Count:6b},Charging:{id:\"timemod:stasis_watch\",Count:1b}}")
            .command("data merge block 1 100 3 {Buffer:40000,Factor:0,Radius:0}");
        for (int i = 0; i < ROW_TOOLS.length; i++) {
            frame(d, 5 - i, 102, ROW_TOOLS[i]);
            frame(d, 5 - i, 101, ROW_PARTS[i]);
        }
        frame(d, 6, 100, "chrono_debug_wand");
    }

    private static void frame(DemoDirector d, int x, int y, String item) {
        d.command("summon glow_item_frame " + x + " " + y + " 6 {Facing:2b,Fixed:1b,Invulnerable:1b,Item:{id:\"timemod:" + item + "\",Count:1b}}");
    }

    private static void openTimeTab(Minecraft mc) {
        CreativeModeInventoryScreen screen = new CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), true);
        mc.setScreen(screen);
        CreativeModeTabs.tryRebuildTabContents(mc.player.connection.enabledFeatures(), true, mc.level.registryAccess());
        for (int page = 0; page < 6 && CreativeScreenInvoker.timemod$selectedTab() != ModTab.TAB; page++) {
            ((CreativeScreenInvoker) screen).timemod$selectTab(ModTab.TAB);
            if (CreativeScreenInvoker.timemod$selectedTab() != ModTab.TAB) {
                ((CreativeGuiExtensions) screen).fabric_nextPage();
            }
        }
    }

    /** Setzt den Mauszeiger auf einen Punkt im Fenster der geöffneten GUI (für Tooltips). */
    private static void hover(Minecraft mc, int imageWidth, int imageHeight, int x, int y) {
        if (mc.screen == null) {
            return;
        }
        double scale = mc.getWindow().getGuiScale();
        int left = (mc.screen.width - imageWidth) / 2;
        int top = (mc.screen.height - imageHeight) / 2;
        ((MouseHandlerInvoker) mc.mouseHandler).timemod$onMove(mc.getWindow().getWindow(), (left + x) * scale, (top + y) * scale);
    }
}
