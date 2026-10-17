package com.fischgesichtert.timemod.mixin;

import com.fischgesichtert.timemod.future.Echo;
import com.fischgesichtert.timemod.safety.Safety;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fällt ein Zeit-Echo zu Boden (zerbrochene Truhe, Spender …), vergeht es sofort. */
@Mixin(ItemEntity.class)
abstract class ItemEntityMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void timemod$echoVanishes(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (!self.level().isClientSide && Echo.is(self.getItem())) {
            Safety.run("Zeit-Echo zerfällt", () -> Echo.dissolve(self.level(), self.position(), null));
            self.discard();
            ci.cancel();
        }
    }
}
