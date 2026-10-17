package com.fischgesichtert.timemod.client.model;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.entity.boss.CityColossus;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Der Wächter der letzten Stadt: ein Rumpf aus Kalkstein mit der Uhr der Stadt auf der Brust und
 * Schulterplatten aus grünem Kupfer, der Kopf ein Glockenturm mit Kupferdach und Blitzableiter, in dessen
 * Fenster ein Licht brennt. Die rechte Hand ist eine Glocke, die linke eine steinerne Faust.
 * Gut vier Blöcke hoch. In der zweiten Phase fehlen die Kupferplatten.
 */
public class CityColossusModel extends HierarchicalModel<CityColossus> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(TimeMod.id("city_colossus"), "main");
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart rightPlate;
    private final ModelPart leftPlate;
    private float partialTick;

    public CityColossusModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.rightArm = body.getChild("right_arm");
        this.leftArm = body.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.rightPlate = body.getChild("right_plate");
        this.leftPlate = body.getChild("left_plate");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-12, -24, -7, 24, 24, 14),
            PartPose.offset(0, 6, 0));
        body.addOrReplaceChild("clock", CubeListBuilder.create().texOffs(80, 0).addBox(-5, -20, -7.8F, 10, 10, 1), PartPose.ZERO);
        body.addOrReplaceChild("right_plate", CubeListBuilder.create().texOffs(104, 0).addBox(-15, -26, -8, 9, 5, 16), PartPose.ZERO);
        body.addOrReplaceChild("left_plate", CubeListBuilder.create().texOffs(104, 24).addBox(6, -26, -8, 9, 5, 16), PartPose.ZERO);
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 40).addBox(-6, -12, -6, 12, 12, 12)
            .texOffs(48, 40).addBox(-7, -14, -7, 14, 2, 14)
            .texOffs(104, 48).addBox(-5, -18, -5, 10, 4, 10)
            .texOffs(144, 48).addBox(-3, -21, -3, 6, 3, 6)
            .texOffs(168, 48).addBox(-1, -27, -1, 2, 6, 2), PartPose.offset(0, -24, 0));
        head.addOrReplaceChild("window", CubeListBuilder.create().texOffs(176, 48).addBox(-3, -8, -6.4F, 6, 3, 1), PartPose.ZERO);
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 64).addBox(-6, -2, -4, 6, 26, 8),
            PartPose.offset(-13, -21, 0));
        rightArm.addOrReplaceChild("bell", CubeListBuilder.create().texOffs(28, 64).addBox(-6, 24, -3, 6, 3, 6)
            .texOffs(52, 64).addBox(-7, 27, -4, 8, 4, 8)
            .texOffs(84, 64).addBox(-8, 31, -5, 10, 2, 10), PartPose.ZERO);
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(124, 64).addBox(0, -2, -4, 6, 26, 8),
            PartPose.offset(13, -21, 0));
        leftArm.addOrReplaceChild("fist", CubeListBuilder.create().texOffs(152, 64).addBox(-1, 22, -5, 8, 8, 10), PartPose.ZERO);
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 98).addBox(-4, 0, -4, 8, 18, 8), PartPose.offset(-6, 6, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(32, 98).addBox(-4, 0, -4, 8, 18, 8), PartPose.offset(6, 6, 0));
        return LayerDefinition.create(mesh, 256, 128);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void prepareMobModel(CityColossus colossus, float limbSwing, float limbSwingAmount, float partialTick) {
        this.partialTick = partialTick;
    }

    @Override
    public void setupAnim(CityColossus colossus, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root().getAllParts().forEach(ModelPart::resetPose);
        rightPlate.visible = !colossus.isBare();
        leftPlate.visible = !colossus.isBare();
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.6F;
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.5F;
        // Schwerer, langsamer Gang
        float walk = Mth.cos(limbSwing * 0.4F) * 0.7F * limbSwingAmount;
        rightLeg.xRot = walk;
        leftLeg.xRot = -walk;
        body.zRot = Mth.cos(limbSwing * 0.4F) * 0.04F * limbSwingAmount;
        rightArm.xRot = -walk * 0.5F;
        leftArm.xRot = walk * 0.5F;
        float sway = Mth.sin(ageInTicks * 0.05F) * 0.03F;
        rightArm.zRot = 0.08F + sway;
        leftArm.zRot = -0.08F - sway;
        int strike = colossus.strikeTicks();
        if (strike > 0) {
            // Glockenschlag: die Glockenhand hebt sich und schlägt auf den Boden
            float t = (CityColossus.STRIKE_TICKS - strike + partialTick) / CityColossus.STRIKE_TICKS;
            float raise = t < 0.45F ? t / 0.45F : t < 0.55F ? 1.0F - (t - 0.45F) / 0.1F * 1.3F : -0.3F * (1.0F - (t - 0.55F) / 0.45F);
            rightArm.xRot = -2.8F * Math.max(raise, -0.3F);
            body.xRot = raise < 0 ? 0.25F * -raise / 0.3F : -0.1F * raise;
        }
        int stomp = colossus.stompTicks();
        if (stomp > 0) {
            float t = (CityColossus.STOMP_TICKS - stomp + partialTick) / CityColossus.STOMP_TICKS;
            float lift = Mth.sin(Math.min(1.0F, t * 2.0F) * Mth.PI);
            rightArm.xRot = -2.4F * lift;
            leftArm.xRot = -2.4F * lift;
            body.xRot = t > 0.5F ? 0.3F * Mth.sin((t - 0.5F) * 2.0F * Mth.PI) : -0.15F * lift;
        }
        int breath = colossus.breathTicks();
        if (breath > 0) {
            float t = (CityColossus.BREATH_TICKS - breath + partialTick) / CityColossus.BREATH_TICKS;
            head.xRot += 0.35F * Mth.sin(t * Mth.PI);
            body.xRot += 0.12F * Mth.sin(t * Mth.PI);
        }
        if (attackTime > 0.0F) {
            float swing = Mth.sin(attackTime * Mth.PI);
            leftArm.xRot = -1.8F * swing;
            body.yRot = 0.25F * swing;
        }
    }
}
