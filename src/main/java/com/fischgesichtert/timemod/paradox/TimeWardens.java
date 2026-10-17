package com.fischgesichtert.timemod.paradox;

import com.fischgesichtert.timemod.entity.TimeWarden;
import com.fischgesichtert.timemod.gate.TimeStream;
import com.fischgesichtert.timemod.registry.ModEntities;
import com.fischgesichtert.timemod.registry.ModGameRules;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Die Zeitpolizei rückt aus: Wer über 50 % Paradox-Druck trägt, wird bemerkt – nach einer Weile
 * erscheint ein Zeitwächter in der Nähe. Ab 75 % kommen sie zu zweit, ab 90 % zu dritt, und öfter.
 * Abschalten lässt sich das mit der Spielregel {@code doTimeWardens}.
 */
public final class TimeWardens {
    /** Wie viele Wächter höchstens gleichzeitig hinter einem Spieler her sind. */
    public static int allowed(float pressure) {
        return pressure >= 90.0F ? 3 : pressure >= ParadoxPressure.HUNTED ? 2 : pressure >= ParadoxPressure.NOTICED ? 1 : 0;
    }

    /** Ticks bis zum nächsten Wächter, je Spieler (nicht gespeichert: nach einem Neustart wird neu gezählt). */
    private static final Map<UUID, Integer> COUNTDOWN = new HashMap<>();
    /** Nach einem besiegten Wächter lassen sie sich etwas mehr Zeit. */
    private static final int DEFEAT_DELAY = 60 * 20;

    private TimeWardens() {
    }

    static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) {
            return;
        }
        ParadoxPressure data = ParadoxPressure.get(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            float pressure = data.value(player.getUUID());
            if (pressure < ParadoxPressure.NOTICED || !eligible(player)) {
                COUNTDOWN.remove(player.getUUID());
                continue;
            }
            RandomSource random = player.getRandom();
            Integer left = COUNTDOWN.get(player.getUUID());
            if (left == null) {
                // Frisch bemerkt: ein wenig Zeit, die Warnung zu verstehen
                COUNTDOWN.put(player.getUUID(), 20 * (20 + random.nextInt(21)));
                continue;
            }
            left -= 20;
            if (left > 0) {
                COUNTDOWN.put(player.getUUID(), left);
                continue;
            }
            if (hunters(player).size() < allowed(pressure)) {
                TimeWarden warden = summon(player);
                if (warden == null) {
                    // Kein Platz gefunden – gleich noch einmal versuchen
                    COUNTDOWN.put(player.getUUID(), 5 * 20);
                    continue;
                }
            }
            COUNTDOWN.put(player.getUUID(), 20 * (pressure >= ParadoxPressure.HUNTED ? 25 + random.nextInt(21) : 60 + random.nextInt(61)));
        }
    }

    /** Ob die Wächter hinter diesem Spieler her sein dürfen. */
    public static boolean eligible(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        return player.isAlive() && ParadoxPressure.counts(player) && level.getDifficulty() != Difficulty.PEACEFUL
            && level.getGameRules().getBoolean(ModGameRules.TIME_WARDENS) && !TimeStream.inStream(player.getUUID());
    }

    /** Die Wächter in der Nähe, die diesen Spieler jagen. */
    public static List<TimeWarden> hunters(ServerPlayer player) {
        return player.serverLevel().getEntitiesOfClass(TimeWarden.class, new AABB(player.blockPosition()).inflate(96),
            warden -> warden.isAlive() && player.getUUID().equals(warden.quarry()));
    }

    /** Ruft einen Wächter in die Nähe des Spielers (8–16 Blöcke entfernt, auf festem Boden). */
    @Nullable
    public static TimeWarden summon(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos spot = findSpot(level, player.blockPosition(), player.getRandom());
        if (spot == null) {
            return null;
        }
        TimeWarden warden = ModEntities.TIME_WARDEN.create(level);
        if (warden == null) {
            return null;
        }
        Vec3 at = Vec3.atBottomCenterOf(spot);
        float yaw = (float) (Mth.atan2(player.getZ() - at.z, player.getX() - at.x) * Mth.RAD_TO_DEG) - 90.0F;
        warden.moveTo(at.x, at.y, at.z, yaw, 0.0F);
        warden.setYHeadRot(yaw);
        warden.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), MobSpawnType.EVENT, null, null);
        warden.hunt(player);
        // Erst der Auftritt, dann in die Welt: So kennt der Client den Lichtstrich schon beim ersten Bild
        warden.arrive();
        level.addFreshEntity(warden);
        if (hunters(player).size() <= 1) {
            player.displayClientMessage(Component.translatable("message.timemod.paradox.warden").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
        return warden;
    }

    /** Ein Platz mit festem Boden und zwei freien Blöcken darüber, nicht im Wasser. */
    @Nullable
    static BlockPos findSpot(ServerLevel level, BlockPos center, RandomSource random) {
        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = 8.0 + random.nextDouble() * 8.0;
            int x = center.getX() + Mth.floor(Math.cos(angle) * distance);
            int z = center.getZ() + Mth.floor(Math.sin(angle) * distance);
            if (!level.hasChunkAt(new BlockPos(x, center.getY(), z))) {
                continue;
            }
            for (int dy = 6; dy >= -8; dy--) {
                BlockPos feet = new BlockPos(x, center.getY() + dy, z);
                if (standable(level, feet)) {
                    return feet;
                }
            }
        }
        return null;
    }

    static boolean standable(ServerLevel level, BlockPos feet) {
        BlockState ground = level.getBlockState(feet.below());
        if (!ground.isFaceSturdy(level, feet.below(), Direction.UP)) {
            return false;
        }
        for (int i = 0; i < 3; i++) {
            BlockPos pos = feet.above(i);
            BlockState state = level.getBlockState(pos);
            if (!state.getFluidState().isEmpty() || !state.getCollisionShape(level, pos).isEmpty()) {
                return false;
            }
        }
        return level.getWorldBorder().isWithinBounds(feet);
    }

    /** Ein Wächter wurde besiegt: Der nächste lässt auf sich warten. */
    public static void onDefeated(ServerPlayer player) {
        COUNTDOWN.merge(player.getUUID(), DEFEAT_DELAY, Integer::sum);
    }

    /** Für Tests und Befehle: den Countdown eines Spielers setzen. */
    public static void schedule(ServerPlayer player, int ticks) {
        COUNTDOWN.put(player.getUUID(), ticks);
    }
}
