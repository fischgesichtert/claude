package com.fischgesichtert.timemod.future;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Zeit-Echos: Was die Zukunft aus den Truhen der Gegenwart kopiert, ist nur ein Abbild. Man sieht es,
 * aber bei Berührung zerfällt es – sonst ließe sich alles verdoppeln, indem man es aus der Zukunft holt.
 * Nur die Zeitkapsel trägt Dinge wirklich durch die Zeit.
 *
 * <p>Echos lassen sich nicht anfassen (Klick im Menü, Aufheben, ins Inventar legen), Trichter ziehen sie
 * nicht ab, und als Item auf dem Boden vergehen sie sofort.
 */
public final class Echo {
    private static final String TAG = "timemod_echo";

    private Echo() {
    }

    public static void mark(ItemStack stack, Level level) {
        if (!stack.isEmpty()) {
            stack.getOrCreateTag().putString(TAG, level.dimension().location().toString());
        }
    }

    public static boolean is(@Nullable ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.hasTag() && stack.getTag().contains(TAG);
    }

    /** Ein Echo zerfällt: verschwindet mit Staub und leisem Rieseln. */
    public static void dissolve(Level level, Vec3 at, @Nullable Player player) {
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ASH, at.x, at.y, at.z, 24, 0.3, 0.3, 0.3, 0.01);
            server.sendParticles(ParticleTypes.WHITE_ASH, at.x, at.y, at.z, 12, 0.25, 0.25, 0.25, 0.01);
            server.playSound(null, at.x, at.y, at.z, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 0.6F, 1.6F);
        }
        if (player != null) {
            player.displayClientMessage(Component.translatable("message.timemod.echo.dissolved"), true);
        }
    }
}
