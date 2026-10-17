package com.fischgesichtert.timemod.mixin;

import com.fischgesichtert.timemod.future.Echo;
import com.fischgesichtert.timemod.safety.Safety;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ein Zeit-Echo in einem Menü zerfällt, sobald man danach greift. */
@Mixin(AbstractContainerMenu.class)
abstract class AbstractContainerMenuMixin {
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void timemod$echoCrumbles(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (slotId < 0 || slotId >= menu.slots.size()) {
            return;
        }
        Slot slot = menu.slots.get(slotId);
        if (!Echo.is(slot.getItem())) {
            return;
        }
        ci.cancel();
        if (!player.level().isClientSide) {
            slot.set(ItemStack.EMPTY);
            Safety.run("Zeit-Echo zerfällt", () -> Echo.dissolve(player.level(), player.getEyePosition().add(player.getLookAngle().scale(0.8)), player));
            menu.broadcastChanges();
        }
    }
}
