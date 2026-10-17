package com.fischgesichtert.timemod.demo;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spielt ein festes Drehbuch im echten Client ab: baut eine Bühne, schaltet Zeitzonen und nimmt
 * Einzelbilder auf (daraus werden später GIFs/Videos). Beendet das Spiel am Ende selbst.
 */
public class DemoDirector implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("TIME-Demo");

    private final List<Step> script = new ArrayList<>();
    private int tick;
    private int cursor;
    private boolean started;
    private boolean creatingWorld;
    private String recording;
    private int recordEvery;
    private int recordFrame;

    private record Step(int atTick, Runnable action) {
    }

    @Override
    public void onInitializeClient() {
        DemoScript.write(this);
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    // ------------------------------------------------------------------ Drehbuch-Bausteine

    private int time;

    public DemoDirector wait(int ticks) {
        time += ticks;
        return this;
    }

    public DemoDirector at(Runnable action) {
        script.add(new Step(time, action));
        return this;
    }

    public DemoDirector command(String command) {
        return at(() -> runCommand(command));
    }

    public DemoDirector screenshot(String name) {
        return at(() -> grab(name));
    }

    /** Nimmt ab jetzt alle {@code every} Ticks ein Bild für die Sequenz {@code name} auf. */
    public DemoDirector record(String name, int every) {
        return at(() -> {
            recording = name;
            recordEvery = every;
            recordFrame = 0;
        });
    }

    public DemoDirector stopRecording() {
        return at(() -> recording = null);
    }

    public DemoDirector quit() {
        return at(() -> {
            LOGGER.info("Demo fertig");
            Minecraft.getInstance().stop();
        });
    }

    // ------------------------------------------------------------------ Ablauf

    private void onTick(Minecraft mc) {
        if (mc.level == null && !creatingWorld && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
            creatingWorld = true;
            createWorld(mc);
            return;
        }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) {
            return;
        }
        if (!started) {
            started = true;
            LOGGER.info("Demo startet ({} Schritte)", script.size());
            mc.options.hideGui = true;
        }
        tick++;
        while (cursor < script.size() && script.get(cursor).atTick() <= tick) {
            script.get(cursor++).action().run();
        }
        if (recording != null && tick % recordEvery == 0) {
            grab(String.format(Locale.ROOT, "%s/%04d", recording, recordFrame++));
        }
    }

    /** Legt bei jedem Lauf eine frische Welt mit festem Seed an (Kreativ, friedlich, Befehle erlaubt). */
    private static void createWorld(Minecraft mc) {
        String name = "TimeDemo-" + System.currentTimeMillis();
        LOGGER.info("Erzeuge Demo-Welt {}", name);
        LevelSettings settings = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(name, settings, new WorldOptions(20261017L, false, false), WorldPresets::createNormalWorldDimensions);
    }

    private static void runCommand(String command) {
        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            LOGGER.info("Befehl: /{}", command);
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
        });
    }

    private static void grab(String name) {
        Minecraft mc = Minecraft.getInstance();
        try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            Path file = mc.gameDirectory.toPath().resolve("demo-frames").resolve(name + ".png");
            Files.createDirectories(file.getParent());
            image.writeToFile(file);
        } catch (IOException e) {
            LOGGER.error("Bild {} konnte nicht gespeichert werden", name, e);
        }
    }

    static File dir() {
        return Minecraft.getInstance().gameDirectory;
    }
}
