package com.fischgesichtert.timemod.client.render;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.client.model.ChronophageModel;
import com.fischgesichtert.timemod.entity.boss.ChronophageBoss;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Der Chronophage: schwebt, wiegt sich und leuchtet im Rachen. */
public class ChronophageRenderer extends MobRenderer<ChronophageBoss, ChronophageModel> {
    private static final ResourceLocation TEXTURE = TimeMod.id("textures/entity/chronophage.png");
    private static final RenderType GLOW = RenderType.eyes(TimeMod.id("textures/entity/chronophage_glow.png"));

    public ChronophageRenderer(EntityRendererProvider.Context context) {
        super(context, new ChronophageModel(context.bakeLayer(ChronophageModel.LAYER)), 1.4F);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    protected void scale(ChronophageBoss boss, PoseStack pose, float partialTick) {
        float bob = Mth.sin((boss.tickCount + partialTick) * 0.08F) * 0.12F;
        pose.translate(0, bob, 0);
        pose.scale(1.15F, 1.15F, 1.15F);
    }

    @Override
    public ResourceLocation getTextureLocation(ChronophageBoss boss) {
        return TEXTURE;
    }
}
