package com.fischgesichtert.timemod.client.model;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.entity.boss.ClockmakerBoss;
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
 * Der Uhrmacher: Messing-Titan mit Helm und Monokel, einer Brustklappe vor dem leuchtenden Uhrwerk
 * und Zahnrad-Sägen an beiden Händen. Rund 3,3 Blöcke hoch.
 */
public class ClockmakerModel extends HierarchicalModel<ClockmakerBoss> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(TimeMod.id("clockmaker"), "main");
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart door;
    private final ModelPart rightSaw;
    private final ModelPart leftSaw;
    private float partialTick;

    public ClockmakerModel(ModelPart root) {
        this.root = root;
        ModelPart body = root.getChild("body");
        this.head = root.getChild("head");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.door = body.getChild("door");
        this.rightSaw = rightArm.getChild("saw");
        this.leftSaw = leftArm.getChild("saw");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-9, 0, -5, 18, 20, 10),
            PartPose.offset(0, -16, 0));
        // Der Kern sitzt vor dem Rumpf, die Klappe davor verdeckt ihn
        body.addOrReplaceChild("core", CubeListBuilder.create().texOffs(104, 0).addBox(-3, 6, -5.7F, 6, 6, 3), PartPose.ZERO);
        body.addOrReplaceChild("door", CubeListBuilder.create().texOffs(96, 24).addBox(0, 0, -1, 10, 10, 1), PartPose.offset(-5, 4, -5.8F));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(56, 0).addBox(-6, -12, -6, 12, 12, 12),
            PartPose.offset(0, -16, 0));
        head.addOrReplaceChild("monocle", CubeListBuilder.create().texOffs(104, 10).addBox(1.5F, -8, -6.6F, 3, 3, 1), PartPose.ZERO);
        head.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(112, 10).addBox(-1, -16, -1, 2, 4, 2), PartPose.ZERO);
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 30).addBox(-5, -2, -3, 6, 22, 6),
            PartPose.offset(-10, -14, 0));
        rightArm.addOrReplaceChild("saw", CubeListBuilder.create().texOffs(0, 60).addBox(-6, -6, -0.5F, 12, 12, 1), PartPose.offset(-2, 21, 0));
        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(24, 30).addBox(-1, -2, -3, 6, 22, 6),
            PartPose.offset(10, -14, 0));
        leftArm.addOrReplaceChild("saw", CubeListBuilder.create().texOffs(0, 60).mirror().addBox(-6, -6, -0.5F, 12, 12, 1), PartPose.offset(2, 21, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(48, 30).addBox(-3, 0, -3, 6, 20, 6), PartPose.offset(-4.5F, 4, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(72, 30).addBox(-3, 0, -3, 6, 20, 6), PartPose.offset(4.5F, 4, 0));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void prepareMobModel(ClockmakerBoss boss, float limbSwing, float limbSwingAmount, float partialTick) {
        this.partialTick = partialTick;
    }

    @Override
    public void setupAnim(ClockmakerBoss boss, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root().getAllParts().forEach(ModelPart::resetPose);
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        float walk = Mth.cos(limbSwing * 0.45F) * 1.1F * limbSwingAmount;
        rightLeg.xRot = walk;
        leftLeg.xRot = -walk;
        rightArm.xRot = -walk * 0.8F;
        leftArm.xRot = walk * 0.8F;
        // Schweres Stampfen: leichtes Schwanken des Oberkörpers
        rightArm.zRot = 0.08F + Mth.sin(ageInTicks * 0.09F) * 0.04F;
        leftArm.zRot = -0.08F - Mth.sin(ageInTicks * 0.09F) * 0.04F;
        int throwing = boss.throwTicks();
        if (throwing > 0) {
            float t = throwing / 10.0F;
            rightArm.xRot = -2.6F * Mth.sin(t * Mth.PI);
            leftArm.xRot = -1.4F * Mth.sin(t * Mth.PI);
        }
        rightSaw.zRot = ageInTicks * 0.7F;
        leftSaw.zRot = -ageInTicks * 0.7F;
        float open = Mth.lerp(partialTick, boss.doorOpenO, boss.doorOpen);
        door.yRot = -1.9F * open;
    }
}
