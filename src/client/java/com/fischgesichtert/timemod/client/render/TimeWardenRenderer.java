package com.fischgesichtert.timemod.client.render;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.client.model.TimeWardenModel;
import com.fischgesichtert.timemod.entity.TimeWarden;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Der Zeitwächter mit leuchtendem Zifferblatt und Heiligenschein. Beim Erscheinen entfaltet er sich
 * aus einem senkrechten Lichtstrich, beim Verschwinden zieht er sich wieder dazu zusammen.
 */
public class TimeWardenRenderer extends MobRenderer<TimeWarden, TimeWardenModel> {
    private static final ResourceLocation TEXTURE = TimeMod.id("textures/entity/time_warden.png");
    private static final RenderType GLOW = RenderType.eyes(TimeMod.id("textures/entity/time_warden_glow.png"));

    public TimeWardenRenderer(EntityRendererProvider.Context context) {
        super(context, new TimeWardenModel(context.bakeLayer(TimeWardenModel.LAYER)), 0.5F);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    protected void scale(TimeWarden warden, PoseStack pose, float partialTick) {
        float presence = warden.presence(partialTick);
        if (presence < 1.0F) {
            float width = 0.03F + 0.97F * presence * presence;
            pose.scale(width, 1.0F + (1.0F - presence) * 0.2F, width);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(TimeWarden warden) {
        return TEXTURE;
    }
}
