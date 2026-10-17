package com.fischgesichtert.timemod.client.render;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.entity.boss.ParadoxBoss;
import com.fischgesichtert.timemod.entity.boss.ParadoxEcho;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Paradox und seine Echos sehen aus wie der Spieler, dessen Echo sie sind: sein Skin, sein Modell
 * (breit oder schmal), seine kopierte Rüstung und Waffe. Dazu leuchtende Augen, ein Zifferblatt auf
 * der Brust und – bei Paradox selbst – Nachbilder, wenn es sich bewegt. Echos sind durchscheinend.
 */
public class ParadoxRenderer<T extends Monster> extends HumanoidMobRenderer<T, PlayerModel<T>> {
    private static final ResourceLocation GLOW = TimeMod.id("textures/entity/paradox_glow.png");
    private static final RenderType GLOW_TYPE = RenderType.eyes(GLOW);
    private static final int SELF_LIGHT = 11;
    private final PlayerModel<T> wide;
    private final PlayerModel<T> slim;
    private final boolean echo;

    public ParadoxRenderer(EntityRendererProvider.Context context, boolean echo) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.wide = getModel();
        this.slim = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        this.echo = echo;
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
            new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW_TYPE;
            }
        });
    }

    @Nullable
    private static UUID echoOf(Monster entity) {
        if (entity instanceof ParadoxBoss boss) {
            return boss.echoOf();
        }
        return entity instanceof ParadoxEcho echo ? echo.echoOf() : null;
    }

    @Nullable
    private static PlayerInfo info(@Nullable UUID uuid) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return uuid == null || connection == null ? null : connection.getPlayerInfo(uuid);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        UUID uuid = echoOf(entity);
        PlayerInfo info = info(uuid);
        if (info != null) {
            return info.getSkinLocation();
        }
        return uuid != null ? DefaultPlayerSkin.getDefaultSkin(uuid) : DefaultPlayerSkin.getDefaultSkin();
    }

    private static boolean slim(Monster entity) {
        UUID uuid = echoOf(entity);
        PlayerInfo info = info(uuid);
        if (info != null) {
            return "slim".equals(info.getModelName());
        }
        return uuid != null && "slim".equals(DefaultPlayerSkin.getSkinModelName(uuid));
    }

    @Override
    @Nullable
    protected RenderType getRenderType(T entity, boolean bodyVisible, boolean translucent, boolean glowing) {
        if (echo && bodyVisible) {
            return RenderType.entityTranslucent(getTextureLocation(entity));
        }
        return super.getRenderType(entity, bodyVisible, translucent, glowing);
    }

    @Override
    public void render(T entity, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
        // Ein Echo aus Zeit leuchtet ein wenig von selbst – auch im dunklen End sichtbar
        light = LightTexture.pack(Math.max(LightTexture.block(light), SELF_LIGHT), LightTexture.sky(light));
        model = slim(entity) ? slim : wide;
        model.rightArmPose = entity.getMainHandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        model.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        super.render(entity, yaw, partialTicks, pose, buffers, light);
        if (entity instanceof ParadoxBoss boss) {
            afterimages(boss, partialTicks, pose, buffers, light);
        }
    }

    /** Drei verblassende Nachbilder an früheren Positionen (nur in Bewegung). */
    private void afterimages(ParadoxBoss boss, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
        Vec3 now = boss.getPosition(partialTicks);
        float bodyYaw = Mth.rotLerp(partialTicks, boss.yBodyRotO, boss.yBodyRot);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(getTextureLocation((T) boss)));
        for (int i = 1; i <= 3; i++) {
            Vec3 past = boss.trail[Math.floorMod(boss.trailHead - 1 - i * 2, boss.trail.length)];
            if (past == null) {
                continue;
            }
            Vec3 offset = past.subtract(now);
            if (offset.lengthSqr() < 0.05) {
                continue;
            }
            pose.pushPose();
            pose.translate(offset.x, offset.y, offset.z);
            pose.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
            pose.scale(-1.0F, -1.0F, 1.0F);
            pose.translate(0.0F, -1.501F, 0.0F);
            model.renderToBuffer(pose, consumer, light, OverlayTexture.NO_OVERLAY, 0.55F, 0.4F, 1.0F, 0.34F / i);
            pose.popPose();
        }
    }
}
