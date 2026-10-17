package com.fischgesichtert.timemod.mixin;

import com.fischgesichtert.timemod.rewind.RewindSession;
import com.fischgesichtert.timemod.time.TimeEngine;
import com.fischgesichtert.timemod.time.TimeHitQueue;
import com.fischgesichtert.timemod.time.TimeLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    /** Eingefrorene Spieler altern nicht: kein Hunger, keine Regeneration, keine ablaufenden Effekte. */
    @Inject(method = "doTick", at = @At("HEAD"), cancellable = true)
    private void timemod$freeze(CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        TimeEngine engine = ((TimeLevel) self.level()).timemod$getEngine();
        if (engine != null && !engine.isIdle() && engine.isFrozenPlayer(self)) {
            ci.cancel();
            return;
        }
        ((TimeHitQueue) self).timemod$releaseHits();
    }

    /**
     * Wer beim Zurückspulen frei bleibt, kann nichts fallen lassen: Sein Inventar folgt der Welt, und
     * Fallengelassenes käme sonst doppelt zurück. Der Gegenstand bleibt im Inventar.
     */
    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At("HEAD"),
        cancellable = true)
    private void timemod$holdItems(ItemStack stack, boolean dropAround, boolean traceItem, CallbackInfoReturnable<ItemEntity> cir) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (!stack.isEmpty() && !self.isDeadOrDying() && RewindSession.holdsInventory(self)) {
            self.getInventory().placeItemBackInInventory(stack.copy());
            stack.setCount(0);
            cir.setReturnValue(null);
        }
    }
}
