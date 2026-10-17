package com.fischgesichtert.timemod.gametest;

import com.fischgesichtert.timemod.energy.Chronon;
import com.fischgesichtert.timemod.entity.ThrownChronoKnife;
import com.fischgesichtert.timemod.entity.boss.ChronophageBoss;
import com.fischgesichtert.timemod.entity.boss.ClockmakerBoss;
import com.fischgesichtert.timemod.entity.boss.ParadoxBoss;
import com.fischgesichtert.timemod.registry.ModBlocks;
import com.fischgesichtert.timemod.registry.ModEntities;
import com.fischgesichtert.timemod.registry.ModItems;
import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.TimeFilter;
import com.fischgesichtert.timemod.time.ZoneShape;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Bosse: Zeitsprung und Schwachstelle des Uhrmachers, Rückspul-Heilung des Chronophagen, Paradox, Altäre. */
public class BossTests implements FabricGameTest {
    private static final String ARENA = "timemod-gametest:arena";

    @GameTest(template = ARENA, timeoutTicks = 200, setupTicks = 20, batch = "timemod_bosses")
    public void clockmakerJumpsBackUnlessSlowed(GameTestHelper helper) {
        ClockmakerBoss boss = helper.spawn(ModEntities.CLOCKMAKER, new Vec3(12.5, 2, 8.5));
        boss.setNoAi(true);
        float max = boss.getMaxHealth();
        ServerTimeEngine engine = ServerTimeEngine.of(helper.getLevel());
        helper.runAtTickTime(70, () -> {
            // 40 Schaden → gepanzert 20 → über der Schwelle: Zeitsprung, alles wieder heil
            boss.hurt(helper.getLevel().damageSources().generic(), 40.0F);
            if (boss.getHealth() != max) {
                helper.fail("Nach dem Zeitsprung muss der Uhrmacher wieder volles Leben haben, hat " + boss.getHealth());
            }
            if (boss.jumpCooldown() <= 0) {
                helper.fail("Der Zeitsprung muss eine Abklingzeit auslösen");
            }
            // Während der Abklingzeit: halber Schaden, kein Sprung
            boss.invulnerableTime = 0;
            boss.hurt(helper.getLevel().damageSources().generic(), 20.0F);
            if (Math.abs(boss.getHealth() - (max - 20.0F * ClockmakerBoss.ARMOR_FACTOR)) > 0.01F) {
                helper.fail("Gepanzert muss nur die Hälfte ankommen: Leben " + boss.getHealth());
            }
            // Zeitstopp hält ihn nicht an
            engine.addZone(new ZoneShape.Sphere(boss.position(), 4), 0.0, TimeFilter.DEFAULT, null, 20);
        });
        helper.runAtTickTime(73, () -> {
            if (engine.isFrozen(boss)) {
                helper.fail("Den Uhrmacher kann man nicht anhalten");
            }
        });
        helper.runAtTickTime(100, () -> engine.addZone(new ZoneShape.Sphere(boss.position(), 4), 0.5, TimeFilter.DEFAULT, null, 80));
        helper.runAtTickTime(110, () -> {
            if (!boss.isWeakSpotExposed()) {
                helper.fail("In Zeitlupe muss die Schwachstelle offen liegen");
            }
            float before = boss.getHealth();
            boss.invulnerableTime = 0;
            boss.hurt(helper.getLevel().damageSources().generic(), 10.0F);
            if (Math.abs(before - boss.getHealth() - 10.0F * ClockmakerBoss.WEAK_SPOT_FACTOR) > 0.01F) {
                helper.fail("Die offene Schwachstelle muss doppelten Schaden nehmen: " + (before - boss.getHealth()));
            }
            boss.discard();
            engine.clear();
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 120, setupTicks = 20, batch = "timemod_bosses_phage")
    public void chronophageOnlyBleedsInStoppedTime(GameTestHelper helper) {
        ChronophageBoss boss = helper.spawn(ModEntities.CHRONOPHAGE, new Vec3(12.5, 4, 8.5));
        boss.setNoAi(true);
        float max = boss.getMaxHealth();
        ServerTimeEngine engine = ServerTimeEngine.of(helper.getLevel());
        helper.runAtTickTime(5, () -> {
            boss.setMouth(ChronophageBoss.Mouth.CLOSED, 1000);
            boss.hurt(helper.getLevel().damageSources().generic(), 50.0F);
            if (Math.abs(boss.getHealth() - (max - 50.0F * ChronophageBoss.CLOSED_FACTOR)) > 0.01F) {
                helper.fail("Geschlossen darf nur ein Zehntel ankommen: " + boss.getHealth());
            }
            boss.rewindWounds();
            boss.setMouth(ChronophageBoss.Mouth.OPEN, 1000);
            boss.invulnerableTime = 0;
            boss.hurt(helper.getLevel().damageSources().generic(), 20.0F);
            boss.rewindWounds();
            if (boss.getHealth() != max) {
                helper.fail("Gewöhnliche Wunden spult er zurück: Leben " + boss.getHealth());
            }
            // Jetzt die Zeit anhalten, während das Maul offen ist, und zweimal zuschlagen
            engine.addZone(new ZoneShape.Sphere(boss.position(), 5), 0.0, TimeFilter.DEFAULT, null, 20);
        });
        helper.runAtTickTime(8, () -> {
            boss.hurt(helper.getLevel().damageSources().generic(), 30.0F);
            boss.hurt(helper.getLevel().damageSources().generic(), 30.0F);
            if (boss.getHealth() != max) {
                helper.fail("Im Stillstand darf noch nichts ankommen");
            }
        });
        helper.runAtTickTime(40, () -> {
            if (Math.abs(boss.getHealth() - (max - 60.0F)) > 0.01F) {
                helper.fail("Nach dem Stopp müssen beide Treffer voll wirken: Leben " + boss.getHealth());
            }
            boss.rewindWounds();
            if (Math.abs(boss.getHealth() - (max - 60.0F)) > 0.01F) {
                helper.fail("Wunden aus dem Stillstand darf er nicht zurückspulen: Leben " + boss.getHealth());
            }
            boss.discard();
            engine.clear();
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 40, setupTicks = 5, batch = "timemod_bosses_altar")
    public void altarsAwakenTheirBosses(GameTestHelper helper) {
        BlockPos clockAltar = new BlockPos(6, 2, 8);
        BlockPos phageAltar = new BlockPos(30, 2, 8);
        helper.setBlock(clockAltar, ModBlocks.CLOCKMAKER_ALTAR);
        helper.setBlock(phageAltar, ModBlocks.PHAGE_ALTAR);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 near = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(18, 2, 8)));
        player.teleportTo(near.x, near.y, near.z);

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TIME_CORE_FRAGMENT, 3));
        use(helper, player, phageAltar);
        if (!helper.getLevel().getEntitiesOfClass(ChronophageBoss.class, new AABB(helper.absolutePos(phageAltar)).inflate(10)).isEmpty()
            || player.getMainHandItem().getCount() != 3) {
            helper.fail("Mit drei Fragmenten darf der Chronophage nicht erwachen");
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TIME_CORE_FRAGMENT, 5));
        use(helper, player, phageAltar);
        if (helper.getLevel().getEntitiesOfClass(ChronophageBoss.class, new AABB(helper.absolutePos(phageAltar)).inflate(10)).size() != 1
            || player.getMainHandItem().getCount() != 1) {
            helper.fail("Vier Fragmente müssen den Chronophagen wecken (und verbraucht werden)");
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CLOCKWORK, 2));
        use(helper, player, clockAltar);
        use(helper, player, clockAltar);
        var clockmakers = helper.getLevel().getEntitiesOfClass(ClockmakerBoss.class, new AABB(helper.absolutePos(clockAltar)).inflate(10));
        if (clockmakers.size() != 1 || player.getMainHandItem().getCount() != 1) {
            helper.fail("Ein Uhrwerk weckt genau einen Uhrmacher (zweiter Versuch: Altar gebunden), gefunden " + clockmakers.size());
        }
        helper.getLevel().getEntitiesOfClass(ClockmakerBoss.class, new AABB(helper.absolutePos(clockAltar)).inflate(10)).forEach(e -> e.discard());
        helper.getLevel().getEntitiesOfClass(ChronophageBoss.class, new AABB(helper.absolutePos(phageAltar)).inflate(10)).forEach(e -> e.discard());
        player.discard();
        helper.succeed();
    }

    /** Der Spiegel-Altar will eine geladene Paradox-Zelle; Paradox trägt dann Kopien deiner Rüstung und Waffe. */
    @GameTest(template = ARENA, timeoutTicks = 40, setupTicks = 5, batch = "timemod_paradox_altar")
    public void mirrorAltarSummonsYourEcho(GameTestHelper helper) {
        BlockPos altar = new BlockPos(12, 2, 8);
        helper.setBlock(altar, ModBlocks.PARADOX_ALTAR);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 near = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(12, 2, 4)));
        player.teleportTo(near.x, near.y, near.z);
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        player.getInventory().setItem(4, new ItemStack(Items.DIAMOND_SWORD));
        ItemStack weak = new ItemStack(ModItems.PARADOX_TIME_CELL);
        Chronon.set(weak, 10_000);
        player.setItemInHand(InteractionHand.MAIN_HAND, weak);
        use(helper, player, altar);
        AABB area = new AABB(helper.absolutePos(altar)).inflate(12);
        if (!helper.getLevel().getEntitiesOfClass(ParadoxBoss.class, area).isEmpty() || player.getMainHandItem().isEmpty()) {
            helper.fail("Mit nur 10k Chronon darf der Spiegel nichts wecken");
        }
        ItemStack full = new ItemStack(ModItems.PARADOX_TIME_CELL);
        Chronon.set(full, 60_000);
        player.setItemInHand(InteractionHand.MAIN_HAND, full);
        use(helper, player, altar);
        var bosses = helper.getLevel().getEntitiesOfClass(ParadoxBoss.class, area);
        if (bosses.size() != 1 || !player.getMainHandItem().isEmpty()) {
            helper.fail("Eine volle Paradox-Zelle weckt genau ein Paradox und wird verbraucht");
            return;
        }
        ParadoxBoss boss = bosses.get(0);
        if (!boss.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE) || !boss.getMainHandItem().is(Items.DIAMOND_SWORD)) {
            helper.fail("Paradox muss Rüstung und beste Waffe kopieren, trägt " + boss.getItemBySlot(EquipmentSlot.CHEST) + " / " + boss.getMainHandItem());
        }
        if (!player.getUUID().equals(boss.echoOf()) || player.getInventory().getItem(4).isEmpty()) {
            helper.fail("Paradox ist das Echo des Spielers – und die Originale bleiben beim Spieler");
        }
        boss.discard();
        player.discard();
        helper.succeed();
    }

    /** Paradox hält die Zeit an (selbst bleibt es beweglich); der Messerkranz hängt in der Luft, bis die Zeit weiterläuft. */
    @GameTest(template = ARENA, timeoutTicks = 120, setupTicks = 5, batch = "timemod_paradox_stop")
    public void paradoxStopsTimeWithKnifeRing(GameTestHelper helper) {
        ParadoxBoss boss = helper.spawn(ModEntities.PARADOX, new Vec3(12.5, 2, 8.5));
        boss.setNoAi(true);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(12, 2, 14)));
        player.teleportTo(at.x, at.y, at.z);
        ServerTimeEngine engine = ServerTimeEngine.of(helper.getLevel());
        boss.timeStop();
        AABB area = new AABB(helper.absolutePos(new BlockPos(12, 3, 11))).inflate(16);
        java.util.List<Vec3> frozenAt = new java.util.ArrayList<>();
        StringBuilder problems = new StringBuilder();
        helper.runAfterDelay(3, () -> {
            if (engine.zones().stream().noneMatch(z -> boss.getUUID().equals(z.owner()) && z.isStop())) {
                problems.append("Paradox muss eine Stopp-Zone anlegen. ");
            }
            if (engine.isFrozen(boss)) {
                problems.append("Paradox selbst lässt sich nicht anhalten. ");
            }
            if (!engine.isFrozen(player)) {
                problems.append("Der Spieler (ohne Chrono-Rüstung) muss im Stillstand einfrieren. ");
            }
            boss.knifeRing(player, ParadoxBoss.KNIFE_RING);
        });
        helper.runAfterDelay(8, () -> helper.getLevel().getEntitiesOfClass(ThrownChronoKnife.class, area).forEach(k -> frozenAt.add(k.position())));
        helper.runAfterDelay(20, () -> {
            var knives = helper.getLevel().getEntitiesOfClass(ThrownChronoKnife.class, area);
            if (knives.size() != ParadoxBoss.KNIFE_RING || frozenAt.size() != ParadoxBoss.KNIFE_RING) {
                problems.append("Der Messerkranz muss aus ").append(ParadoxBoss.KNIFE_RING).append(" Messern bestehen, gefunden ").append(knives.size()).append(". ");
            }
            for (var knife : knives) {
                if (frozenAt.stream().noneMatch(p -> p.distanceToSqr(knife.position()) < 1.0E-6)) {
                    problems.append("Im Stillstand müssen die Messer in der Luft hängen bleiben. ");
                    break;
                }
            }
        });
        helper.runAfterDelay(ParadoxBoss.STOP_TICKS + 12, () -> {
            var knives = helper.getLevel().getEntitiesOfClass(ThrownChronoKnife.class, area);
            boolean moved = knives.isEmpty() || knives.stream().anyMatch(k -> frozenAt.stream().noneMatch(p -> p.distanceToSqr(k.position()) < 1.0E-6));
            if (!moved) {
                problems.append("Nach dem Stillstand müssen die Messer losfliegen. ");
            }
            knives.forEach(k -> k.discard());
            boss.discard();
            player.discard();
            if (problems.length() > 0) {
                helper.fail(problems.toString());
            }
            helper.succeed();
        });
    }

    /** Wer Paradox verlangsamt, wird selbst langsamer: Es spiegelt die Zone auf den Spieler. */
    @GameTest(template = ARENA, timeoutTicks = 60, setupTicks = 5, batch = "timemod_paradox_mirror")
    public void paradoxMirrorsYourTime(GameTestHelper helper) {
        ParadoxBoss boss = helper.spawn(ModEntities.PARADOX, new Vec3(12.5, 2, 8.5));
        boss.setNoAi(true);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(12, 2, 16)));
        player.teleportTo(at.x, at.y, at.z);
        ServerTimeEngine engine = ServerTimeEngine.of(helper.getLevel());
        engine.addZone(new ZoneShape.Sphere(boss.position(), 4), 0.5, TimeFilter.DEFAULT, player.getUUID(), 200);
        helper.runAfterDelay(15, () -> {
            boolean mirrored = engine.zones().stream().anyMatch(z -> boss.getUUID().equals(z.owner()) && z.isTargeted()
                && player.getUUID().equals(z.targetUuid()) && z.factor() == 0.5);
            engine.removeZonesOwnedBy(player.getUUID());
            engine.removeZonesOwnedBy(boss.getUUID());
            boss.discard();
            player.discard();
            if (!mirrored) {
                helper.fail("Paradox muss die Zeitlupe auf den Spieler spiegeln");
            }
            helper.succeed();
        });
    }

    /** Besiegt hinterlässt Paradox das Herz der Zeit. */
    @GameTest(template = ARENA, timeoutTicks = 60, setupTicks = 5, batch = "timemod_paradox_loot")
    public void paradoxDropsHeartOfTime(GameTestHelper helper) {
        ParadoxBoss boss = helper.spawn(ModEntities.PARADOX, new Vec3(12.5, 2, 8.5));
        boss.setNoAi(true);
        boss.kill();
        helper.runAfterDelay(5, () -> {
            AABB area = new AABB(helper.absolutePos(new BlockPos(12, 2, 8))).inflate(6);
            boolean heart = helper.getLevel().getEntitiesOfClass(ItemEntity.class, area).stream().anyMatch(e -> e.getItem().is(ModItems.HEART_OF_TIME));
            if (!heart) {
                helper.fail("Paradox muss das Herz der Zeit fallen lassen");
            }
            helper.getLevel().getEntitiesOfClass(ItemEntity.class, area).forEach(ItemEntity::discard);
            helper.succeed();
        });
    }

    private static void use(GameTestHelper helper, ServerPlayer player, BlockPos altar) {
        BlockPos abs = helper.absolutePos(altar);
        helper.getLevel().getBlockState(abs).use(helper.getLevel(), player, InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
    }
}
