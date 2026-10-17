package com.fischgesichtert.timemod.client.render;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.client.model.CityColossusModel;
import com.fischgesichtert.timemod.entity.boss.CityColossus;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Der Wächter der letzten Stadt. Beim Erwachen steigt er aus dem Boden; sein Turmfenster und die Uhr auf
 * der Brust leuchten, in der zweiten Phase (ohne Kupferplatten) glüht auch das Uhrwerk in seinem Rumpf.
 */
public class CityColossusRenderer extends MobRenderer<CityColossus, CityColossusModel> {
    private static final ResourceLocation TEXTURE = TimeMod.id("textures/entity/city_colossus.png");
    private static final ResourceLocation BARE = TimeMod.id("textures/entity/city_colossus_bare.png");
    private static final RenderType GLOW = RenderType.eyes(TimeMod.id("textures/entity/city_colossus_glow.png"));
    private static final RenderType GLOW_BARE = RenderType.eyes(TimeMod.id("textures/entity/city_colossus_bare_glow.png"));

    public CityColossusRenderer(EntityRendererProvider.Context context) {
        super(context, new CityColossusModel(context.bakeLayer(CityColossusModel.LAYER)), 2.0F);
        addLayer(new GlowLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(CityColossus colossus) {
        return colossus.isBare() ? BARE : TEXTURE;
    }

    @Override
    protected void setupRotations(CityColossus colossus, PoseStack pose, float ageInTicks, float yaw, float partialTick) {
        super.setupRotations(colossus, pose, ageInTicks, yaw, partialTick);
        int emerge = colossus.emergeTicks();
        if (emerge > 0) {
            // Aus dem Boden steigen: anfangs ganz unten, mit leichtem Zittern
            float left = Math.max(0.0F, emerge - partialTick) / CityColossus.EMERGE_TICKS;
            float shake = Mth.sin(ageInTicks * 2.3F) * 0.04F * left;
            pose.translate(shake, -colossus.getBbHeight() * left * left, 0.0F);
        }
    }

    private static final class GlowLayer extends RenderLayer<CityColossus, CityColossusModel> {
        GlowLayer(RenderLayerParent<CityColossus, CityColossusModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, CityColossus colossus, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            float pulse = colossus.breathTicks() > 0 ? 1.0F : 0.8F + 0.2F * Mth.sin(ageInTicks * 0.15F);
            getParentModel().renderToBuffer(pose, buffers.getBuffer(colossus.isBare() ? GLOW_BARE : GLOW), 0xF00000, OverlayTexture.NO_OVERLAY,
                pulse, pulse, pulse, 1.0F);
        }
    }
}
