package com.fischgesichtert.timemod.demo;

import com.fischgesichtert.timemod.entity.boss.ParadoxBoss;
import com.fischgesichtert.timemod.registry.ModBlocks;
import com.fischgesichtert.timemod.registry.ModEntities;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Vorführung: die Zeitlose Zitadelle im End und Paradox, das Echo des Spielers. */
final class ParadoxShowcase {
    /** Chunk-Ecke, in der die Zitadelle entsteht (ihre Höhe würfelt die Struktur selbst). */
    private static final int X = 1200;
    private static final int Z = 1200;
    /** Mittelpunkt der Arena auf Höhe des Altars – wird nach dem Platzieren gesucht. */
    private static BlockPos altar = new BlockPos(X + 20, 121, Z + 20);

    private ParadoxShowcase() {
    }

    static void write(DemoDirector d) {
        d.wait(20)
            .command("gamemode spectator @a")
            .command("gamerule sendCommandFeedback false")
            .command("gamerule announceAdvancements false")
            .command("execute in minecraft:the_end run forceload add " + (X - 16) + " " + (Z - 16) + " " + (X + 56) + " " + (Z + 56))
            .command("execute in minecraft:the_end run tp @a " + (X + 20) + " 140 " + (Z - 40) + " 0 20")
            .wait(80)
            .command("execute in minecraft:the_end run place structure timemod:timeless_citadel " + (X + 8) + " 80 " + (Z + 8))
            .wait(40)
            .server(ParadoxShowcase::findAltar)
            .client(mc -> {
                mc.options.hideGui = true;
                mc.getToasts().clear();
                mc.gui.getChat().clearMessages(false);
            });
        // Anflug von schräg oben, dann von der Seite
        d.server(p -> look(p, -34, 12, -48, -35, 22)).wait(40).screenshot("p1_zitadelle")
            .server(p -> look(p, 30, -12, 34, 140, 15)).wait(30).screenshot("p2_zitadelle_seite")
            // Auf dem Platz: das erloschene Tor zum Zeitstrom
            .server(p -> look(p, -3, -33, -19, -10, -4)).wait(30).screenshot("p3_platz_tor")
            // Im Turm: Wendeltreppe
            .server(p -> look(p, 2.6, -33, -1.4, 160, -62)).wait(20).screenshot("p4_wendeltreppe")
            // Oben: Arena mit Spiegel-Altar
            .server(p -> look(p, 0.5, 2.5, -11, 0, 15)).wait(30).screenshot("p5_arena");

        // Paradox: der Spieler in Chrono-Rüstung (drei Teile – so friert er ein) mit Diamantschwert
        d.command("gamemode survival @a")
            .command("difficulty hard")
            .command("effect give @a resistance infinite 4 true")
            .command("effect give @a regeneration infinite 3 true")
            .command("item replace entity @a armor.chest with timemod:chrono_chestplate")
            .command("item replace entity @a armor.legs with timemod:chrono_leggings")
            .command("item replace entity @a armor.feet with timemod:chrono_boots")
            .command("item replace entity @a weapon.mainhand with minecraft:diamond_sword")
            .server(p -> look(p, -2.0, 0, -6.0, -28, 6))
            .wait(10)
            .server(player -> {
                ServerLevel level = player.serverLevel();
                ParadoxBoss boss = ModEntities.PARADOX.create(level);
                boss.moveTo(altar.getX() + 3.5, altar.getY(), altar.getZ() + 1.5, 150, 0);
                boss.becomeEchoOf(player);
                boss.setNoAi(true);
                level.addFreshEntity(boss);
            })
            .client(mc -> mc.options.hideGui = false)
            .wait(30).client(mc -> mc.gui.getChat().clearMessages(false)).screenshot("p6_paradox_echo");

        // „Die Zeit gehört mir“: Stillstand und Messerkranz
        d.record("seq_p_messerkranz", 1)
            .server(player -> find(player).ifPresent(boss -> {
                boss.setTarget(player);
                boss.timeStop();
            }))
            .client(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK))
            .wait(6)
            .server(player -> find(player).ifPresent(boss -> boss.knifeRing(player, 8)))
            .wait(20).screenshot("p7_messerkranz")
            .wait(50)
            .stopRecording()
            .client(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));

        // Zweite Phase: Echos
        d.wait(20)
            .server(player -> find(player).ifPresent(boss -> {
                boss.setNoAi(false);
                boss.setHealth(boss.getMaxHealth() * 0.45F);
            }))
            .wait(10)
            .server(player -> find(player).ifPresent(boss -> boss.setNoAi(true)))
            .server(p -> look(p, -2.0, 0, -7.0, -20, 8))
            .wait(30).client(mc -> mc.gui.getChat().clearMessages(false)).screenshot("p8_echos");
        d.wait(10).quit();
    }

    /** Sucht den Spiegel-Altar der eben platzierten Zitadelle. */
    private static void findAltar(ServerPlayer player) {
        ServerLevel end = player.server.getLevel(Level.END);
        if (end == null) {
            return;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = 100; y < 150; y++) {
            for (int x = X + 10; x < X + 30; x++) {
                for (int z = Z + 10; z < Z + 30; z++) {
                    if (end.getBlockState(pos.set(x, y, z)).is(ModBlocks.PARADOX_ALTAR)) {
                        altar = pos.immutable();
                        return;
                    }
                }
            }
        }
    }

    /** Kamera relativ zum Altar setzen. */
    private static void look(ServerPlayer player, double dx, double dy, double dz, float yaw, float pitch) {
        ServerLevel end = player.server.getLevel(Level.END);
        if (end != null) {
            player.teleportTo(end, altar.getX() + 0.5 + dx, altar.getY() + dy, altar.getZ() + 0.5 + dz, yaw, pitch);
        }
    }

    private static Optional<ParadoxBoss> find(ServerPlayer player) {
        return player.serverLevel().getEntitiesOfClass(ParadoxBoss.class, new AABB(player.blockPosition()).inflate(48)).stream().findFirst();
    }
}
