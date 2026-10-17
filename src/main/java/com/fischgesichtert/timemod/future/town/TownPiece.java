package com.fischgesichtert.timemod.future.town;

import com.fischgesichtert.timemod.worldgen.TimePiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import org.jetbrains.annotations.Nullable;

/** Gemeinsame Basis der Bauteile einer gewachsenen Siedlung: Stil (Dorfart und Stufe) und Bewohner. */
abstract class TownPiece extends TimePiece {
    /** Unter freiem Himmel echte Luft (Höhlenluft ist für Untergrund-Bauten). */
    protected static final BlockState OPEN = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
    protected final TownStyle style;
    /** Verlassen: Niemand wohnt mehr hier (die Ruinen verwittern, Bewohner gibt es keine). */
    protected boolean abandoned;

    protected TownPiece(StructurePieceType type, BoundingBox box, Direction orientation, long seed, TownStyle style) {
        super(type, box, orientation, seed);
        this.style = style;
    }

    protected TownPiece(StructurePieceType type, CompoundTag tag) {
        super(type, tag);
        this.style = TownStyle.of(tag.getString("Town"), tag.getInt("Stage"));
        this.abandoned = tag.getBoolean("Abandoned");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putString("Town", style.type());
        tag.putInt("Stage", style.stage());
        tag.putBoolean("Abandoned", abandoned);
    }

    /** Gewachsener Boden (oder ein alter Trampelpfad) – dort darf gepflastert und gemauert werden, nie auf Dächern. */
    protected static boolean natural(BlockState state) {
        return state.is(net.minecraft.tags.BlockTags.DIRT) || state.is(net.minecraft.tags.BlockTags.SAND) || state.is(net.minecraft.tags.BlockTags.SNOW)
            || state.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD) || state.is(net.minecraft.world.level.block.Blocks.GRAVEL)
            || state.is(net.minecraft.world.level.block.Blocks.DIRT_PATH) || state.is(net.minecraft.world.level.block.Blocks.CLAY)
            || state.is(net.minecraft.tags.BlockTags.ICE) || state.is(net.minecraft.tags.BlockTags.TERRACOTTA);
    }

    void abandon() {
        this.abandoned = true;
    }

    public boolean isAbandoned() {
        return abandoned;
    }

    protected BlockState state(net.minecraft.world.level.block.Block block) {
        return block.defaultBlockState();
    }

    /** Ein Dorfbewohner der passenden Art; nur in dem Chunk, der gerade entsteht (sonst gäbe es ihn doppelt). */
    protected void villager(WorldGenLevel level, BoundingBox box, int x, int y, int z, boolean child) {
        villager(level, box, x, y, z, child, null);
    }

    /** Ein Bewohner; mit {@code profession} schon im Beruf (mit etwas Erfahrung, damit er ihn behält). */
    protected void villager(WorldGenLevel level, BoundingBox box, int x, int y, int z, boolean child, @Nullable VillagerProfession profession) {
        BlockPos pos = getWorldPos(x, y, z);
        if (abandoned || !box.isInside(pos)) {
            return;
        }
        Villager villager = EntityType.VILLAGER.create(level.getLevel());
        if (villager == null) {
            return;
        }
        villager.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, chance(x, y, z, 77) * 360.0F, 0.0F);
        villager.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null, null);
        villager.setVillagerData(villager.getVillagerData().setType(villagerType()));
        if (profession != null && !child) {
            villager.setVillagerData(villager.getVillagerData().setProfession(profession));
            villager.setVillagerXp(1);
        }
        if (child) {
            villager.setAge(-24000);
        }
        villager.setPersistenceRequired();
        level.addFreshEntityWithPassengers(villager);
    }

    protected void golem(WorldGenLevel level, BoundingBox box, int x, int y, int z) {
        BlockPos pos = getWorldPos(x, y, z);
        if (abandoned || !box.isInside(pos)) {
            return;
        }
        IronGolem golem = EntityType.IRON_GOLEM.create(level.getLevel());
        if (golem == null) {
            return;
        }
        golem.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, chance(x, y, z, 78) * 360.0F, 0.0F);
        golem.setPersistenceRequired();
        level.addFreshEntityWithPassengers(golem);
    }

    private VillagerType villagerType() {
        return switch (style.type()) {
            case "desert" -> VillagerType.DESERT;
            case "savanna" -> VillagerType.SAVANNA;
            case "snowy" -> VillagerType.SNOW;
            case "taiga" -> VillagerType.TAIGA;
            default -> VillagerType.PLAINS;
        };
    }
}
