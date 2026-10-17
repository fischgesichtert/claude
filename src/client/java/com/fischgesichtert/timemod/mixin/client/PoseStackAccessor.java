package com.fischgesichtert.timemod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Deque;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Für den Absturzschutz: wie tief der Matrix-Stapel gerade ist. */
@Mixin(PoseStack.class)
public interface PoseStackAccessor {
    @Accessor("poseStack")
    Deque<PoseStack.Pose> timemod$poses();
}
