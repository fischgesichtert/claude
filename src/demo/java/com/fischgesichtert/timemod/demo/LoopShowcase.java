package com.fischgesichtert.timemod.demo;

import net.minecraft.client.CameraType;

/** Vorführung der Zeitschleife: TNT neben dem Spieler – „Nicht heute.“ – und die Welt spult zurück. */
final class LoopShowcase {
    private LoopShowcase() {
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
            .command("time set 6000")
            .command("kill @e[type=!player]")
            .command("fill -16 98 -16 16 98 16 dirt")
            .command("fill -16 99 -16 16 99 16 grass_block")
            .command("fill -16 100 -16 16 112 16 air")
            .command("fill -6 100 4 -2 103 8 oak_planks hollow")
            .command("fill 3 100 4 7 100 8 hay_block")
            .command("place feature minecraft:oak 9 100 -3")
            .command("summon sheep 2 100 2 {NoAI:1b}")
            .command("summon cow -3 100 0 {NoAI:1b}")
            .wait(10)
            .command("difficulty easy")
            .command("gamemode survival @a")
            .command("clear @a")
            .command("item replace entity @a hotbar.0 with minecraft:iron_sword")
            .command("item replace entity @a hotbar.1 with timemod:time_loop{Chronon:150000}")
            .command("tp @a 0.5 100 -2.5 0 10")
            .client(mc -> {
                mc.options.hideGui = false;
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                mc.getToasts().clear();
                mc.gui.getChat().clearMessages(false);
            })
            // Die Welt soll sich ein paar Sekunden lang erinnern
            .wait(80)
            .record("seq_loop", 1)
            .command("summon tnt 0.5 100 -1.5 {Fuse:24}")
            .wait(40).screenshot("l1_nicht_heute")
            .wait(60)
            .stopRecording()
            .screenshot("l2_zurueck");
        d.wait(5).quit();
    }
}
