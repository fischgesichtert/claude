package com.fischgesichtert.timemod.client.model;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.entity.RuinGuardian;
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
 * Der Ruinenwächter: ein gebeugter Riese aus Steinquadern und rostigen Eisenbändern, auf den Schultern
 * eine Moosdecke mit herabhängenden Ranken und einem Schössling, im Kopf ein einzelnes Linsenauge.
 * Lange Arme mit steinernen Fäusten. Im Schlaf sinkt der Kopf auf die Brust.
 */
public class RuinGuardianModel extends HierarchicalModel<RuinGuardian> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(TimeMod.id("ruin_guardian"), "main");
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart eye;
    private float partialTick;

    public RuinGuardianModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.eye = head.getChild("eye");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -10, -5.5F, 8, 10, 8),
            PartPose.offset(0, -7, -2));
        head.addOrReplaceChild("eye", CubeListBuilder.create().texOffs(34, 0).addBox(-1.5F, -7, -6.2F, 3, 3, 1), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 40).addBox(-9, -2, -6, 18, 14, 11)
            .texOffs(0, 70).addBox(-5, 12, -3.5F, 10, 5, 7), PartPose.offset(0, -7, 0));
        body.addOrReplaceChild("moss", CubeListBuilder.create().texOffs(0, 90).addBox(-9.5F, -3, -6.5F, 19, 2, 12), PartPose.ZERO);
        body.addOrReplaceChild("vine_right", CubeListBuilder.create().texOffs(0, 108).addBox(-7, -1, -6.6F, 5, 12, 0), PartPose.ZERO);
        body.addOrReplaceChild("vine_left", CubeListBuilder.create().texOffs(12, 108).addBox(3, -1, -6.6F, 4, 9, 0), PartPose.ZERO);
        body.addOrReplaceChild("sprout_a", CubeListBuilder.create().texOffs(24, 108).addBox(-2.5F, -6, 0, 5, 6, 0),
            PartPose.offsetAndRotation(-6, -3, 1, 0, Mth.PI / 4, 0));
        body.addOrReplaceChild("sprout_b", CubeListBuilder.create().texOffs(24, 108).addBox(-2.5F, -6, 0, 5, 6, 0),
            PartPose.offsetAndRotation(-6, -3, 1, 0, -Mth.PI / 4, 0));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(60, 21).addBox(-13, -2.5F, -3, 4, 30, 6)
            .texOffs(84, 0).addBox(-14, 22, -4, 6, 6, 8), PartPose.offset(0, -7, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(60, 58).addBox(9, -2.5F, -3, 4, 30, 6)
            .texOffs(84, 14).addBox(8, 22, -4, 6, 6, 8), PartPose.offset(0, -7, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(84, 30).addBox(-3.5F, -3, -3, 6, 16, 5), PartPose.offset(-4, 11, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(84, 52).addBox(-3.5F, -3, -3, 6, 16, 5), PartPose.offset(5, 11, 0));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    /** Nur das Linsenauge (für die Leuchtschicht). */
    public ModelPart eye() {
        return eye;
    }

    public ModelPart head() {
        return head;
    }

    @Override
    public void prepareMobModel(RuinGuardian guardian, float limbSwing, float limbSwingAmount, float partialTick) {
        this.partialTick = partialTick;
        int attack = guardian.getAttackAnimationTick();
        if (attack > 0) {
            rightArm.xRot = -2.0F + 1.5F * Mth.triangleWave(attack - partialTick, 10.0F);
            leftArm.xRot = -2.0F + 1.5F * Mth.triangleWave(attack - partialTick, 10.0F);
        } else {
            rightArm.xRot = (-0.2F + 1.5F * Mth.triangleWave(limbSwing, 13.0F)) * limbSwingAmount;
            leftArm.xRot = (-0.2F - 1.5F * Mth.triangleWave(limbSwing, 13.0F)) * limbSwingAmount;
        }
    }

    @Override
    public void setupAnim(RuinGuardian guardian, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float awake = Mth.lerp(partialTick, guardian.eyeOpenO, guardian.eyeOpen);
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * awake;
        // Im Schlaf sinkt der Kopf, der Rücken krümmt sich
        head.xRot = headPitch * Mth.DEG_TO_RAD * awake + (1.0F - awake) * 0.55F;
        body.xRot = 0.1F + (1.0F - awake) * 0.12F;
        rightLeg.xRot = -1.5F * Mth.triangleWave(limbSwing, 13.0F) * limbSwingAmount;
        leftLeg.xRot = 1.5F * Mth.triangleWave(limbSwing, 13.0F) * limbSwingAmount;
        rightLeg.yRot = 0.0F;
        leftLeg.yRot = 0.0F;
        // Die Arme hängen schwer; im Schlaf ruhen die Fäuste am Boden
        rightArm.zRot = 0.05F + (1.0F - awake) * 0.06F;
        leftArm.zRot = -0.05F - (1.0F - awake) * 0.06F;
    }
}
