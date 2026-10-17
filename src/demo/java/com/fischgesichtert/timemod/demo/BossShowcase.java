package com.fischgesichtert.timemod.demo;

import com.fischgesichtert.timemod.entity.boss.ChronophageBoss;
import com.fischgesichtert.timemod.entity.boss.ClockmakerBoss;
import com.fischgesichtert.timemod.registry.ModEntities;
import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.TimeFilter;
import com.fischgesichtert.timemod.time.ZoneShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Vorführung der Bosse: Uhrmacher (Sägen, Schwachstelle in Zeitlupe) und Chronophage (Maul, Zeitstopp). */
final class BossShowcase {
    private BossShowcase() {
    }

    static void write(DemoDirector d) {
        d.wait(20)
            .command("gamemode spectator @a")
            .command("forceload add -32 -32 32 32")
            .command("tp @a 0 104 -12 0 15")
            .wait(60)
            .command("gamerule sendCommandFeedback false")
            .command("difficulty hard")
            .command("gamerule doMobSpawning false")
            .command("gamerule doDaylightCycle false")
            .command("gamerule doWeatherCycle false")
            .command("weather clear")
            .command("time set 13000")
            .command("kill @e[type=!player]")
            .command("fill -16 99 -16 16 99 16 polished_blackstone_bricks")
            .command("fill -16 100 -16 16 115 16 air")
            .command("fill -6 99 -6 6 99 6 smooth_quartz")
            .command("fill -16 100 16 16 104 16 polished_blackstone_bricks")
            .command("fill -16 105 16 16 105 16 timemod:brass_block");
        for (int x = -14; x <= 14; x += 7) {
            d.command("setblock " + x + " 100 15 lantern");
        }
        // Der Uhrmacher und sein Ziel
        d.command("summon villager 0 100 -6 {NoAI:1b,Invulnerable:1b,Silent:1b}").wait(2)
            .server(player -> {
                ServerLevel level = player.serverLevel();
                ClockmakerBoss boss = ModEntities.CLOCKMAKER.create(level);
                boss.moveTo(0.5, 100, 6.5, 180, 0);
                level.addFreshEntity(boss);
                target(level, boss);
            })
            .wait(30).server(player -> focus(player, ClockmakerBoss.class, 9.0, 3.5, 35.0))
            .wait(40).screenshot("b1_uhrmacher")
            .record("seq_b_uhrmacher", 1).wait(70).stopRecording()
            // Zeitlupe auf ihn: die Brustklappe öffnet sich
            .server(player -> find(player, ClockmakerBoss.class).ifPresent(boss -> {
                boss.setNoAi(true);
                ServerTimeEngine.of(player.serverLevel()).addZone(new ZoneShape.Sphere(boss.position().add(0, 1.5, 0), 3.5), 0.5,
                    TimeFilter.DEFAULT, null, 400);
            }))
            .wait(30).server(player -> focus(player, ClockmakerBoss.class, 4.5, 1.6, 0.0))
            .wait(10).screenshot("b2_schwachstelle")
            .command("chrono clear")
            .command("kill @e[type=timemod:clockmaker]").command("kill @e[type=timemod:gear_saw]").wait(10);

        // Der Chronophage
        d.server(player -> {
            ServerLevel level = player.serverLevel();
            ChronophageBoss boss = ModEntities.CHRONOPHAGE.create(level);
            boss.moveTo(0.5, 103, 6.5, 180, 0);
            level.addFreshEntity(boss);
            target(level, boss);
            boss.setMouth(ChronophageBoss.Mouth.CLOSED, 90);
        }).wait(30).server(player -> focus(player, ChronophageBoss.class, 10.0, 2.0, 30.0))
            .wait(20).screenshot("b3_chronophage")
            .record("seq_b_chronophage", 1).wait(90).stopRecording()
            .server(player -> find(player, ChronophageBoss.class).ifPresent(boss -> {
                boss.setNoAi(true);
                boss.setMouth(ChronophageBoss.Mouth.OPEN, 400);
            }))
            .wait(25).server(player -> focus(player, ChronophageBoss.class, 7.0, 1.0, 0.0))
            .wait(10).screenshot("b4_maul_offen")
            // „Im richtigen Moment die Zeit anhalten“
            .server(player -> find(player, ChronophageBoss.class).ifPresent(boss ->
                ServerTimeEngine.of(player.serverLevel()).addZone(new ZoneShape.Sphere(boss.position(), 12), 0.0,
                    TimeFilter.DEFAULT | TimeFilter.PLAYERS, player.getUUID(), 200)))
            .wait(30).screenshot("b5_zeitstopp")
            .server(player -> focus(player, ChronophageBoss.class, 5.0, 0.6, -25.0))
            .wait(10).screenshot("b6_eingefroren");
        d.wait(5).quit();
    }

    private static <T extends Mob> java.util.Optional<T> find(ServerPlayer player, Class<T> type) {
        return player.serverLevel().getEntitiesOfClass(type, new AABB(player.blockPosition()).inflate(64)).stream().findFirst();
    }

    /** Kamera vor den Boss stellen (Abstand, Höhe, seitlicher Winkel) und auf ihn richten. */
    private static void focus(ServerPlayer player, Class<? extends Mob> type, double distance, double height, double sideDegrees) {
        find(player, type).ifPresent(boss -> {
            Vec3 forward = Vec3.directionFromRotation(0, boss.getYRot()).yRot((float) Math.toRadians(sideDegrees));
            Vec3 target = boss.position().add(0, boss.getBbHeight() * 0.6, 0);
            Vec3 eye = target.add(forward.scale(distance)).add(0, height, 0);
            Vec3 dir = target.subtract(eye);
            float yaw = (float) (Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90.0);
            float pitch = (float) -Math.toDegrees(Math.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z)));
            player.teleportTo(player.serverLevel(), eye.x, eye.y - player.getEyeHeight(), eye.z, yaw, pitch);
        });
    }

    private static void target(ServerLevel level, Mob boss) {
        level.getEntitiesOfClass(Villager.class, new AABB(boss.blockPosition()).inflate(40)).stream().findFirst().ifPresent(boss::setTarget);
    }
}
