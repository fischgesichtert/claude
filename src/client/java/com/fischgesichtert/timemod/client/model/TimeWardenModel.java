package com.fischgesichtert.timemod.client.model;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.entity.TimeWarden;
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
 * Der Zeitwächter: hochgewachsen, im langen dunklen Mantel mit Messingschultern, das Gesicht hinter
 * einem Zifferblatt verborgen, hinter dem Kopf ein sich drehender Heiligenschein aus Ziffern. In der
 * rechten Hand trägt er die Zeigerklinge, den geschliffenen Minutenzeiger einer Turmuhr.
 */
public class TimeWardenModel extends HierarchicalModel<TimeWarden> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(TimeMod.id("time_warden"), "main");
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart halo;
    private final ModelPart body;
    private final ModelPart coat;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart blade;
    private float partialTick;

    public TimeWardenModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.halo = head.getChild("halo");
        this.body = root.getChild("body");
        this.coat = root.getChild("coat");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.blade = rightArm.getChild("blade");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8),
            PartPose.offset(0, -3, 0));
        head.addOrReplaceChild("mask", CubeListBuilder.create().texOffs(32, 0).addBox(-4, -8, -4.6F, 8, 8, 1), PartPose.ZERO);
        head.addOrReplaceChild("halo", CubeListBuilder.create().texOffs(64, 0).addBox(-8, -8, 0, 16, 16, 0), PartPose.offset(0, -4, 5));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16).addBox(-4, 0, -2, 8, 13, 4),
            PartPose.offset(0, -3, 0));
        body.addOrReplaceChild("collar", CubeListBuilder.create().texOffs(24, 16).addBox(-4.5F, -1, -2.5F, 9, 3, 5), PartPose.ZERO);
        root.addOrReplaceChild("coat", CubeListBuilder.create().texOffs(52, 16).addBox(-4.5F, 0, -2.5F, 9, 11, 5), PartPose.offset(0, 7, 0));
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(80, 16).addBox(-3, -2, -2, 4, 13, 4)
            .texOffs(32, 36).addBox(-3.5F, -3, -2.5F, 5, 3, 5), PartPose.offset(-5, -1, 0));
        rightArm.addOrReplaceChild("blade", CubeListBuilder.create().texOffs(72, 36).addBox(0, -2.5F, -15, 0, 5, 20),
            PartPose.offsetAndRotation(-1, 10, 0, 0.55F, 0, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(96, 16).addBox(-1, -2, -2, 4, 13, 4)
            .texOffs(52, 36).addBox(-1.5F, -3, -2.5F, 5, 3, 5), PartPose.offset(5, -1, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 36).addBox(-2, 0, -2, 4, 14, 4), PartPose.offset(-2, 10, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 36).addBox(-2, 0, -2, 4, 14, 4), PartPose.offset(2, 10, 0));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void prepareMobModel(TimeWarden warden, float limbSwing, float limbSwingAmount, float partialTick) {
        this.partialTick = partialTick;
    }

    @Override
    public void setupAnim(TimeWarden warden, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root().getAllParts().forEach(ModelPart::resetPose);
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        halo.zRot = Mth.lerp(partialTick, warden.haloSpinO, warden.haloSpin);
        float walk = Mth.cos(limbSwing * 0.6662F) * 1.2F * limbSwingAmount;
        rightLeg.xRot = walk;
        leftLeg.xRot = -walk;
        // Der Mantel schwingt hinter den Beinen her
        coat.xRot = 0.08F + Math.abs(walk) * 0.25F;
        float breathe = Mth.sin(ageInTicks * 0.067F) * 0.04F;
        leftArm.xRot = walk * 0.7F;
        leftArm.zRot = -0.06F - breathe;
        // Die Zeigerklinge hält er gesenkt nach vorn
        rightArm.xRot = -0.25F - walk * 0.2F;
        rightArm.zRot = 0.06F + breathe;
        int cast = warden.castTicks();
        if (cast > 0) {
            // Werfender Arm nach vorn, der Heiligenschein dreht schneller (siehe Entity)
            float t = 1.0F - (cast - partialTick) / TimeWarden.CAST_TICKS;
            leftArm.xRot = -1.6F + Mth.sin(t * Mth.PI) * 0.5F;
            leftArm.yRot = 0.15F;
        }
        if (attackTime > 0.0F) {
            float swing = Mth.sin(attackTime * Mth.PI);
            rightArm.xRot = -0.25F - swing * 1.9F;
            rightArm.yRot = -swing * 0.4F;
            body.yRot = -swing * 0.2F;
            blade.xRot = 0.55F + swing * 0.6F;
        }
    }
}
