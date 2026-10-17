package com.fischgesichtert.timemod.demo;

import com.fischgesichtert.timemod.block.entity.ChronosConsoleBlockEntity;
import com.fischgesichtert.timemod.gate.ChronosGate;
import com.fischgesichtert.timemod.registry.ModBlocks;
import com.fischgesichtert.timemod.registry.ModItems;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/** Vorführung des Chronos-Tors: Konsole, Aufladen mit rasenden Ringen, offenes Portal und eine Reise in die Kreidezeit. */
final class GateShowcase {
    private static final BlockPos CORNER = new BlockPos(-3, 100, 0);
    private static final BlockPos CONSOLE = new BlockPos(0, 100, -4);

    private GateShowcase() {
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
            .command("fill -16 98 -16 16 98 16 stone_bricks")
            .command("fill -16 99 -16 16 99 16 polished_andesite")
            .command("fill -16 100 -16 16 112 16 air")
            .command("setblock 0 100 -4 timemod:chronos_console[facing=north]")
            .server(player -> {
                ServerLevel level = player.serverLevel();
                ChronosGate.Shape gate = new ChronosGate.Shape(CORNER, Direction.Axis.X);
                for (BlockPos pos : gate.frame()) {
                    level.setBlockAndUpdate(pos, ModBlocks.CHRONOS_GATE_FRAME.defaultBlockState());
                }
                for (int x : new int[] {-6, 6}) {
                    level.setBlockAndUpdate(new BlockPos(x, 100, -2), net.minecraft.world.level.block.Blocks.LANTERN.defaultBlockState());
                }
            })
            .wait(40)
            .server(player -> {
                if (player.serverLevel().getBlockEntity(CONSOLE) instanceof ChronosConsoleBlockEntity console) {
                    console.items().setItem(ChronosConsoleBlockEntity.SLOT_CELL, cell());
                    console.items().setItem(ChronosConsoleBlockEntity.SLOT_FOCUS, new ItemStack(ModItems.MESOZOIC_FOCUS));
                    for (int i = 0; i < 40 && console.target().years() != -66_000_000; i++) {
                        console.select(1);
                    }
                }
            })
            .wait(40)
            // Vor die Konsole treten und sie öffnen
            .command("gamemode creative @a")
            .command("tp @a 0.5 100 -6.5 0 40")
            .wait(30)
            .server(player -> {
                if (player.serverLevel().getBlockEntity(CONSOLE) instanceof ChronosConsoleBlockEntity console) {
                    console.openMenu(player);
                }
            })
            .client(mc -> mc.options.hideGui = false)
            .wait(30).screenshot("g1_konsole")
            .client(mc -> {
                if (mc.player != null) {
                    mc.player.closeContainer();
                }
                mc.options.hideGui = true;
            })
            .wait(10)
            .command("gamemode spectator @a")
            .command("tp @a 8.5 104 -10.5 38.7 9.5")
            .wait(40)
            .server(player -> {
                if (player.serverLevel().getBlockEntity(CONSOLE) instanceof ChronosConsoleBlockEntity console) {
                    player.setGameMode(GameType.CREATIVE);
                    console.charge(player);
                    player.setGameMode(GameType.SPECTATOR);
                }
            })
            .record("seq_gate", 3)
            .wait(ChronosConsoleBlockEntity.CHARGE_TICKS - 40).screenshot("g2_ringe_rasen")
            .wait(70)
            .stopRecording()
            .screenshot("g3_portal_offen")
            // Hindurchgehen
            .command("gamemode creative @a")
            .command("tp @a 0.5 101 -2.5 0 0")
            .client(mc -> {
                mc.options.hideGui = false;
                mc.options.setCameraType(CameraType.FIRST_PERSON);
            })
            .wait(10)
            .client(mc -> mc.options.keyUp.setDown(true))
            .wait(25)
            .client(mc -> mc.options.keyUp.setDown(false))
            .wait(15).screenshot("g4_ankunft_titel")
            .wait(200).screenshot("g5_kreidezeit");
        d.wait(5).quit();
    }

    private static ItemStack cell() {
        ItemStack cell = new ItemStack(ModItems.INFINITY_TIME_CELL);
        com.fischgesichtert.timemod.energy.Chronon.set(cell, 600_000);
        return cell;
    }
}
