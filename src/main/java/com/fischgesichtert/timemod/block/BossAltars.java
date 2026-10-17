package com.fischgesichtert.timemod.block;

import com.fischgesichtert.timemod.energy.Chronon;
import com.fischgesichtert.timemod.entity.boss.ChronophageBoss;
import com.fischgesichtert.timemod.entity.boss.ClockmakerBoss;
import com.fischgesichtert.timemod.entity.boss.ParadoxBoss;
import com.fischgesichtert.timemod.registry.ModBlocks;
import com.fischgesichtert.timemod.registry.ModEntities;
import com.fischgesichtert.timemod.registry.ModItems;
import com.fischgesichtert.timemod.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Erwecken der Bosse: Am Uhrwerk-Altar genügt ein Uhrwerk, der Phagen-Altar verlangt vier
 * Zeitkern-Fragmente, der Spiegel-Altar eine geladene Paradox-Zeitzelle – Paradox wird zum Echo
 * dessen, der sie opfert. Solange der Boss in der Nähe lebt, ruht der Altar.
 */
public final class BossAltars {
    public static final int FRAGMENTS = 4;
    /** Der Spiegel-Altar verlangt eine Paradox-Zeitzelle mit mindestens so viel Chronon. */
    public static final int PARADOX_CHARGE = 50_000;

    private BossAltars() {
    }

    static InteractionResult tryAwaken(BossAltarBlock altar, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        AABB nearby = new AABB(pos).inflate(48);
        if (state.is(ModBlocks.CLOCKMAKER_ALTAR) && held.is(ModItems.CLOCKWORK)) {
            if (!server.getEntitiesOfClass(ClockmakerBoss.class, nearby).isEmpty()) {
                return busy(player);
            }
            consume(player, held, 1);
            spawn(server, ModEntities.CLOCKMAKER.create(server), Vec3.atBottomCenterOf(pos).add(0, 1.0, 3.0), player,
                "boss.timemod.clockmaker.awakens");
            return InteractionResult.CONSUME;
        }
        if (state.is(ModBlocks.PHAGE_ALTAR) && held.is(ModItems.TIME_CORE_FRAGMENT)) {
            if (held.getCount() < FRAGMENTS && !player.getAbilities().instabuild) {
                player.displayClientMessage(Component.translatable("message.timemod.altar.phage.more", FRAGMENTS).withStyle(ChatFormatting.DARK_PURPLE), true);
                return InteractionResult.CONSUME;
            }
            if (!server.getEntitiesOfClass(ChronophageBoss.class, nearby).isEmpty()) {
                return busy(player);
            }
            consume(player, held, FRAGMENTS);
            spawn(server, ModEntities.CHRONOPHAGE.create(server), Vec3.atBottomCenterOf(pos).add(0, 3.0, 0), player,
                "boss.timemod.chronophage.awakens");
            return InteractionResult.CONSUME;
        }
        if (state.is(ModBlocks.PARADOX_ALTAR) && held.is(ModItems.PARADOX_TIME_CELL)) {
            if (Chronon.stored(held) < PARADOX_CHARGE && !player.getAbilities().instabuild) {
                player.displayClientMessage(Component.translatable("message.timemod.altar.paradox.charge", Chronon.format(PARADOX_CHARGE))
                    .withStyle(ChatFormatting.DARK_PURPLE), true);
                return InteractionResult.CONSUME;
            }
            if (!server.getEntitiesOfClass(ParadoxBoss.class, nearby).isEmpty()) {
                return busy(player);
            }
            consume(player, held, 1);
            ParadoxBoss boss = ModEntities.PARADOX.create(server);
            if (boss != null) {
                boss.becomeEchoOf(player);
            }
            spawn(server, boss, Vec3.atBottomCenterOf(pos).add(0, 1.0, 4.0), player, "boss.timemod.paradox.awakens");
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    private static InteractionResult busy(Player player) {
        player.displayClientMessage(Component.translatable("message.timemod.altar.busy").withStyle(ChatFormatting.GRAY), true);
        return InteractionResult.CONSUME;
    }

    private static void consume(Player player, ItemStack stack, int count) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(count);
        }
    }

    private static void spawn(ServerLevel level, Mob boss, Vec3 at, Player summoner, String titleKey) {
        if (boss == null) {
            return;
        }
        boss.moveTo(at.x, at.y, at.z, summoner.getYRot() + 180.0F, 0.0F);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(boss.blockPosition()), MobSpawnType.TRIGGERED, null, null);
        level.addFreshEntity(boss);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 1, at.z, 200, 1.5, 1.5, 1.5, 0.3);
        level.sendParticles(ParticleTypes.FLASH, at.x, at.y + 1, at.z, 1, 0, 0, 0, 0);
        level.playSound(null, at.x, at.y, at.z, ModSounds.BOSS_AWAKEN, SoundSource.HOSTILE, 4.0F, 1.0F);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(BlockPos.containing(at)).inflate(48))) {
            player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable(titleKey).withStyle(ChatFormatting.GOLD)));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable(titleKey + ".hint").withStyle(ChatFormatting.GRAY)));
        }
    }
}
