package com.fischgesichtert.timemod.mixin;

import com.fischgesichtert.timemod.future.ChangeTracking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Bauen, Eimer, Türen: Was ein Spieler an einem Block bewirkt, verändert die Welt dort (und daneben). */
@Mixin(ServerPlayerGameMode.class)
abstract class ServerPlayerGameModeMixin {
    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void timemod$trackChange(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hit,
                                     CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue().consumesAction()) {
            ChangeTracking.changed(level, hit.getBlockPos(), true);
            ChangeTracking.changed(level, hit.getBlockPos().relative(hit.getDirection()), true);
        }
    }
}
