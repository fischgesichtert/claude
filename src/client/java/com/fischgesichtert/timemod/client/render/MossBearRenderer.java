package com.fischgesichtert.timemod.client.render;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.entity.MossBear;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PolarBearModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Der Moosbär im Körperbau des Eisbären – mit grünem Rücken oder, frisch geschoren, kahlem Fell. */
public class MossBearRenderer extends MobRenderer<MossBear, PolarBearModel<MossBear>> {
    private static final ResourceLocation MOSSY = TimeMod.id("textures/entity/moss_bear.png");
    private static final ResourceLocation SHEARED = TimeMod.id("textures/entity/moss_bear_sheared.png");

    public MossBearRenderer(EntityRendererProvider.Context context) {
        super(context, new PolarBearModel<>(context.bakeLayer(ModelLayers.POLAR_BEAR)), 0.9F);
    }

    @Override
    public ResourceLocation getTextureLocation(MossBear bear) {
        return bear.isSheared() ? SHEARED : MOSSY;
    }

    @Override
    protected void scale(MossBear bear, PoseStack pose, float partialTick) {
        pose.scale(1.2F, 1.2F, 1.2F);
        super.scale(bear, pose, partialTick);
    }
}
