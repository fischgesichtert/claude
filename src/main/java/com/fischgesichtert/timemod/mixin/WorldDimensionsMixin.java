package com.fischgesichtert.timemod.mixin;

import com.fischgesichtert.timemod.era.Eras;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.Lifecycle;
import java.util.stream.Stream;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Die Epochen-Dimensionen machen eine Welt nicht „experimentell“: Vanilla hält jede Welt mit mehr
 * als drei Dimensionen für unsicher. Fremde Zusatz-Dimensionen bleiben davon unberührt.
 */
@Mixin(WorldDimensions.class)
public abstract class WorldDimensionsMixin {
    @Shadow
    @Final
    private Registry<LevelStem> dimensions;

    @Inject(method = "checkStability", at = @At("HEAD"), cancellable = true)
    private static void timemod$stableEra(ResourceKey<LevelStem> key, LevelStem stem, CallbackInfoReturnable<Lifecycle> cir) {
        if (Eras.isEraDimension(key.location())) {
            cir.setReturnValue(Lifecycle.stable());
        }
    }

    @ModifyExpressionValue(method = "bake", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I"))
    private int timemod$withoutEras(int size, @Local(argsOnly = true) Registry<LevelStem> datapackDimensions) {
        long eras = Stream.concat(datapackDimensions.registryKeySet().stream(), dimensions.registryKeySet().stream())
            .distinct()
            .filter(key -> Eras.isEraDimension(key.location()))
            .count();
        return size - (int) eras;
    }
}
