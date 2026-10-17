package com.fischgesichtert.timemod.demo;

/** Das Drehbuch der Vorführung. Koordinaten: Bühne auf y=100 um (0, 100, 0). */
final class DemoScript {
    private DemoScript() {
    }

    static void write(DemoDirector d) {
        // Welt vorbereiten: erst Kamera an den Drehort, Chunks laden lassen, dann bauen
        d.wait(20)
            .command("gamemode spectator @a")
            .command("forceload add -32 -32 32 32")
            .command("tp @a 0 104 -18 0 18")
            .wait(80)
            .command("gamerule doMobSpawning false")
            .command("gamerule doDaylightCycle true")
            .command("gamerule doWeatherCycle false")
            .command("weather clear")
            .command("time set 2000")
            .command("kill @e[type=!player]")
            // Bühne: Grasplattform, Weide, Feld, Wasser, Lagerfeuer
            .command("fill -12 99 -12 12 99 12 grass_block")
            .command("fill -12 100 -12 12 110 12 air")
            .command("fill -12 100 -12 12 100 -12 oak_fence")
            .command("fill -12 100 12 12 100 12 oak_fence")
            .command("fill -12 100 -12 -12 100 12 oak_fence")
            .command("fill 12 100 -12 12 100 12 oak_fence")
            .command("fill -10 99 4 -2 99 10 farmland[moisture=7]")
            .command("fill -10 100 4 -2 100 10 wheat[age=0]")
            .command("fill -6 99 7 -6 99 7 water")
            .command("setblock 6 100 6 campfire")
            .command("setblock 8 100 8 oak_sapling")
            .command("setblock 4 100 8 birch_sapling")
            .command("setblock 2 104 2 water")
            .command("tp @a 0 104 -18 0 18");
        d.wait(10)
            .command("summon cow 2 100 -4")
            .command("summon cow -3 100 -6")
            .command("summon cow 5 100 -2")
            .command("summon chicken 0 100 -8")
            .command("summon chicken 7 100 -7")
            .command("summon sheep -6 100 -2")
            .command("summon pig 8 100 0");
        // Erst laden lassen (Software-Rendering ist langsam)
        d.wait(100).screenshot("01_normal");

        // Normale Zeit als Referenz-Video
        d.record("seq_1_normal", 2).wait(80).stopRecording();

        // 4× vorgespult: gleiche Wege, nur schneller
        d.command("execute positioned 0 100 0 run chrono zone 14 4").wait(2)
            .record("seq_2_vorspulen_4x", 2).wait(80).stopRecording()
            .command("chrono clear");

        // Zeitlupe 0,25×
        d.command("execute positioned 0 100 0 run chrono zone 14 0.25").wait(2)
            .record("seq_3_zeitlupe", 2).wait(80).stopRecording()
            .command("chrono clear");

        // Zeitstopp
        d.command("execute positioned 0 100 0 run chrono zone 14 0").wait(10)
            .screenshot("02_zeitstopp")
            .record("seq_4_zeitstopp", 2).wait(40).stopRecording()
            .command("chrono clear");

        // Globale Zeit 5×: Sonne und Wolken rasen
        d.command("chrono global 5").wait(2)
            .record("seq_5_global_5x", 2).wait(80).stopRecording()
            .command("chrono clear");

        d.wait(10).screenshot("03_ende").wait(5).quit();
    }
}
