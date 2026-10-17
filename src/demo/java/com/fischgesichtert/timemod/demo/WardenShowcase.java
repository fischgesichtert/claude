package com.fischgesichtert.timemod.demo;

import com.fischgesichtert.timemod.entity.TimeWarden;
import com.fischgesichtert.timemod.paradox.ParadoxPressure;
import com.fischgesichtert.timemod.paradox.TimeWardens;
import com.fischgesichtert.timemod.registry.ModEntities;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Vorführung „Paradox-Druck und Zeitwächter“: ein Wächter entfaltet sich aus einem Lichtstrich, ein
 * Porträt mit Zifferblatt-Maske und Heiligenschein, ein Stasis-Zeiger im Flug – und aus Sicht des
 * Spielers die Anzeige des Drucks, die ab 50 % ausfransende Welt und die Jagd.
 */
final class WardenShowcase {
    private WardenShowcase() {
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
            .command("gamerule doTimeWardens false")
            .command("difficulty normal")
            .command("weather clear")
            .command("time set 12900")
            .command("kill @e[type=!player]")
            .command("fill -24 99 -24 24 99 24 polished_deepslate")
            .command("fill -24 100 -24 24 124 24 air")
            .command("fill -24 98 -24 24 98 24 deepslate_tiles");
        // Der Platz als Zifferblatt: Ring aus Kalzit, zwölf Stundenmarken aus Gold, Laternen auf Säulen
        for (int i = 0; i < 96; i++) {
            double angle = i * Math.PI * 2 / 96;
            d.command(String.format(Locale.ROOT, "setblock %d 99 %d calcite", (int) Math.round(Math.cos(angle) * 11), (int) Math.round(Math.sin(angle) * 11)));
        }
        for (int hour = 0; hour < 12; hour++) {
            double angle = hour * Math.PI / 6;
            int x = (int) Math.round(Math.cos(angle) * 9);
            int z = (int) Math.round(Math.sin(angle) * 9);
            d.command(String.format(Locale.ROOT, "setblock %d 99 %d gold_block", x, z));
            if (hour % 3 == 1) {
                // Säulen schräg zwischen den Stundenmarken, damit sie keinem Wächter im Rücken stehen
                double between = angle + Math.PI / 12;
                int px = (int) Math.round(Math.cos(between) * 14);
                int pz = (int) Math.round(Math.sin(between) * 14);
                d.command(String.format(Locale.ROOT, "fill %d 100 %d %d 103 %d polished_blackstone_bricks", px, pz, px, pz))
                    .command(String.format(Locale.ROOT, "setblock %d 104 %d lantern", px, pz));
            }
        }
        d.command("setblock 0 99 0 timemod:brass_block")
            .command("fill 0 99 1 0 99 6 polished_blackstone")
            .command("fill 1 99 0 4 99 0 polished_blackstone");

        // 1) Ein Wächter entfaltet sich aus einem Lichtstrich (vorher genug Zeit, damit der Platz fertig gezeichnet ist)
        d.command("tp @a 0.5 100.4 -1.2 0 -3").wait(140)
            .server(player -> warden(player.serverLevel(), new Vec3(0.5, 100, 3.5), 180.0F, true).setNoAi(true))
            .record("seq_w_erscheinen", 1).wait(34).stopRecording()
            .wait(20).screenshot("w1_erschienen");

        // 2) Porträt: Zifferblatt-Maske („fünf vor zwölf“), Heiligenschein, Zeigerklinge
        d.server(player -> look(player, new Vec3(0.5, 101.9, 0.4), new Vec3(0.5, 101.9, 3.5)))
            .wait(12).screenshot("w2_portraet")
            .server(player -> look(player, new Vec3(-3.2, 101.2, 1.2), new Vec3(0.5, 101.3, 3.5)))
            .wait(12).screenshot("w3_seite");

        // 3) Ein Stasis-Zeiger fliegt – auf einen Dorfbewohner, der nichts dafür kann
        d.command("summon villager 9.5 100 3.5 {NoAI:1b,Invulnerable:1b,Silent:1b,Rotation:[90f,0f]}").wait(2)
            .server(player -> find(player).ifPresent(warden -> {
                warden.setYRot(-90.0F);
                warden.setYHeadRot(-90.0F);
                warden.setYBodyRot(-90.0F);
            }))
            .server(player -> look(player, new Vec3(5.0, 101.4, -3.8), new Vec3(5.0, 101.0, 3.5)))
            .wait(10)
            .record("seq_w_zeiger", 1)
            .wait(6)
            .server(player -> find(player).ifPresent(warden ->
                player.serverLevel().getEntitiesOfClass(Villager.class, new AABB(warden.blockPosition()).inflate(20)).stream().findFirst()
                    .ifPresent(warden::throwBolt)))
            .wait(8).screenshot("w4_zeiger_im_flug")
            .wait(30).stopRecording()
            .command("kill @e[type=timemod:time_warden]")
            .command("kill @e[type=villager]")
            .wait(10);

        // 4) Aus Sicht des Spielers: Druck, Ausfransen, Jagd
        d.command("gamemode survival @a")
            .command("effect give @a resistance infinite 255 true")
            .command("effect give @a saturation infinite 255 true")
            .command("effect give @a regeneration infinite 255 true")
            .command("item replace entity @a weapon.mainhand with timemod:stasis_watch{Chronon:3600}")
            .command("tp @a 0.5 100 -8.5 0 8")
            .wait(30)
            .client(mc -> {
                mc.options.hideGui = false;
                mc.getToasts().clear();
                mc.gui.getChat().clearMessages(false);
            })
            .server(player -> ParadoxPressure.set(player, 18.0F))
            .wait(30).screenshot("w5_druck_gering")
            .server(player -> ParadoxPressure.set(player, 31.0F))
            .wait(30).screenshot("w6_unruhig")
            .server(player -> ParadoxPressure.set(player, 62.0F))
            .wait(40).screenshot("w7_bemerkt")
            .server(player -> ParadoxPressure.set(player, 88.0F))
            .wait(20)
            // Zwei Wächter erscheinen vor dem Spieler und kommen auf ihn zu
            .server(player -> {
                for (double side : new double[] {-3.5, 3.5}) {
                    TimeWarden warden = warden(player.serverLevel(), new Vec3(0.5 + side, 100, 5.5), 180.0F, false);
                    warden.hunt(player);
                    warden.arrive();
                    player.serverLevel().addFreshEntity(warden);
                }
            })
            .record("seq_w_jagd", 2);
        for (int t = 0; t < 100; t += 5) {
            d.wait(5).server(WardenShowcase::faceHunters);
            if (t == 20) {
                d.screenshot("w8_gejagt");
            }
        }
        d.stopRecording().screenshot("w9_zeitarrest")
            .wait(5).quit();
    }

    /** Der Spieler schaut zu den Wächtern, die ihn jagen. */
    private static void faceHunters(ServerPlayer player) {
        var hunters = TimeWardens.hunters(player);
        if (hunters.isEmpty()) {
            return;
        }
        Vec3 sum = Vec3.ZERO;
        for (TimeWarden warden : hunters) {
            sum = sum.add(warden.position());
        }
        Vec3 target = sum.scale(1.0 / hunters.size()).add(0, 1.6, 0);
        Vec3 dir = target.subtract(player.getEyePosition());
        float yaw = (float) (Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z)));
        player.connection.teleport(player.getX(), player.getY(), player.getZ(), yaw, pitch);
    }

    /** Ein Wächter an Ort und Stelle; {@code appear}: mit Auftritt und gleich in der Welt, sonst noch nicht hinzugefügt. */
    private static TimeWarden warden(ServerLevel level, Vec3 at, float yaw, boolean appear) {
        TimeWarden warden = ModEntities.TIME_WARDEN.create(level);
        warden.moveTo(at.x, at.y, at.z, yaw, 0.0F);
        warden.setYHeadRot(yaw);
        warden.setYBodyRot(yaw);
        warden.finalizeSpawn(level, level.getCurrentDifficultyAt(warden.blockPosition()), MobSpawnType.COMMAND, null, null);
        if (appear) {
            warden.arrive();
            level.addFreshEntity(warden);
        }
        return warden;
    }

    private static Optional<TimeWarden> find(ServerPlayer player) {
        return player.serverLevel().getEntitiesOfClass(TimeWarden.class, new AABB(player.blockPosition()).inflate(48)).stream().findFirst();
    }

    private static void look(ServerPlayer player, Vec3 eye, Vec3 target) {
        Vec3 dir = target.subtract(eye);
        float yaw = (float) (Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z)));
        player.teleportTo(player.serverLevel(), eye.x, eye.y - player.getEyeHeight(), eye.z, yaw, pitch);
    }
}
