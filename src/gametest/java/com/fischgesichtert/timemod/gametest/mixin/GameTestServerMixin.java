package com.fischgesichtert.timemod.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.WorldLoader;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Vanilla legt die Testwelt nur mit Oberwelt, Nether und End an und übergeht Dimensionen aus
 * Datenpaketen. Die Epochen sollen aber mitgeprüft werden. (Nur im Testlauf, nicht in der Mod.)
 */
@Mixin(value = GameTestServer.class, remap = false)
public abstract class GameTestServerMixin {
    @WrapOperation(method = "method_40377", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/levelgen/WorldDimensions;bake(Lnet/minecraft/core/Registry;)Lnet/minecraft/world/level/levelgen/WorldDimensions$Complete;"))
    private static WorldDimensions.Complete timemod$withDatapackDimensions(WorldDimensions dimensions, Registry<LevelStem> empty,
                                                                          Operation<WorldDimensions.Complete> original,
                                                                          @Local(argsOnly = true) WorldLoader.DataLoadContext context) {
        return original.call(dimensions, context.datapackDimensions().registryOrThrow(Registries.LEVEL_STEM));
    }
}
