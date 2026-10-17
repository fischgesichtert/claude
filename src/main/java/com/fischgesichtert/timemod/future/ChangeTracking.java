package com.fischgesichtert.timemod.future;

import com.fischgesichtert.timemod.era.Era;
import com.fischgesichtert.timemod.era.Eras;
import com.fischgesichtert.timemod.safety.Safety;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * Merkt sich, wo die Welt verändert wurde: In der Gegenwart wird der Chunk neu – die Zukunft rechnet ihn
 * beim nächsten Besuch neu. In der Zukunft gehört ein Chunk dem, der dort baut: er bleibt, wie er ist.
 */
public final class ChangeTracking {
    private ChangeTracking() {
    }

    public static void init() {
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> Safety.run("Zukunft: Änderung merken", () -> changed(level, pos, true)));
    }

    /** {@code byPlayer}: von Hand gebaut oder abgebaut (Explosionen verändern nur die Gegenwart). */
    public static void changed(Level level, BlockPos pos, boolean byPlayer) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (level.dimension() == Level.OVERWORLD) {
            PresentChanges.get(server.getServer()).mark(new ChunkPos(pos), server.getGameTime());
            return;
        }
        Era era = Eras.byLevel(level.dimension());
        if (byPlayer && era != null && era.isFuture()) {
            FutureChunks.get(server).claim(ChunkPos.asLong(pos));
        }
    }
}
