package com.fischgesichtert.timemod.mixin;

import com.fischgesichtert.timemod.TimeMod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Biome, Strukturen und Epochen der Mod sind fester Bestandteil von TIME und keine „experimentellen
 * Einstellungen“. Ohne das zeigt Minecraft bei jeder Welt eine Warnung samt Backup-Frage an.
 */
@Mixin(RegistryDataLoader.class)
public abstract class RegistryDataLoaderMixin {
    @WrapOperation(method = "loadRegistryContents", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/core/WritableRegistry;register(Lnet/minecraft/resources/ResourceKey;Ljava/lang/Object;Lcom/mojang/serialization/Lifecycle;)Lnet/minecraft/core/Holder$Reference;"))
    private static <E> Holder.Reference<E> timemod$stableModData(WritableRegistry<E> registry, ResourceKey<E> key, Object value, Lifecycle lifecycle,
                                                               Operation<Holder.Reference<E>> original) {
        boolean ours = key.location().getNamespace().equals(TimeMod.MOD_ID);
        return original.call(registry, key, value, ours ? Lifecycle.stable() : lifecycle);
    }
}
