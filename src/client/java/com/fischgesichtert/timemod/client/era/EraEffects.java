package com.fischgesichtert.timemod.client.era;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.era.Era;
import com.fischgesichtert.timemod.era.Eras;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;

/**
 * Himmel und Luft der Epochen: dieselbe Optik wie in der Oberwelt, aber getönt nach der Stimmung der
 * Epoche – blauweißer Schneedunst in der Eiszeit, Bernstein in der Kreide, grüner Dunst im Jura,
 * rötliche Hitze in der Trias.
 */
public class EraEffects extends DimensionSpecialEffects {
    public EraEffects(float cloudLevel) {
        super(cloudLevel, true, SkyType.NORMAL, false, false);
    }

    public static void init() {
        // Tiefe, schwere Wolken in der Eiszeit, hohe über den Urzeit-Gebirgen
        DimensionRenderingRegistry.registerDimensionEffects(TimeMod.id("ice_age"), new EraEffects(168.0F));
        DimensionRenderingRegistry.registerDimensionEffects(TimeMod.id("steppe"), new EraEffects(192.0F));
        DimensionRenderingRegistry.registerDimensionEffects(TimeMod.id("deep_time"), new EraEffects(224.0F));
    }

    /** Die Stimmung der Welt, in der man gerade ist. */
    public static Era.Mood currentMood() {
        ClientLevel level = Minecraft.getInstance().level;
        Era era = level == null ? null : Eras.byLevel(level.dimension());
        return era == null ? Era.Mood.CLEAR : era.mood();
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
        Vec3 base = color.multiply(brightness * 0.94F + 0.06F, brightness * 0.94F + 0.06F, brightness * 0.91F + 0.09F);
        Era.Mood mood = currentMood();
        if (mood.strength() <= 0.0F) {
            return base;
        }
        Vec3 tint = Vec3.fromRGB24(mood.tint()).scale(brightness * 0.94F + 0.06F);
        return base.lerp(tint, mood.strength());
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    /** Nach dem Vanilla-Nebel: In dunstigen Epochen beginnt der Geländenebel näher. */
    public static void applyHaze(Camera camera, FogRenderer.FogMode mode, float renderDistance, boolean thickFog) {
        if (mode != FogRenderer.FogMode.FOG_TERRAIN || thickFog || camera.getFluidInCamera() != FogType.NONE) {
            return;
        }
        if (camera.getEntity() instanceof LivingEntity living && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS))) {
            return;
        }
        Era.Mood mood = currentMood();
        if (mood.hazeStart() >= 1.0F) {
            return;
        }
        float start = renderDistance * mood.hazeStart();
        if (start < RenderSystem.getShaderFogStart()) {
            RenderSystem.setShaderFogStart(start);
        }
    }
}
