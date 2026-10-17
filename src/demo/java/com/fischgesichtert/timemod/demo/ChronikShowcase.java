package com.fischgesichtert.timemod.demo;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.client.screen.ChronikScreen;
import net.minecraft.advancements.Advancement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import org.lwjgl.glfw.GLFW;

/** Vorführung: Handbuch „Die Chronik“ und der Advancement-Baum von TIME. */
final class ChronikShowcase {
    private ChronikShowcase() {
    }

    static void write(DemoDirector d) {
        d.wait(20)
            .command("gamerule sendCommandFeedback false")
            .command("gamerule announceAdvancements false")
            .command("time set 6000")
            .command("tp @a 0 120 0 0 20")
            .wait(40)
            .client(mc -> {
                mc.options.hideGui = false;
                mc.setScreen(new ChronikScreen());
            }).wait(10).client(mc -> mc.getToasts().clear()).screenshot("c1_inhalt");
        // Durch einige Kapitel blättern
        for (int i = 0; i < 14; i++) {
            d.client(mc -> press(mc, GLFW.GLFW_KEY_RIGHT)).wait(2);
            if (i == 0 || i == 5 || i == 9 || i == 12) {
                d.wait(4).screenshot("c2_seite_" + i);
            }
        }
        d.client(mc -> mc.setScreen(null))
            .command("advancement grant @a until timemod:module")
            .command("advancement grant @a until timemod:rewind")
            .command("advancement grant @a only timemod:observatory")
            .wait(20).client(mc -> {
                mc.getToasts().clear();
                AdvancementsScreen screen = new AdvancementsScreen(mc.player.connection.getAdvancements());
                mc.setScreen(screen);
                Advancement root = mc.player.connection.getAdvancements().getAdvancements().get(TimeMod.id("root"));
                if (root != null) {
                    mc.player.connection.getAdvancements().setSelectedTab(root, true);
                }
            }).wait(10).client(mc -> mc.getToasts().clear()).screenshot("c3_advancements");
        d.wait(5).quit();
    }

    private static void press(Minecraft mc, int key) {
        if (mc.screen != null) {
            mc.screen.keyPressed(key, 0, 0);
        }
    }
}
