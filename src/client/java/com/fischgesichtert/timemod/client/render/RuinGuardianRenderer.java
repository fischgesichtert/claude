package com.fischgesichtert.timemod.client.render;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.client.model.RuinGuardianModel;
import com.fischgesichtert.timemod.entity.RuinGuardian;
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

/** Der Ruinenwächter; sein Linsenauge glüht bernsteinfarben, sobald er erwacht ist. */
public class RuinGuardianRenderer extends MobRenderer<RuinGuardian, RuinGuardianModel> {
    private static final ResourceLocation TEXTURE = TimeMod.id("textures/entity/ruin_guardian.png");
    private static final RenderType GLOW = RenderType.eyes(TimeMod.id("textures/entity/ruin_guardian_glow.png"));

    public RuinGuardianRenderer(EntityRendererProvider.Context context) {
        super(context, new RuinGuardianModel(context.bakeLayer(RuinGuardianModel.LAYER)), 1.0F);
        addLayer(new EyeLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(RuinGuardian guardian) {
        return TEXTURE;
    }

    /** Das Auge leuchtet mit dem Erwachen auf (im Schlaf dunkel). */
    private static final class EyeLayer extends RenderLayer<RuinGuardian, RuinGuardianModel> {
        EyeLayer(RenderLayerParent<RuinGuardian, RuinGuardianModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, RuinGuardian guardian, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            float open = Mth.lerp(partialTick, guardian.eyeOpenO, guardian.eyeOpen);
            if (open < 0.05F) {
                return;
            }
            float flicker = 0.85F + 0.15F * Mth.sin(ageInTicks * 0.3F);
            float b = open * flicker;
            getParentModel().renderToBuffer(pose, buffers.getBuffer(GLOW), 0xF00000, OverlayTexture.NO_OVERLAY, b, b, b, 1.0F);
        }
    }
}
