package com.fischgesichtert.timemod.client;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.mixin.client.PoseStackAccessor;
import com.fischgesichtert.timemod.safety.Safety;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Absturzschutz im Client: Was TIME zeichnet (Wesen, Maschinen, HUD, Effekte) oder als Bildschirm
 * zeigt, darf das Spiel nicht beenden. Scheitert es, fehlt es in diesem Bild; der Matrix-Stapel und
 * die wichtigsten Zeichen-Zustände werden wiederhergestellt, damit der Rest des Bildes stimmt.
 */
public final class ClientSafety {
    private static final String PACKAGE = "com.fischgesichtert.timemod.";

    private ClientSafety() {
    }

    public static int depth(PoseStack pose) {
        return ((PoseStackAccessor) pose).timemod$poses().size();
    }

    /** Entfernt alles, was ein abgebrochenes Zeichnen auf dem Matrix-Stapel liegen gelassen hat. */
    public static void restore(PoseStack pose, int depth) {
        while (depth(pose) > Math.max(1, depth)) {
            pose.popPose();
        }
    }

    /** Setzt Zustände zurück, die ein abgebrochenes Zeichnen verstellt haben könnte. */
    private static void resetRenderState() {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableScissor();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    /** Zeichnet etwas aus TIME (HUD, Effekt in der Welt). Scheitert es, fehlt es nur in diesem Bild. */
    public static void render(String where, PoseStack pose, Runnable render) {
        int depth = depth(pose);
        try {
            render.run();
        } catch (Throwable error) {
            Safety.report(where, error);
            restore(pose, depth);
            Safety.run("Absturzschutz: Zeichen-Zustand", () -> {
                resetRenderState();
                Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
            });
        }
    }

    /** Ist das Zeichnen einer Entity an TIME gescheitert? Aufruf direkt aus dem fangenden Mixin. */
    public static boolean absorbEntityRender(Object renderer, Entity entity, Throwable error) {
        boolean own = renderer.getClass().getName().startsWith(PACKAGE)
            || TimeMod.MOD_ID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace());
        if (!own && !Safety.blames(error, 1)) {
            return false;
        }
        Safety.report("Zeichnen: " + BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()), error);
        return true;
    }

    /** Ist das Zeichnen einer Block-Entity an TIME gescheitert? Aufruf direkt aus dem fangenden Mixin. */
    public static boolean absorbBlockEntityRender(Object renderer, BlockEntity blockEntity, Throwable error) {
        boolean own = renderer.getClass().getName().startsWith(PACKAGE)
            || TimeMod.MOD_ID.equals(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()).getNamespace());
        if (!own && !Safety.blames(error, 1)) {
            return false;
        }
        Safety.report("Zeichnen: " + BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()), error);
        return true;
    }

    /**
     * Ein Bildschirm ist gescheitert (Zeichnen, Klick, Taste). Ein Bildschirm von TIME schließt sich dann
     * mit einem Hinweis; bei einem fremden Bildschirm fällt nur dieser eine Aufruf aus, wenn TIME schuld war.
     * Aufruf direkt aus dem fangenden Mixin.
     */
    public static boolean absorbScreen(String action, String screenName, Throwable error) {
        boolean own = screenName != null && screenName.startsWith(PACKAGE);
        if (!own && !Safety.blames(error, 1)) {
            return false;
        }
        Safety.report("Bildschirm " + screenName + " (" + action + ")", error);
        Safety.run("Absturzschutz: Zeichen-Zustand", ClientSafety::resetRenderState);
        if (own) {
            Minecraft minecraft = Minecraft.getInstance();
            Screen failed = minecraft.screen;
            minecraft.tell(() -> {
                if (minecraft.screen == failed) {
                    minecraft.setScreen(null);
                    if (minecraft.player != null) {
                        minecraft.player.displayClientMessage(Component.translatable("timemod.safety.screen_closed"), false);
                    }
                }
            });
        }
        return true;
    }

    /** Eine Benutzung (Rechtsklick, Angriff) ist an TIME gescheitert. Aufruf direkt aus dem fangenden Mixin. */
    public static boolean absorbUse(String action, Throwable error) {
        if (!Safety.blames(error, 1)) {
            return false;
        }
        Safety.report(action, error);
        return true;
    }
}
