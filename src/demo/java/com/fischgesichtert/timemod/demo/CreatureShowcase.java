package com.fischgesichtert.timemod.demo;

import com.fischgesichtert.timemod.entity.MossBear;
import com.fischgesichtert.timemod.entity.RuinGuardian;
import com.fischgesichtert.timemod.era.Eras;
import com.fischgesichtert.timemod.future.town.Settlements;
import com.fischgesichtert.timemod.registry.ModEntities;
import com.fischgesichtert.timemod.registry.ModVillagers;
import com.mojang.datafixers.util.Pair;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Vorführung „Wesen der Zukunft“ in der echten Welt von +1000: Ruinenwächter in einem untergegangenen Dorf
 * (schlafend und erwacht), Moosbären daneben (bemoost und geschoren) und der Chrono-Händler im Uhrturm
 * einer Stadt – samt seinem Handelsfenster.
 */
final class CreatureShowcase {
    private static final Logger LOGGER = LoggerFactory.getLogger("TIME-Demo");
    /** Die Ruine in +250 (der Wald hat sie noch nicht ganz verschluckt), die Stadt in +1000. */
    private static final int RUIN_YEARS = 250;
    private static final int CITY_YEARS = 1000;
    private static volatile ChunkPos ruin;
    private static volatile ChunkPos city;

    private CreatureShowcase() {
    }

    static void write(DemoDirector d) {
        d.wait(20)
            .command("gamemode spectator @a")
            .command("gamerule sendCommandFeedback false")
            .command("gamerule announceAdvancements false")
            .command("gamerule doDaylightCycle false")
            .command("gamerule doWeatherCycle false")
            .command("gamerule doTimeWardens false")
            .command("weather clear")
            .client(mc -> mc.options.renderDistance().set(10))
            .wait(20)
            .server(CreatureShowcase::findVillages)
            .server(player -> {
                // Die Epochen teilen die Uhr der Oberwelt
                player.server.overworld().setDayTime(13300);
                ServerLevel level = level(player, RUIN_YEARS);
                Vec3 at = center(level, ruin);
                look(player, level, at.add(14, 12, 14), at);
            })
            .wait(600)
            .server(CreatureShowcase::faceGuardian)
            .wait(80).screenshot("c1_waechter_ruht")
            .server(player -> nearestGuardian(player).ifPresent(RuinGuardian::wake))
            .record("seq_c_erwachen", 2).wait(40).stopRecording()
            .screenshot("c2_waechter_erwacht")
            // Moosbären bei der Ruine – im freien Blickfeld neben dem Wächter: einer bemoost, einer frisch geschoren
            .server(player -> nearestGuardian(player).ifPresent(guardian -> {
                ServerLevel level = level(player, RUIN_YEARS);
                Vec3 view = guardian.position().subtract(player.position()).multiply(1, 0, 1).normalize();
                Vec3 side = new Vec3(-view.z, 0, view.x);
                double toGuardian = guardian.position().subtract(player.position()).multiply(1, 0, 1).length();
                for (int i = 0; i < 2; i++) {
                    // Zwischen Kamera und Wächter, links und rechts im Bild
                    Vec3 at = player.position().add(view.scale(toGuardian * 0.6)).add(side.scale(i == 0 ? 2.2 : -2.2));
                    BlockPos pos = ground(level, BlockPos.containing(at));
                    MossBear bear = ModEntities.MOSS_BEAR.create(level);
                    float yaw = (float) Math.toDegrees(Math.atan2(-view.x, view.z)) + 180.0F + (i == 0 ? 50.0F : -50.0F);
                    bear.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0F);
                    LOGGER.info("Moosbär bei {}", pos.toShortString());
                    bear.setYHeadRot(yaw);
                    bear.setYBodyRot(yaw);
                    bear.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.COMMAND, null, null);
                    bear.setAge(0);
                    bear.setNoAi(true);
                    bear.setSheared(i == 1);
                    level.addFreshEntity(bear);
                }
            }))
            .wait(60).screenshot("c3_moosbaeren")
            // Der Chrono-Händler im Uhrturm der Stadt
            .server(player -> {
                player.server.overworld().setDayTime(6000);
                ServerLevel level = level(player, CITY_YEARS);
                Vec3 at = center(level, city);
                look(player, level, at.add(10, 20, 10), at);
            })
            .wait(600)
            .server(CreatureShowcase::faceTrader)
            .wait(60).screenshot("c4_chrono_haendler")
            .server(player -> {
                player.setGameMode(GameType.CREATIVE);
                trader(player).ifPresent(villager -> villager.mobInteract(player, InteractionHand.MAIN_HAND));
            })
            .wait(30).screenshot("c5_handel")
            .wait(5).quit();
    }

    private static ServerLevel level(ServerPlayer player, int years) {
        return player.server.getLevel(Eras.byYears(years).level());
    }

    /** Ein Platz mit festem Boden und zwei freien Blöcken darüber, nahe der Höhe von {@code near}. */
    private static BlockPos ground(ServerLevel level, BlockPos near) {
        for (int dy = 3; dy >= -4; dy--) {
            BlockPos feet = near.above(dy);
            if (level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), net.minecraft.core.Direction.UP)
                && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty() && level.getBlockState(feet).getFluidState().isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) {
                return feet;
            }
        }
        return near;
    }

    /**
     * Ein Kamerapunkt mit freier Sicht auf {@code target}: rundherum im Abstand {@code distance} (und etwas näher
     * oder weiter) gesucht, in der Luft und ohne Block dazwischen. Notfalls von oben.
     */
    private static Vec3 frame(ServerPlayer player, ServerLevel level, Vec3 target, double distance, double height) {
        for (double dist : new double[] {distance, distance * 0.75, distance * 1.3, distance * 0.5}) {
            for (int i = 0; i < 16; i++) {
                double angle = i * Math.PI / 8;
                Vec3 eye = target.add(Math.cos(angle) * dist, height, Math.sin(angle) * dist);
                BlockPos at = BlockPos.containing(eye);
                if (!level.getBlockState(at).isAir() || !level.getBlockState(at.below()).getFluidState().isEmpty() && !level.getBlockState(at.below()).isAir()) {
                    continue;
                }
                // Drei Strahlen (Mitte, oben, unten), damit nichts das Motiv halb verdeckt
                boolean clear = true;
                for (double dy : new double[] {0.0, 0.6, -0.6}) {
                    HitResult hit = level.clip(new ClipContext(eye, target.add(0, dy, 0), ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, player));
                    clear &= hit.getType() == HitResult.Type.MISS;
                }
                if (clear) {
                    return eye;
                }
            }
        }
        return target.add(0, 12, 4);
    }

    /** Ein untergegangenes Dorf (vor dem Untergang mindestens Stufe 2) und eine Stadt der Stufe 4. */
    private static void findVillages(ServerPlayer player) {
        ServerLevel present = player.server.overworld();
        var structures = present.registryAccess().registryOrThrow(Registries.STRUCTURE);
        HolderSet<Structure> villages = structures.getTag(StructureTags.VILLAGE).orElseThrow();
        long seed = present.getSeed();
        for (int i = 0; i < 40 && (ruin == null || city == null); i++) {
            double angle = i * 2.399963;
            BlockPos from = new BlockPos((int) (Math.cos(angle) * (300 + i * 220)), 64, (int) (Math.sin(angle) * (300 + i * 220)));
            Pair<BlockPos, Holder<Structure>> found = present.getChunkSource().getGenerator().findNearestMapStructure(present, villages, from, 8, false);
            if (found == null) {
                continue;
            }
            ChunkPos chunk = new ChunkPos(found.getFirst());
            Settlements.Outlook fallen = Settlements.outlook(seed, chunk, RUIN_YEARS);
            Settlements.Outlook grown = Settlements.outlook(seed, chunk, CITY_YEARS);
            LOGGER.info("Dorf {}: +{} {}, +{} {}", chunk, RUIN_YEARS, fallen, CITY_YEARS, grown);
            boolean plains = found.getSecond().is(net.minecraft.resources.ResourceKey.create(Registries.STRUCTURE,
                new net.minecraft.resources.ResourceLocation("village_plains")));
            if (ruin == null && fallen.fate() == Settlements.Fate.RUINED && fallen.stage() >= 1) {
                ruin = chunk;
            }
            if (city == null && grown.fate() == Settlements.Fate.THRIVING && grown.stage() == 4 && plains) {
                city = chunk;
            }
        }
        LOGGER.info("Ruine: {}, Stadt: {}", ruin, city);
    }

    private static Vec3 center(ServerLevel level, ChunkPos chunk) {
        if (chunk == null) {
            return new Vec3(0, 100, 0);
        }
        int y = level.getChunkSource().getGenerator().getBaseHeight(chunk.getMiddleBlockX(), chunk.getMiddleBlockZ(), Heightmap.Types.WORLD_SURFACE_WG,
            level, level.getChunkSource().randomState());
        return new Vec3(chunk.getMiddleBlockX() + 0.5, y, chunk.getMiddleBlockZ() + 0.5);
    }

    private static Optional<RuinGuardian> nearestGuardian(ServerPlayer player) {
        return nearest(player, RuinGuardian.class);
    }

    private static <T extends net.minecraft.world.entity.Entity> Optional<T> nearest(ServerPlayer player, Class<T> type) {
        return player.serverLevel().getEntitiesOfClass(type, new AABB(player.blockPosition()).inflate(96)).stream()
            .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
    }

    private static void faceGuardian(ServerPlayer player) {
        Optional<RuinGuardian> guardian = nearestGuardian(player);
        LOGGER.info("Ruinenwächter in der Nähe: {}", guardian.map(g -> g.blockPosition().toShortString()).orElse("keiner"));
        guardian.ifPresent(g -> {
            ServerLevel level = level(player, RUIN_YEARS);
            Vec3 target = g.position().add(0, 1.5, 0);
            Vec3 eye = frame(player, level, target, 5.5, 0.9);
            // Der Wächter steht der Kamera zugewandt (sein Auge soll man sehen)
            float yaw = (float) Math.toDegrees(Math.atan2(-(eye.x - target.x), eye.z - target.z));
            g.setNoAi(true);
            g.setYRot(yaw);
            g.setYHeadRot(yaw);
            g.setYBodyRot(yaw);
            look(player, level, eye, target);
        });
    }

    private static Optional<Villager> trader(ServerPlayer player) {
        return player.serverLevel().getEntitiesOfClass(Villager.class, new AABB(player.blockPosition()).inflate(128),
                villager -> villager.getVillagerData().getProfession() == ModVillagers.CHRONO_TRADER).stream()
            .min(Comparator.comparingDouble(v -> v.distanceToSqr(player)));
    }

    private static void faceTrader(ServerPlayer player) {
        Optional<Villager> trader = trader(player);
        LOGGER.info("Chrono-Händler: {}", trader.map(v -> v.blockPosition().toShortString()).orElse("keiner"));
        trader.ifPresent(villager -> {
            villager.setNoAi(true);
            ServerLevel level = level(player, CITY_YEARS);
            Vec3 target = villager.position().add(0, 1.4, 0);
            Vec3 eye = frame(player, level, target, 2.2, 0.25);
            // Der Händler schaut in die Kamera
            float yaw = (float) Math.toDegrees(Math.atan2(-(eye.x - target.x), eye.z - target.z));
            villager.setYRot(yaw);
            villager.setYHeadRot(yaw);
            villager.setYBodyRot(yaw);
            look(player, level, eye, target);
        });
    }

    private static void look(ServerPlayer player, ServerLevel level, Vec3 eye, Vec3 target) {
        double dx = target.x - eye.x;
        double dz = target.z - eye.z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(Math.atan2(eye.y - target.y, Math.sqrt(dx * dx + dz * dz)));
        player.teleportTo(level, eye.x, eye.y - player.getEyeHeight(), eye.z, yaw, pitch);
    }
}
