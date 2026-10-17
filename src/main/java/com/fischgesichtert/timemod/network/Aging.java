package com.fischgesichtert.timemod.network;

import com.fischgesichtert.timemod.TimeMod;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Sichtbares Altern: Der Client färbt gealterte Wesen blass ein. */
public final class Aging {
    public static final ResourceLocation PACKET = TimeMod.id("aging");

    private Aging() {
    }

    /** {@code age} 0 = jung, 1 = am Ende. */
    public static void send(LivingEntity entity, float age) {
        for (ServerPlayer player : PlayerLookup.tracking(entity)) {
            if (ServerPlayNetworking.canSend(player, PACKET)) {
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                buf.writeVarInt(entity.getId());
                buf.writeFloat(age);
                ServerPlayNetworking.send(player, PACKET, buf);
            }
        }
    }
}
