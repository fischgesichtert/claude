package com.fischgesichtert.timemod.demo;

import com.fischgesichtert.timemod.chronos.ChronosSettings;
import com.fischgesichtert.timemod.client.chronos.ChronosClient;
import com.fischgesichtert.timemod.client.chronos.RadialMenuScreen;
import com.fischgesichtert.timemod.demo.mixin.MouseHandlerInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;

/** Vorführung der Chronos-Uhr: HUD, Radialmenü, Geisterpfad, Scrubbing und globaler Zeitstopp. */
final class ChronosShowcase {
    private ChronosShowcase() {
    }

    static void write(DemoDirector d) {
        d.wait(20)
            .command("gamemode spectator @a")
            .command("forceload add -32 -32 32 32")
            .command("tp @a 0 104 -12 0 15")
            .wait(60)
            .command("gamerule sendCommandFeedback false")
            .command("gamerule announceAdvancements false")
            .command("gamerule doMobSpawning false")
            .command("gamerule doDaylightCycle false")
            .command("gamerule doWeatherCycle false")
            .command("weather clear")
            .command("time set 5000")
            .command("kill @e[type=!player]")
            .command("fill -24 98 -24 24 98 24 dirt")
            .command("fill -24 99 -24 24 99 24 grass_block")
            .command("fill -24 100 -24 24 118 24 air")
            // Haus
            .command("fill 4 100 6 10 103 12 oak_planks hollow")
            .command("fill 4 104 6 10 104 12 dark_oak_planks")
            .command("fill 7 100 6 7 101 6 air")
            .command("setblock 5 102 6 glass_pane")
            .command("setblock 9 102 6 glass_pane")
            .command("fill 4 100 5 10 100 5 flowering_azalea_leaves")
            // Feld mit Wasser
            .command("fill -10 99 6 -3 99 13 farmland")
            .command("fill -10 100 6 -3 100 13 wheat[age=7]")
            .command("fill -7 99 6 -7 99 13 water")
            .command("fill -7 100 6 -7 100 13 air")
            // Mauer für die Explosion
            .command("fill -3 100 16 3 104 16 spruce_planks")
            .command("fill -2 101 16 2 103 16 glass")
            .command("place feature minecraft:oak 13 100 14")
            .command("place feature minecraft:birch 14 100 2")
            .command("place feature minecraft:oak -14 100 3");
        for (int i = 0; i < 4; i++) {
            d.command("summon cow " + (-2 + i * 2) + " 100 " + (4 + i % 2 * 3));
        }
        d.command("summon sheep 2 100 10 {Color:0}").command("summon sheep -1 100 11 {Color:6}").command("summon sheep 3 100 2 {Color:3}")
            .command("summon chicken 0 100 3").command("summon chicken 1 100 5")
            .command("summon villager 6 100 3 {VillagerData:{profession:\"minecraft:librarian\",level:2,type:\"minecraft:plains\"}}");

        // Spieler mit Chronos-Uhr (Meisterschaft „Zeitmeister“)
        d.wait(20)
            .command("difficulty peaceful")
            .command("gamemode survival @a")
            .command("effect give @a resistance infinite 255 true")
            .command("effect give @a saturation infinite 255 true")
            .command("item replace entity @a weapon.mainhand with timemod:chronos_watch{Chronon:186500,Chronos:{Mode:0,Mastery:420,"
                + "Radius:32,Stop:5,Rewind:8}}")
            .command("tp @a 0.5 100 -7.5 0 12")
            .wait(60)
            .client(mc -> {
                mc.options.hideGui = false;
                mc.getToasts().clear();
                mc.gui.getChat().clearMessages(false);
            })
            .wait(10).screenshot("ch1_hud");

        // Radialmenü: Zeiger auf „Zeitraffer“, dann ein Filter-Tooltip
        d.client(mc -> mc.setScreen(new RadialMenuScreen())).wait(4)
            .client(mc -> pointAt(mc, 0, 57)).wait(4).screenshot("ch2_radial")
            .client(mc -> pointAt(mc, (float) (-Math.PI / 2 + 7.5 * Math.PI * 2 / 14), 102)).wait(4).screenshot("ch3_filter")
            .client(mc -> ChronosClient.send(ChronosSettings.Action.MODE, ChronosSettings.Mode.REWIND.ordinal()))
            .client(mc -> pointAt(mc, (float) Math.PI, 57)).wait(6).screenshot("ch4_radial_rewind")
            .client(mc -> mc.setScreen(null)).wait(10);

        // Etwas passiert: Der Spieler geht rückwärts vom Haus weg, an der Mauer explodiert TNT
        d.command("tp @a 0.50 100 4.00 0 18").wait(70)
            .record("seq_ch_story", 2)
            .command("summon tnt -1 100 15 {Fuse:24}")
            .command("summon tnt 1 100 15 {Fuse:26}");
        for (int t = 0; t < 70; t += 2) {
            double progress = t / 70.0;
            double x = 0.5 + 3.0 * Math.sin(progress * Math.PI * 2);
            double z = 4.0 - 11.5 * progress;
            d.wait(2).command(String.format(java.util.Locale.ROOT, "tp @a %.2f 100 %.2f 0 18", x, z));
        }
        d.wait(30).screenshot("ch5_geisterpfad");

        // Scrubbing: Taste halten – die Welt läuft rückwärts; loslassen hält sie an
        d.client(mc -> mc.options.keyUse.setDown(true))
            .wait(46)
            .client(mc -> mc.options.keyUse.setDown(false))
            .wait(40)
            .stopRecording()
            .screenshot("ch6_angehalten");

        // Globaler Zeitstopp
        d.client(mc -> {
                ChronosClient.send(ChronosSettings.Action.MODE, ChronosSettings.Mode.STOP.ordinal());
                ChronosClient.send(ChronosSettings.Action.GLOBAL, 0);
            })
            .command("tp @a 0.5 101 -9.5 0 6")
            .wait(10)
            .server(player -> player.getMainHandItem().getItem().use(player.level(), player, InteractionHand.MAIN_HAND))
            .wait(16).screenshot("ch7_global_stopp")
            .wait(10).client(mc -> mc.setScreen(new RadialMenuScreen())).wait(6).screenshot("ch8_radial_global")
            .client(mc -> mc.setScreen(null));
        d.wait(10).quit();
    }

    /** Zeiger im Radialmenü auf Winkel/Abstand vom Mittelpunkt setzen (Layout-Maß, wie im Menü skaliert). */
    private static void pointAt(Minecraft mc, float angle, float distance) {
        if (mc.screen == null) {
            return;
        }
        float scale = Math.min(1.0F, Math.min(mc.screen.width / 520.0F, mc.screen.height / 300.0F));
        double x = mc.screen.width / 2.0 + Math.cos(angle) * distance * scale;
        double y = mc.screen.height / 2.0 - 6 + Math.sin(angle) * distance * scale;
        double gui = mc.getWindow().getGuiScale();
        ((MouseHandlerInvoker) mc.mouseHandler).timemod$onMove(mc.getWindow().getWindow(), x * gui, y * gui);
    }
}
