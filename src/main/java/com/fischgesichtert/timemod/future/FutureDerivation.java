package com.fischgesichtert.timemod.future;

import com.fischgesichtert.timemod.era.Era;
import com.fischgesichtert.timemod.era.Eras;
import com.fischgesichtert.timemod.registry.ModAdvancements;
import com.fischgesichtert.timemod.registry.ModBlocks;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * Die Zukunft entsteht aus der Gegenwart: Wird in einer Zukunfts-Epoche ein Chunk geladen, den Spieler
 * in der Gegenwart verändert haben, wird er am Ende des Ticks aus der Gegenwart kopiert und gealtert –
 * Blöcke, Truhen, Schilder, Tiere, und auf dem freien Boden wächst der Wald der Zukunft nach. Ändert sich die Gegenwart, rechnet die Zukunft beim nächsten Besuch
 * neu, außer dort, wo jemand in der Zukunft selbst gebaut hat.
 */
public final class FutureDerivation {
    /** So viel Zeit je Tick darf die Zukunft sich nehmen (mindestens ein Chunk), damit eine große Basis nicht ruckelt. */
    private static final long BUDGET_NANOS = 15_000_000L;
    private static final int CENSUS_INTERVAL = 400;
    /** Ab so viel Spielzeit in einem Chunk (Ticks) gilt er als bewohnt – und damit als Teil der Zukunft. */
    private static final long LIVED_IN = 1200;
    private static final long SALT = 0xB0_4E5L;
    /** So lange leben gehaltene Tiere noch; danach bleiben Knochen. */
    private static final int ANIMAL_LIFETIME = 15;
    private static final Heightmap.Types[] HEIGHTMAPS = {Heightmap.Types.MOTION_BLOCKING, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
        Heightmap.Types.OCEAN_FLOOR, Heightmap.Types.WORLD_SURFACE};
    private static final Map<ResourceKey<Level>, LongLinkedOpenHashSet> DUE = new HashMap<>();

    private FutureDerivation() {
    }

    public static void init() {
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk) -> {
            Era era = Eras.byLevel(level.dimension());
            if (era != null && era.isFuture() && needsDerivation(level, chunk.getPos().toLong())) {
                DUE.computeIfAbsent(level.dimension(), key -> new LongLinkedOpenHashSet()).add(chunk.getPos().toLong());
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(FutureDerivation::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> DUE.clear());
    }

    private static boolean needsDerivation(ServerLevel future, long chunk) {
        long revision = PresentChanges.get(future.getServer()).revision(chunk);
        return revision > 0 && FutureChunks.get(future).needs(chunk, revision);
    }

    /** Ob der Chunk fertig ist (nichts mehr aus der Gegenwart zu übernehmen) – der Zeitstrom landet erst dann. */
    public static boolean isSettled(ServerLevel level, ChunkPos chunk) {
        LongLinkedOpenHashSet due = DUE.get(level.dimension());
        return due == null || !due.contains(chunk.toLong());
    }

    private static void tick(MinecraftServer server) {
        long start = System.nanoTime();
        boolean first = true;
        for (Map.Entry<ResourceKey<Level>, LongLinkedOpenHashSet> entry : DUE.entrySet()) {
            ServerLevel level = server.getLevel(entry.getKey());
            LongLinkedOpenHashSet due = entry.getValue();
            if (level == null) {
                due.clear();
                continue;
            }
            while (!due.isEmpty() && (first || System.nanoTime() - start < BUDGET_NANOS)) {
                long key = due.removeFirstLong();
                LevelChunk chunk = level.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key));
                if (chunk != null && needsDerivation(level, key)) {
                    derive(level, chunk);
                    first = false;
                }
            }
        }
        if (server.getTickCount() % CENSUS_INTERVAL == 0) {
            census(server);
        }
        if (server.getTickCount() % 40 == 0) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                Era era = Eras.byLevel(player.level().dimension());
                if (era != null && era.isFuture() && FutureChunks.get(player.serverLevel()).isDerived(player.chunkPosition().toLong())) {
                    ModAdvancements.award(player, "remains");
                }
            }
        }
    }

    /** Leitet einen Zukunfts-Chunk aus der Gegenwart ab (öffentlich für Befehle und Tests). */
    public static void derive(ServerLevel future, LevelChunk target) {
        Era era = Eras.byLevel(future.dimension());
        if (era == null || !era.isFuture()) {
            return;
        }
        MinecraftServer server = future.getServer();
        ChunkPos pos = target.getPos();
        long years = era.years();
        LevelChunk source = server.overworld().getChunk(pos.x, pos.z);
        WeatheringRules.Resolved rules = WeatheringRules.get();
        WorkCanvas canvas = new WorkCanvas(future, source);
        Weathering.apply(canvas, years, rules);
        PresentChanges changes = PresentChanges.get(server);
        List<PresentChanges.Animal> animals = changes.animals(pos.toLong());
        if (years >= ANIMAL_LIFETIME) {
            bones(canvas, animals, years, rules.reclaim());
        }
        // Wo die Zukunft Wald ist, wächst er auch auf dem freien Boden nach
        Succession.apply(canvas, years, rules);
        write(future, target, canvas);
        blockEntities(future, target, source, canvas, years, rules.reclaim());
        FutureChunks memory = FutureChunks.get(future);
        if (!animals.isEmpty() && memory.populate(pos.toLong())) {
            descendants(future, animals, years, rules.reclaim());
        }
        memory.derived(pos.toLong(), changes.revision(pos.toLong()));
    }

    /** Schreibt nur, was sich unterscheidet – direkt in die Abschnitte: keine Drops, keine Nachbar-Updates, keine Zündung. */
    private static void write(ServerLevel level, LevelChunk chunk, WorkCanvas canvas) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        LevelChunkSection[] sections = chunk.getSections();
        for (int index = 0; index < sections.length; index++) {
            int baseY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(index));
            LevelChunkSection section = sections[index];
            for (int y = 0; y < 16; y++) {
                int worldY = baseY + y;
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState wanted = canvas.local(x, worldY, z);
                        if (section.getBlockState(x, y, z) != wanted) {
                            pos.set(chunk.getPos().getMinBlockX() + x, worldY, chunk.getPos().getMinBlockZ() + z);
                            set(level, chunk, section, pos, wanted);
                        }
                    }
                }
            }
        }
    }

    private static void set(ServerLevel level, LevelChunk chunk, LevelChunkSection section, BlockPos pos, BlockState state) {
        boolean wasEmpty = section.hasOnlyAir();
        int x = pos.getX() & 15;
        int y = pos.getY();
        int z = pos.getZ() & 15;
        BlockState old = section.setBlockState(x, y & 15, z, state);
        for (Heightmap.Types type : HEIGHTMAPS) {
            chunk.getOrCreateHeightmapUnprimed(type).update(x, y, z, state);
        }
        boolean empty = section.hasOnlyAir();
        if (wasEmpty != empty) {
            level.getChunkSource().getLightEngine().updateSectionStatus(pos, empty);
        }
        if (LightEngine.hasDifferentLightProperties(chunk, pos, old, state)) {
            chunk.getSkyLightSources().update(chunk, x, y, z);
            level.getChunkSource().getLightEngine().checkBlock(pos);
        }
        if (old.hasBlockEntity()) {
            chunk.removeBlockEntity(pos);
        }
        level.getChunkSource().blockChanged(pos);
        level.onBlockStateChange(pos.immutable(), old, state);
        chunk.setUnsaved(true);
    }

    /** Truhen, Schilder & Co.: Daten aus der Gegenwart, gealtert; sonst ein frisches Block-Entity. */
    private static void blockEntities(ServerLevel level, LevelChunk chunk, LevelChunk source, WorkCanvas canvas, long years,
                                      Reclaim.Settings settings) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = canvas.minY(); y < canvas.maxY(); y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    BlockState state = canvas.local(x, y, z);
                    if (!state.hasBlockEntity()) {
                        continue;
                    }
                    pos.set(chunk.getPos().getMinBlockX() + x, y, chunk.getPos().getMinBlockZ() + z);
                    BlockPos at = pos.immutable();
                    BlockEntity present = source.getBlockEntity(at, LevelChunk.EntityCreationType.CHECK);
                    BlockEntity existing = chunk.getBlockEntity(at, LevelChunk.EntityCreationType.CHECK);
                    CompoundTag tag = present != null && present.getBlockState().is(state.getBlock()) ? present.saveWithFullMetadata() : null;
                    RandomSource random = Weathering.random(canvas.seed(), SALT, at.getX(), at.getY(), at.getZ());
                    BlockState aged = tag == null ? null : BlockEntityAging.age(tag, state, present.getType(), level, years, random, settings);
                    if (aged != null) {
                        if (aged != state) {
                            set(level, chunk, chunk.getSection(chunk.getSectionIndex(y)), at, aged);
                            state = aged;
                        }
                        BlockEntity copy = BlockEntity.loadStatic(at, state, tag);
                        if (copy != null) {
                            if (existing != null) {
                                chunk.removeBlockEntity(at);
                            }
                            chunk.addAndRegisterBlockEntity(copy);
                            level.getChunkSource().blockChanged(at);
                        }
                    } else if (existing == null && state.getBlock() instanceof EntityBlock entityBlock) {
                        BlockEntity fresh = entityBlock.newBlockEntity(at, state);
                        if (fresh != null) {
                            chunk.addAndRegisterBlockEntity(fresh);
                        }
                    }
                }
            }
        }
    }

    /** Von den alten Tieren bleiben Knochen, wo sie zuletzt standen. */
    private static void bones(WorkCanvas canvas, List<PresentChanges.Animal> animals, long years, Reclaim.Settings settings) {
        double chance = settings.bones().at(years);
        for (PresentChanges.Animal animal : animals) {
            BlockPos at = animal.pos();
            if (!canvas.inside(at.getX(), at.getZ())) {
                continue;
            }
            RandomSource random = Weathering.random(canvas.seed(), SALT ^ 0xB0E5L, at.getX(), at.getY(), at.getZ());
            if (random.nextFloat() >= chance) {
                continue;
            }
            for (int dy = 2; dy >= -3; dy--) {
                int y = at.getY() + dy;
                BlockState here = canvas.at(at.getX(), y, at.getZ());
                BlockState below = canvas.at(at.getX(), y - 1, at.getZ());
                if ((here.isAir() || here.canBeReplaced()) && !below.isAir() && below.isFaceSturdy(EmptyBlockGetter.INSTANCE,
                    BlockPos.ZERO, Direction.UP)) {
                    canvas.set(at.getX(), y, at.getZ(), ModBlocks.BONE_PILE.defaultBlockState());
                    break;
                }
            }
        }
    }

    /** Die Nachkommen: Nach wenigen Jahren leben die Tiere noch; später streifen ihre Nachfahren verwildert umher. */
    private static void descendants(ServerLevel level, List<PresentChanges.Animal> animals, long years, Reclaim.Settings settings) {
        double chance = years < ANIMAL_LIFETIME ? 1.0 : settings.descendants().at(years);
        for (PresentChanges.Animal animal : animals) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(animal.type());
            RandomSource random = Weathering.random(level.getSeed(), SALT ^ 0xD35CL, animal.pos().getX(), animal.pos().getY(), animal.pos().getZ());
            if (random.nextFloat() >= chance) {
                continue;
            }
            BlockPos spot = years < ANIMAL_LIFETIME ? freeAt(level, animal.pos()) : wildSpot(level, animal.pos(), random);
            if (spot == null) {
                continue;
            }
            Entity entity = type.create(level);
            if (entity == null) {
                continue;
            }
            entity.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
            if (entity instanceof AgeableMob ageable && years >= ANIMAL_LIFETIME && random.nextFloat() < 0.3F) {
                ageable.setAge(-24000);
            }
            if (entity instanceof Mob mob) {
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), MobSpawnType.NATURAL, null, null);
            }
            level.addFreshEntity(entity);
        }
    }

    /** Wenige Jahre später steht das Tier noch dort, wo es war – sofern dort Platz ist. */
    @Nullable
    private static BlockPos freeAt(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() ? pos : null;
    }

    /** Verwildert: ein freier Platz auf festem Boden in der Nähe (nie auf Zäunen oder Knochen), notfalls näher. */
    @Nullable
    private static BlockPos wildSpot(ServerLevel level, BlockPos origin, RandomSource random) {
        for (int attempt = 0; attempt < 6; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = attempt < 4 ? 6 + random.nextInt(8) : 2 + random.nextInt(4);
            int x = origin.getX() + (int) Math.round(Math.cos(angle) * distance);
            int z = origin.getZ() + (int) Math.round(Math.sin(angle) * distance);
            if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) {
                continue;
            }
            BlockPos spot = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            BlockState ground = level.getBlockState(spot.below());
            if (ground.isFaceSturdy(level, spot.below(), Direction.UP) && !ground.is(BlockTags.LEAVES) && ground.getFluidState().isEmpty()
                && level.getBlockState(spot).getCollisionShape(level, spot).isEmpty()) {
                return spot;
            }
        }
        return null;
    }

    /**
     * Rund um jeden Spieler der Gegenwart: Chunks, in denen schon länger gespielt wurde, gelten als verändert
     * (so kennt die Zukunft auch Bauten aus der Zeit vor dieser Version), und die gehaltenen Tiere werden gezählt.
     */
    private static void census(MinecraftServer server) {
        ServerLevel present = server.overworld();
        PresentChanges changes = PresentChanges.get(server);
        for (ServerPlayer player : present.players()) {
            if (player.isSpectator()) {
                continue;
            }
            ChunkPos center = player.chunkPosition();
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    ChunkPos chunk = new ChunkPos(center.x + dx, center.z + dz);
                    LevelChunk loaded = present.getChunkSource().getChunkNow(chunk.x, chunk.z);
                    if (loaded == null) {
                        continue;
                    }
                    if (changes.revision(chunk.toLong()) == 0 && loaded.getInhabitedTime() > LIVED_IN) {
                        changes.mark(chunk, present.getGameTime());
                    }
                    if (changes.revision(chunk.toLong()) > 0) {
                        countAnimals(present, chunk);
                    }
                }
            }
        }
    }

    /** Zählt die gehaltenen Tiere eines Chunks der Gegenwart neu (die alte Zählung wird ersetzt). */
    public static void countAnimals(ServerLevel present, ChunkPos chunk) {
        AABB area = new AABB(chunk.getMinBlockX(), present.getMinBuildHeight(), chunk.getMinBlockZ(), chunk.getMaxBlockX() + 1,
            present.getMaxBuildHeight(), chunk.getMaxBlockZ() + 1);
        List<PresentChanges.Animal> found = new ArrayList<>();
        for (Animal animal : present.getEntitiesOfClass(Animal.class, area, FutureDerivation::kept)) {
            found.add(new PresentChanges.Animal(BuiltInRegistries.ENTITY_TYPE.getKey(animal.getType()), animal.blockPosition()));
        }
        PresentChanges.get(present.getServer()).count(chunk.toLong(), found);
    }

    /** Ein gehaltenes Tier: angeleint, mit Namen, oder mit einem Zaun ganz in der Nähe. */
    private static boolean kept(Animal animal) {
        if (animal.isLeashed() || animal.hasCustomName()) {
            return true;
        }
        BlockPos feet = animal.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-5, -1, -5), feet.offset(5, 1, 5))) {
            BlockState state = animal.level().getBlockState(pos);
            if (state.is(BlockTags.FENCES) || state.is(BlockTags.FENCE_GATES) || state.is(BlockTags.WALLS)) {
                return true;
            }
        }
        return false;
    }
}
