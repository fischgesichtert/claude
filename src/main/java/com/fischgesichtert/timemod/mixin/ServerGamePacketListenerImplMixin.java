package com.fischgesichtert.timemod.mixin;

import com.fischgesichtert.timemod.time.TimeEngine;
import com.fischgesichtert.timemod.time.TimeLevel;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPickItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Eingefrorene Spieler können im Stillstand nichts tun. Die Prüfung erfolgt erst nach dem Wechsel
 * auf den Server-Thread ({@code ensureRunningOnSameThread}).
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Shadow
    public abstract void teleport(double x, double y, double z, float yRot, float xRot);

    @Unique
    private static final String ENSURE = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V";

    @Unique
    private boolean timemod$frozen() {
        TimeEngine engine = ((TimeLevel) player.level()).timemod$getEngine();
        return engine != null && !engine.isIdle() && engine.isFrozenPlayer(player);
    }

    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE", target = ENSURE, shift = At.Shift.AFTER), cancellable = true)
    private void timemod$move(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (timemod$frozen()) {
            teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
            ci.cancel();
        }
    }

    @Inject(method = "handleMoveVehicle", at = @At(value = "INVOKE", target = ENSURE, shift = At.Shift.AFTER), cancellable = true)
    private void timemod$moveVehicle(ServerboundMoveVehiclePacket packet, CallbackInfo ci) {
        if (timemod$frozen()) {
            ci.cancel();
        }
    }

    @Inject(method = "handleUseItem", at = @At(value = "INVOKE", target = ENSURE, shift = At.Shift.AFTER), cancellable = true)
    private void timemod$useItem(ServerboundUseItemPacket packet, CallbackInfo ci) {
        if (timemod$frozen()) {
            ci.cancel();
        }
    }

    @Inject(method = "handleUseItemOn", at = @At(value = "INVOKE", target = ENSURE, shift = At.Shift.AFTER), cancellable = true)
    private void timemod$useItemOn(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
        if (timemod$frozen()) {
            player.containerMenu.sendAllDataToRemote();
            ci.cancel();
        }
    }

    @Inject(method = "handlePlayerAction", at = @At(value = "INVOKE", target = ENSURE, shift = At.Shift.AFTER), cancellable = true)
    private void timemod$playerAction(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        if (timemod$frozen()) {
            ci.cancel();
        }
    }

    @Inject(method = "handleInteract", at = @At(value = "INVOKE", target = ENSURE, shift = At.Shift.AFTER), cancellable = true)
    private void timemod$interact(ServerboundInteractPacket packet, CallbackInfo ci) {
        if (timemod$frozen()) {
            ci.cancel();
        }
    }

    @Inject(method = "handlePickItem", at = @At(value = "INVOKE", target = ENSURE, shift = At.Shift.AFTER), cancellable = true)
    private void timemod$pickItem(ServerboundPickItemPacket packet, CallbackInfo ci) {
        if (timemod$frozen()) {
            ci.cancel();
        }
    }

    @Inject(method = "handleContainerClick", at = @At(value = "INVOKE", target = ENSURE, shift = At.Shift.AFTER), cancellable = true)
    private void timemod$containerClick(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        if (timemod$frozen()) {
            player.containerMenu.sendAllDataToRemote();
            ci.cancel();
        }
    }

    @Inject(method = "handlePlayerCommand", at = @At(value = "INVOKE", target = ENSURE, shift = At.Shift.AFTER), cancellable = true)
    private void timemod$playerCommand(ServerboundPlayerCommandPacket packet, CallbackInfo ci) {
        if (timemod$frozen()) {
            ci.cancel();
        }
    }

    @Inject(method = "handleAnimate", at = @At(value = "INVOKE", target = ENSURE, shift = At.Shift.AFTER), cancellable = true)
    private void timemod$swing(ServerboundSwingPacket packet, CallbackInfo ci) {
        if (timemod$frozen()) {
            ci.cancel();
        }
    }
}
