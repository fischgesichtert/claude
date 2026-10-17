package com.fischgesichtert.timemod.mixin;

import com.fischgesichtert.timemod.future.Echo;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Trichter (auch in Loren) ziehen keine Zeit-Echos aus Truhen. */
@Mixin(HopperBlockEntity.class)
abstract class HopperBlockEntityMixin {
    @Inject(method = "canTakeItemFromContainer", at = @At("HEAD"), cancellable = true)
    private static void timemod$echoStays(Container source, Container destination, ItemStack stack, int slot, Direction direction,
                                          CallbackInfoReturnable<Boolean> cir) {
        if (Echo.is(stack)) {
            cir.setReturnValue(false);
        }
    }
}
