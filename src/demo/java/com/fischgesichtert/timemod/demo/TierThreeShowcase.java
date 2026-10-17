package com.fischgesichtert.timemod.demo;

import com.fischgesichtert.timemod.block.entity.TemporalWorkbenchBlockEntity;
import com.fischgesichtert.timemod.demo.mixin.MouseHandlerInvoker;
import com.fischgesichtert.timemod.menu.TemporalWorkbenchMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Vorführung der Stufe III: erwachte Temporal-Werkbank, Module, Einbau-Oberfläche. */
final class TierThreeShowcase {
    private static final BlockPos BENCH = new BlockPos(0, 100, 4);
    private static final String[] MODULES = {"blank_module", "filter_animals", "filter_monsters", "filter_villagers", "filter_players",
        "filter_projectiles", "filter_items", "filter_vehicles", "filter_growth", "filter_environment", "filter_fluids", "filter_redstone",
        "filter_machines", "radius_module", "duration_module", "strength_module", "efficiency_module", "clockwork_core", "phage_heart"};

    private TierThreeShowcase() {
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
            .command("time set 6000")
            .command("kill @e[type=!player]")
            .command("fill -10 99 -10 10 99 12 polished_deepslate")
            .command("fill -10 100 -10 10 112 12 air")
            .command("fill -2 99 2 2 99 6 chiseled_polished_blackstone")
            // Rückwand mit allen Modulen in Leuchtrahmen
            .command("fill -10 99 11 10 106 11 dark_prismarine");
        d.command("setblock 0 100 4 timemod:temporal_workbench");
        for (int x = -2; x <= 2; x += 4) {
            for (int z = 2; z <= 6; z += 4) {
                d.command("fill " + x + " 100 " + z + " " + x + " 101 " + z + " timemod:chrono_pillar");
            }
        }
        d.wait(5).command("data merge block 0 100 4 {Tool:{id:\"timemod:time_accelerator\",Count:1b,tag:{Chronon:4000,"
            + "Modules:[\"timemod:radius_module\",\"timemod:strength_module\",\"timemod:filter_growth\"]}}}");
        for (int i = 0; i < MODULES.length; i++) {
            int row = i < 10 ? 104 : 103;
            int col = i < 10 ? 4 - i : 13 - i;
            d.command("summon glow_item_frame " + (col * 2 + 1) + " " + row + " 10 {Facing:2b,Fixed:1b,Invulnerable:1b,Item:{id:\"timemod:"
                + MODULES[i] + "\",Count:1b}}");
        }

        // Tag: Überblick, dann die drehenden Ringe als Sequenz
        d.command("tp @a 0.5 101.2 -3.5 0 12").wait(40).screenshot("t3_01_werkbank_tag")
            .record("seq_t3_ringe", 1).wait(80).stopRecording();
        d.command("tp @a 0.5 101.4 1.0 0 0").wait(10).screenshot("t3_02_modulwand");
        // Nacht: die Ringe leuchten
        d.command("time set 18000").command("tp @a 3.6 101.6 -0.6 30 20").wait(30).screenshot("t3_03_werkbank_nacht");

        // Chrono-Rüstung, Lebenszeit-Extraktor mit alternden Kühen, Zeitsicht des Helms
        d.command("time set 6000")
            .command("summon armor_stand -5.5 100 4.5 {Rotation:[200f,0f],ShowArms:1b,NoBasePlate:1b,ArmorItems:["
                + "{id:\"timemod:chrono_boots\",Count:1b},{id:\"timemod:chrono_leggings\",Count:1b},"
                + "{id:\"timemod:chrono_chestplate\",Count:1b},{id:\"timemod:chrono_helmet\",Count:1b}],"
                + "HandItems:[{id:\"timemod:stasis_watch\",Count:1b},{}]}")
            .command("setblock 6 100 4 timemod:lifetime_extractor")
            .command("summon cow 6.5 100.7 4.5 {NoAI:1b,Rotation:[160f,0f]}")
            .command("summon cow 8.5 100 2.5 {NoAI:1b,Rotation:[120f,0f]}")
            .command("summon cow 9.5 100 7.5 {NoAI:1b,Rotation:[200f,0f]}")
            .command("tp @a 0.5 101.5 -5.5 0 10").wait(140).screenshot("t3_07_ruestung_extraktor")
            .command("tp @a 6.5 101.4 0.5 0 25").wait(10).screenshot("t3_08_extraktor_nah")
            .command("item replace entity @a armor.head with timemod:chrono_helmet")
            .command("execute positioned -6 101 -2 run chrono zone 3 3")
            .command("execute positioned 5 101 -3 run chrono zone 2.5 0.25")
            .command("execute positioned 0 102 9 run chrono zone 4 0")
            .command("tp @a 0.5 102.5 -9.5 0 12").wait(20).screenshot("t3_09_zeitsicht")
            .command("chrono clear").command("item replace entity @a armor.head with air").wait(5);

        // Oberfläche: Werkzeug mit Modulen
        d.command("gamemode creative @a").command("time set 6000").command("tp @a 0.5 100 0.5 0 30").wait(10)
            .client(mc -> mc.options.hideGui = false)
            .server(player -> {
                if (player.level().getBlockEntity(BENCH) instanceof TemporalWorkbenchBlockEntity workbench) {
                    workbench.open(player);
                }
            }).wait(10)
            .client(mc -> hover(mc, TemporalWorkbenchMenu.TOOL_X + 8, TemporalWorkbenchMenu.TOOL_Y + 8)).wait(6)
            .client(mc -> mc.getToasts().clear())
            .screenshot("t3_04_gui_werkzeug")
            .client(mc -> hover(mc, TemporalWorkbenchMenu.MODULE_X + 8, TemporalWorkbenchMenu.MODULE_Y[2] + 8)).wait(6)
            .screenshot("t3_05_gui_modul")
            .client(mc -> hover(mc, 120, 60)).wait(6)
            .screenshot("t3_06_gui")
            .client(mc -> mc.setScreen(null)).wait(5);
        d.wait(5).quit();
    }

    private static void hover(Minecraft mc, int x, int y) {
        if (mc.screen == null) {
            return;
        }
        double scale = mc.getWindow().getGuiScale();
        int left = (mc.screen.width - 176) / 2;
        int top = (mc.screen.height - 166) / 2;
        ((MouseHandlerInvoker) mc.mouseHandler).timemod$onMove(mc.getWindow().getWindow(), (left + x) * scale, (top + y) * scale);
    }
}
