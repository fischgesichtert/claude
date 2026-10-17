package com.fischgesichtert.timemod.mixin;

import com.fischgesichtert.timemod.future.Echo;
import com.fischgesichtert.timemod.safety.Safety;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ein Zeit-Echo kommt nie ins Inventar – egal auf welchem Weg (Pult, Bücherregal, Aufheben …). */
@Mixin(Inventory.class)
abstract class InventoryMixin {
    @Shadow
    @Final
    public Player player;

    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void timemod$echoCrumbles(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (Echo.is(stack)) {
            stack.setCount(0);
            if (!player.level().isClientSide) {
                Safety.run("Zeit-Echo zerfällt", () -> Echo.dissolve(player.level(), player.position().add(0, 1, 0), player));
            }
            cir.setReturnValue(true);
        }
    }
}
