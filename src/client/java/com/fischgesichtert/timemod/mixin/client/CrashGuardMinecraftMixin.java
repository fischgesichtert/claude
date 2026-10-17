package com.fischgesichtert.timemod.mixin.client;

import com.fischgesichtert.timemod.client.ClientSafety;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Absturzschutz für Rechtsklick und Angriff im Client: Scheitert dabei ein Gegenstand, Block oder Wesen
 * der Mod, passiert eben nichts – statt dass das Spiel abstürzt.
 */
@Mixin(Minecraft.class)
abstract class CrashGuardMinecraftMixin {
    @WrapOperation(method = "startUseItem", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItem(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult timemod$guardUseItem(MultiPlayerGameMode gameMode, Player player, InteractionHand hand, Operation<InteractionResult> original) {
        try {
            return original.call(gameMode, player, hand);
        } catch (Throwable error) {
            if (!ClientSafety.absorbUse("Benutzen", error)) {
                throw error;
            }
            return InteractionResult.FAIL;
        }
    }

    @WrapOperation(method = "startUseItem", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult timemod$guardUseItemOn(MultiPlayerGameMode gameMode, LocalPlayer player, InteractionHand hand, BlockHitResult hit,
                                                     Operation<InteractionResult> original) {
        try {
            return original.call(gameMode, player, hand, hit);
        } catch (Throwable error) {
            if (!ClientSafety.absorbUse("Benutzen an Block", error)) {
                throw error;
            }
            return InteractionResult.FAIL;
        }
    }

    @WrapOperation(method = "startUseItem", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;interactAt(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/EntityHitResult;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult timemod$guardInteractAt(MultiPlayerGameMode gameMode, Player player, Entity entity, EntityHitResult hit, InteractionHand hand,
                                                      Operation<InteractionResult> original) {
        try {
            return original.call(gameMode, player, entity, hit, hand);
        } catch (Throwable error) {
            if (!ClientSafety.absorbUse("Benutzen an Wesen", error)) {
                throw error;
            }
            return InteractionResult.FAIL;
        }
    }

    @WrapOperation(method = "startUseItem", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;interact(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult timemod$guardInteract(MultiPlayerGameMode gameMode, Player player, Entity entity, InteractionHand hand,
                                                    Operation<InteractionResult> original) {
        try {
            return original.call(gameMode, player, entity, hand);
        } catch (Throwable error) {
            if (!ClientSafety.absorbUse("Benutzen an Wesen", error)) {
                throw error;
            }
            return InteractionResult.FAIL;
        }
    }

    @WrapOperation(method = "startAttack", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;attack(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;)V"))
    private void timemod$guardAttack(MultiPlayerGameMode gameMode, Player player, Entity target, Operation<Void> original) {
        try {
            original.call(gameMode, player, target);
        } catch (Throwable error) {
            if (!ClientSafety.absorbUse("Angriff", error)) {
                throw error;
            }
        }
    }
}
