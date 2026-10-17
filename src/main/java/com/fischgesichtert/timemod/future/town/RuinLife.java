package com.fischgesichtert.timemod.future.town;

import com.fischgesichtert.timemod.entity.MossBear;
import com.fischgesichtert.timemod.entity.RuinGuardian;
import com.fischgesichtert.timemod.registry.ModEntities;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Leben in den Ruinen der Zukunft. Aus den Eisengolems untergegangener Dörfer sind Ruinenwächter geworden
 * (in Plünderer-Lagern haben sie nicht überlebt); dazu wachen je nach früherer Größe der Siedlung ein bis
 * drei weitere über ihren alten Dorfplatz. In Ruinen, die schon über hundert Jahre verlassen sind, leben
 * manchmal Moosbären. Alles ist aus Seed, Dorf und Jahr bestimmt.
 */
public final class RuinLife {
    private RuinLife() {
    }

    /** Bewohner eines verlassenen Dorf-Chunks: wird von der Verwitterung aufgerufen. */
    public static void populate(WorldGenLevel level, ChunkAccess chunk, Settlements.Presence presence, long years) {
        if (chunk instanceof ProtoChunk proto) {
            proto.getEntities().removeIf(tag -> {
                String id = tag.getString("id");
                if (id.equals("minecraft:iron_golem")) {
                    if (presence.raided()) {
                        return true;
                    }
                    // Der Golem ist geblieben – und mit der Ruine verwachsen
                    tag.putString("id", "timemod:ruin_guardian");
                    tag.putBoolean("PlayerCreated", false);
                    return false;
                }
                return id.equals("minecraft:villager") || id.equals("minecraft:cat");
            });
        }
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (Map.Entry<Structure, StructureStart> entry : chunk.getAllStarts().entrySet()) {
            Holder<Structure> holder = registry.wrapAsHolder(entry.getKey());
            if (!holder.is(StructureTags.VILLAGE) || !entry.getValue().isValid()) {
                continue;
            }
            ChunkPos start = chunk.getPos();
            Settlements.Outlook outlook = Settlements.outlook(level.getSeed(), start, years);
            if (outlook.fate() != Settlements.Fate.RUINED) {
                continue;
            }
            RandomSource random = RandomSource.create(level.getSeed() ^ start.toLong() * 0x2545F4914F6CDD1DL ^ years);
            int guardians = 1 + outlook.stage() / 2;
            for (int i = 0; i < guardians; i++) {
                BlockPos spot = spot(level, chunk.getPos(), random);
                if (spot != null) {
                    guardian(level, spot, random);
                }
            }
            if (outlook.abandonedFor(years) >= MossBear.FROM_YEARS && random.nextFloat() < 0.4F) {
                BlockPos spot = spot(level, chunk.getPos(), random);
                if (spot != null) {
                    bear(level, spot, random, false);
                    if (random.nextFloat() < 0.5F) {
                        bear(level, spot, random, true);
                    }
                }
            }
        }
    }

    /** Ein Platz im Chunk mit festem Boden und drei freien Blöcken darüber, nicht im Wasser. */
    @Nullable
    static BlockPos spot(WorldGenLevel level, ChunkPos chunk, RandomSource random) {
        for (int attempt = 0; attempt < 12; attempt++) {
            int x = chunk.getMinBlockX() + 2 + random.nextInt(12);
            int z = chunk.getMinBlockZ() + 2 + random.nextInt(12);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos feet = new BlockPos(x, y, z);
            BlockState ground = level.getBlockState(feet.below());
            if (!ground.getFluidState().isEmpty() || !ground.isFaceSturdy(level, feet.below(), Direction.UP)) {
                continue;
            }
            boolean free = true;
            for (int i = 0; i < 3 && free; i++) {
                BlockState above = level.getBlockState(feet.above(i));
                free = above.getFluidState().isEmpty() && above.getCollisionShape(level, feet.above(i)).isEmpty();
            }
            if (free) {
                return feet;
            }
        }
        return null;
    }

    private static void guardian(WorldGenLevel level, BlockPos spot, RandomSource random) {
        RuinGuardian guardian = ModEntities.RUIN_GUARDIAN.create(level.getLevel());
        if (guardian == null) {
            return;
        }
        guardian.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
        guardian.setPost(spot);
        guardian.setPersistenceRequired();
        level.addFreshEntity(guardian);
    }

    private static void bear(WorldGenLevel level, BlockPos spot, RandomSource random, boolean cub) {
        MossBear bear = ModEntities.MOSS_BEAR.create(level.getLevel());
        if (bear == null) {
            return;
        }
        bear.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
        bear.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), MobSpawnType.STRUCTURE, null, (CompoundTag) null);
        bear.setAge(cub ? -24000 : 0);
        bear.setPersistenceRequired();
        level.addFreshEntity(bear);
    }
}
