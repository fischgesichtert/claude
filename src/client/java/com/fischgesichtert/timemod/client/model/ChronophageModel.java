package com.fischgesichtert.timemod.client.model;

import com.fischgesichtert.timemod.TimeMod;
import com.fischgesichtert.timemod.entity.boss.ChronophageBoss;
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
 * Der Chronophage: ein schwebendes Maul mit Zifferblatt auf dem Panzer, Zähnen wie Uhrzeigern, einem
 * leuchtenden Auge im Rachen, kreisenden Zeitsplittern und vier pendelnden Tentakeln.
 */
public class ChronophageModel extends HierarchicalModel<ChronophageBoss> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(TimeMod.id("chronophage"), "main");
    private static final int TENDRILS = 4;
    private static final int SEGMENTS = 3;
    private static final int ORBITERS = 6;
    private final ModelPart root;
    private final ModelPart upperJaw;
    private final ModelPart lowerJaw;
    private final ModelPart core;
    private final ModelPart[][] tendrils = new ModelPart[TENDRILS][SEGMENTS];
    private final ModelPart[] orbiters = new ModelPart[ORBITERS];
    private float partialTick;

    public ChronophageModel(ModelPart root) {
        this.root = root;
        ModelPart body = root.getChild("body");
        this.upperJaw = body.getChild("upper_jaw");
        this.lowerJaw = body.getChild("lower_jaw");
        this.core = body.getChild("core");
        for (int i = 0; i < TENDRILS; i++) {
            ModelPart part = lowerJaw.getChild("tendril" + i);
            for (int s = 0; s < SEGMENTS; s++) {
                tendrils[i][s] = part;
                if (s < SEGMENTS - 1) {
                    part = part.getChild("segment");
                }
            }
        }
        for (int i = 0; i < ORBITERS; i++) {
            orbiters[i] = body.getChild("orbiter" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0, 4, 0));
        PartDefinition upper = body.addOrReplaceChild("upper_jaw", CubeListBuilder.create().texOffs(0, 0).addBox(-12, -10, -22, 24, 10, 24),
            PartPose.offset(0, 0, 10));
        for (int i = 0; i < 7; i++) {
            float x = -10.5F + i * 3.4F;
            upper.addOrReplaceChild("tooth" + i, CubeListBuilder.create().texOffs(96, 16).addBox(-1, 0, -1, 2, 4 + (i % 2) * 2, 2),
                PartPose.offset(x, 0, -20.5F));
        }
        PartDefinition lower = body.addOrReplaceChild("lower_jaw", CubeListBuilder.create().texOffs(0, 34).addBox(-11, 0, -21, 22, 6, 23),
            PartPose.offset(0, 0, 10));
        for (int i = 0; i < 6; i++) {
            float x = -9 + i * 3.6F;
            lower.addOrReplaceChild("tooth" + i, CubeListBuilder.create().texOffs(96, 16).addBox(-1, -3, -1, 2, 3, 2),
                PartPose.offset(x, 0, -19.5F));
        }
        for (int i = 0; i < TENDRILS; i++) {
            float x = i < 2 ? -7 : 7;
            float z = i % 2 == 0 ? -14 : -4;
            PartDefinition seg = lower.addOrReplaceChild("tendril" + i, CubeListBuilder.create().texOffs(96, 32).addBox(-1, 0, -1, 2, 7, 2),
                PartPose.offset(x, 6, z));
            for (int s = 1; s < SEGMENTS; s++) {
                seg = seg.addOrReplaceChild("segment", CubeListBuilder.create().texOffs(104, 32).addBox(-1, 0, -1, 2, 7, 2), PartPose.offset(0, 7, 0));
            }
        }
        body.addOrReplaceChild("core", CubeListBuilder.create().texOffs(96, 0).addBox(-4, -4, -4, 8, 8, 8), PartPose.offset(0, 0, -2));
        for (int i = 0; i < ORBITERS; i++) {
            body.addOrReplaceChild("orbiter" + i, CubeListBuilder.create().texOffs(96, 24).addBox(-1.5F, -1.5F, -1.5F, 3, 3, 3), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void prepareMobModel(ChronophageBoss boss, float limbSwing, float limbSwingAmount, float partialTick) {
        this.partialTick = partialTick;
    }

    @Override
    public void setupAnim(ChronophageBoss boss, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root().getAllParts().forEach(ModelPart::resetPose);
        float open = Mth.lerp(partialTick, boss.mouthOpenO, boss.mouthOpen);
        float breathe = Mth.sin(ageInTicks * 0.12F) * 0.04F;
        upperJaw.xRot = -0.55F * open - breathe;
        lowerJaw.xRot = 0.45F * open + breathe;
        core.zRot = ageInTicks * 0.05F;
        float scale = 0.6F + 0.4F * open;
        core.xScale = scale;
        core.yScale = scale;
        core.zScale = scale;
        for (int i = 0; i < TENDRILS; i++) {
            for (int s = 0; s < SEGMENTS; s++) {
                float phase = ageInTicks * 0.09F + i * 1.7F + s * 0.6F;
                tendrils[i][s].xRot = Mth.sin(phase) * (0.25F + 0.1F * s);
                tendrils[i][s].zRot = Mth.cos(phase * 0.8F) * (0.15F + 0.08F * s);
            }
        }
        for (int i = 0; i < ORBITERS; i++) {
            float a = ageInTicks * 0.06F + i * Mth.TWO_PI / ORBITERS;
            float r = 19.0F;
            orbiters[i].x = Mth.cos(a) * r;
            orbiters[i].z = Mth.sin(a) * r - 4;
            orbiters[i].y = -4 + Mth.sin(a * 2 + i) * 4;
            orbiters[i].xRot = a;
            orbiters[i].yRot = a * 1.3F;
        }
    }
}
