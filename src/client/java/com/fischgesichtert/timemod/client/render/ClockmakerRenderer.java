package com.fischgesichtert.timemod.client.render;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.client.model.ClockmakerModel;
import com.fischgesichtert.timemod.entity.boss.ClockmakerBoss;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** Der Uhrmacher mit leuchtendem Monokel und Uhrwerk-Kern. */
public class ClockmakerRenderer extends MobRenderer<ClockmakerBoss, ClockmakerModel> {
    private static final ResourceLocation TEXTURE = TimeMod.id("textures/entity/clockmaker.png");
    private static final RenderType GLOW = RenderType.eyes(TimeMod.id("textures/entity/clockmaker_glow.png"));

    public ClockmakerRenderer(EntityRendererProvider.Context context) {
        super(context, new ClockmakerModel(context.bakeLayer(ClockmakerModel.LAYER)), 1.1F);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(ClockmakerBoss boss) {
        return TEXTURE;
    }
}
