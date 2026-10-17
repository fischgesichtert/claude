package com.fischgesichtert.timemod.demo;

/**
 * Das Drehbuch der Vorführung. Bühne auf y=100 um (0, 100, 0), Kamera südlich davor.
 * Eine Lore-Rundbahn sorgt für gleichmäßige Bewegung, an der man das Tempo gut erkennt.
 */
final class DemoScript {
    private static final String CAMERA = "tp @a 0 106 -21 0 22";
    private static final String AT_STAGE = "execute positioned 0 100 -2 run ";

    private DemoScript() {
    }

    static void write(DemoDirector d) {
        // Kamera an den Drehort, Chunks laden lassen
        d.wait(20)
            .command("gamemode spectator @a")
            .command("forceload add -32 -32 32 32")
            .command(CAMERA)
            .wait(60)
            .command("gamerule doMobSpawning false")
            .command("gamerule doDaylightCycle true")
            .command("gamerule doWeatherCycle false")
            .command("weather clear")
            .command("time set 5000")
            .command("kill @e[type=!player]");
        buildStage(d);
        d.command(CAMERA);

        // Einschwingen
        d.wait(60).screenshot("01_buehne");

        // 1) Normale Zeit als Vergleich
        d.record("seq_1_normal", 1).wait(100).stopRecording();

        // 2) 4× vorgespult
        d.command(AT_STAGE + "chrono zone 16 4").wait(1)
            .record("seq_2_vorspulen_4x", 1).wait(100).stopRecording()
            .command("chrono clear").wait(5);

        // 3) Zeitlupe 0,25×
        d.command(AT_STAGE + "chrono zone 16 0.25").wait(1)
            .record("seq_3_zeitlupe", 1).wait(100).stopRecording()
            .command("chrono clear").wait(5);

        // 4) Zeitstopp von außen gesehen: Welle, graue Kugel, Weiterlaufen
        d.record("seq_4_zeitstopp_aussen", 1)
            .command(AT_STAGE + "chrono zone 16 0").wait(50)
            .command("chrono clear").wait(30)
            .stopRecording().wait(5);

        // 5) Zeitstopp mit der Kamera mittendrin (der „Die Welt“-Moment)
        d.record("seq_5_zeitstopp_innen", 1)
            .command(AT_STAGE + "chrono zone 30 0").wait(50)
            .command("chrono clear").wait(30)
            .stopRecording().wait(5);

        // 6) Globale Zeit 5×: Sonne und Wolken rasen, alles läuft schneller
        d.command("chrono global 5").wait(1)
            .record("seq_6_global_5x", 1).wait(100).stopRecording()
            .command("chrono clear");

        d.wait(5).screenshot("99_ende").wait(2).quit();
    }

    private static void buildStage(DemoDirector d) {
        d.command("fill -14 99 -14 14 99 14 grass_block")
            .command("fill -14 100 -14 14 112 14 air")
            .command("fill -14 100 -14 14 100 -14 oak_fence")
            .command("fill -14 100 14 14 100 14 oak_fence")
            .command("fill -14 100 -14 -14 100 14 oak_fence")
            .command("fill 14 100 -14 14 100 14 oak_fence")
            // Feld, Wasser, Lagerfeuer, Setzlinge, Wasserfall
            .command("fill -11 99 4 -3 99 11 farmland[moisture=7]")
            .command("fill -11 100 4 -3 100 11 wheat[age=2]")
            .command("setblock -7 99 7 water")
            .command("setblock 7 100 8 campfire")
            .command("setblock 10 100 10 campfire")
            .command("setblock 4 100 10 oak_sapling")
            .command("setblock 2 107 6 water")
            // Lore-Rundbahn (Antriebsschienen auf Redstone-Blöcken)
            .command("fill -10 99 -10 10 99 -10 redstone_block")
            .command("fill -10 99 -5 10 99 -5 redstone_block")
            .command("fill -9 100 -10 9 100 -10 powered_rail[shape=east_west,powered=true]")
            .command("fill -9 100 -5 9 100 -5 powered_rail[shape=east_west,powered=true]")
            .command("fill -10 100 -9 -10 100 -6 rail[shape=north_south]")
            .command("fill 10 100 -9 10 100 -6 rail[shape=north_south]")
            .command("setblock -10 100 -10 rail[shape=south_east]")
            .command("setblock 10 100 -10 rail[shape=south_west]")
            .command("setblock 10 100 -5 rail[shape=north_west]")
            .command("setblock -10 100 -5 rail[shape=north_east]");
        d.wait(5)
            .command("summon minecart 0.5 100.1 -9.5 {Motion:[0.6d,0.0d,0.0d]}")
            .command("summon minecart 0.5 100.1 -4.5 {Motion:[-0.6d,0.0d,0.0d]}")
            .command("summon cow 3 100 1")
            .command("summon cow -4 100 -1")
            .command("summon cow 7 100 3")
            .command("summon chicken 0 100 2")
            .command("summon chicken 9 100 -1")
            .command("summon sheep -8 100 1")
            .command("summon pig 10 100 4");
    }
}
