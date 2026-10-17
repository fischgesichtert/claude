package com.fischgesichtert.timemod.gametest;

import com.fischgesichtert.timemod.energy.Chronon;
import com.fischgesichtert.timemod.entity.StasisBolt;
import com.fischgesichtert.timemod.entity.TimeWarden;
import com.fischgesichtert.timemod.item.TimeSealItem;
import com.fischgesichtert.timemod.module.Modules;
import com.fischgesichtert.timemod.paradox.ParadoxPressure;
import com.fischgesichtert.timemod.paradox.TimeWardens;
import com.fischgesichtert.timemod.registry.ModEntities;
import com.fischgesichtert.timemod.registry.ModItems;
import com.fischgesichtert.timemod.time.ServerTimeEngine;
import com.fischgesichtert.timemod.time.TimeFilter;
import com.fischgesichtert.timemod.time.TimeZone;
import com.fischgesichtert.timemod.time.ZoneShape;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Paradox-Druck (Ursachen, Dämpfung, Abklingen, Siegel) und die Zeitwächter (Erscheinen, Arrest, Schuld, Fall erledigt). */
public class ParadoxTests implements FabricGameTest {
    private static final String ARENA = "timemod-gametest:arena";

    private static ServerPlayer survivor(GameTestHelper helper, BlockPos at) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 pos = Vec3.atBottomCenterOf(helper.absolutePos(at));
        player.teleportTo(pos.x, pos.y, pos.z);
        return player;
    }

    private static void close(ServerPlayer player) {
        ParadoxPressure.set(player, 0.0F);
        player.discard();
    }

    private static void near(GameTestHelper helper, float actual, float expected, String what) {
        if (Math.abs(actual - expected) > 0.05F) {
            helper.fail(what + ": erwartet " + expected + " %, ist " + actual + " %");
        }
    }

    /** Chronon und Reisen erzeugen Druck nach Formel; Dämpfungsmodule halbieren ihn; im Kreativmodus entsteht keiner. */
    @GameTest(template = EMPTY_STRUCTURE)
    public void pressureFollowsTheCost(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, new BlockPos(1, 2, 1));
        ItemStack watch = new ItemStack(ModItems.STASIS_WATCH);
        Chronon.set(watch, 10_000);
        if (!Chronon.pay(player, watch, 400)) {
            helper.fail("Bezahlen mit genug Chronon muss klappen");
        }
        near(helper, ParadoxPressure.of(player), 2.0F, "400 Chronon");
        ItemStack damped = new ItemStack(ModItems.STASIS_WATCH);
        Chronon.set(damped, 10_000);
        Modules.write(damped, List.of(new ItemStack(ModItems.DAMPING_MODULE), new ItemStack(ModItems.DAMPING_MODULE), ItemStack.EMPTY));
        Chronon.pay(player, damped, 400);
        near(helper, ParadoxPressure.of(player), 2.5F, "Mit zwei Dämpfungsmodulen kommt nur ein Viertel dazu");
        near(helper, ParadoxPressure.forTravel(0, 1000), 7.5F, "Reise +1000");
        near(helper, ParadoxPressure.forTravel(0, -245_000_000), 21.0F, "Reise −245 Mio.");
        ServerPlayer builder = helper.makeMockServerPlayerInLevel();
        builder.setGameMode(GameType.CREATIVE);
        Chronon.pay(builder, watch, 100_000);
        near(helper, ParadoxPressure.of(builder), 0.0F, "Im Kreativmodus");
        close(player);
        close(builder);
        helper.succeed();
    }

    /** Der Druck klingt ab (2 % pro Minute); ein Zeitsiegel nimmt 20 %; nach oben ist bei 100 Schluss. */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void pressureDecaysAndSeals(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, new BlockPos(1, 2, 1));
        ParadoxPressure.set(player, 30.0F);
        helper.runAfterDelay(61, () -> {
            float value = ParadoxPressure.of(player);
            // drei Sekunden Abklingen (± eine, je nachdem wo der Sekundentakt liegt)
            if (value > 30.0F - 2 * ParadoxPressure.DECAY_PER_SECOND + 0.001F || value < 30.0F - 4 * ParadoxPressure.DECAY_PER_SECOND - 0.001F) {
                helper.fail("Nach drei Sekunden muss der Druck um etwa 0,1 % gesunken sein: " + value);
            }
            ParadoxPressure.set(player, 50.0F);
            ItemStack seal = new ItemStack(ModItems.TIME_SEAL, 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, seal);
            seal.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            near(helper, ParadoxPressure.of(player), 50.0F - TimeSealItem.RELIEF, "Ein Siegel");
            if (player.getMainHandItem().getCount() != 1) {
                helper.fail("Das Siegel wird verbraucht");
            }
            ParadoxPressure.add(player, 500.0F);
            near(helper, ParadoxPressure.of(player), ParadoxPressure.MAX, "Obergrenze");
            close(player);
            helper.succeed();
        });
    }

    /** Wächter stehen außerhalb der Zeit: Ein Stopp hält sie nicht an. */
    @GameTest(template = ARENA, timeoutTicks = 60, setupTicks = 5, batch = "timemod_wardens")
    public void wardensStandOutsideTime(GameTestHelper helper) {
        TimeWarden warden = helper.spawn(ModEntities.TIME_WARDEN, new Vec3(12.5, 2, 8.5));
        warden.setNoAi(true);
        ServerTimeEngine engine = ServerTimeEngine.of(helper.getLevel());
        TimeZone zone = engine.addZone(new ZoneShape.Sphere(warden.position(), 6), 0.0, TimeFilter.DEFAULT, null, 40);
        helper.runAfterDelay(2, () -> {
            if (engine.isFrozen(warden) || engine.resolveEntity(warden) != null) {
                helper.fail("Ein Zeitwächter darf von keiner Zeitzone erfasst werden");
            }
            engine.removeZone(zone.id());
            warden.discard();
            helper.succeed();
        });
    }

    /** Ein Treffer treibt Schuld ein (−3 %, Chronon) und setzt den Gejagten fest – außer er trägt die volle Chrono-Rüstung. */
    @GameTest(template = ARENA, timeoutTicks = 160, setupTicks = 5, batch = "timemod_wardens")
    public void wardenArrestsAndCollectsDebt(GameTestHelper helper) {
        ServerPlayer suspect = survivor(helper, new BlockPos(10, 2, 8));
        ServerPlayer armored = survivor(helper, new BlockPos(16, 2, 8));
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            armored.setItemSlot(slot, new ItemStack(switch (slot) {
                case HEAD -> ModItems.CHRONO_HELMET;
                case CHEST -> ModItems.CHRONO_CHESTPLATE;
                case LEGS -> ModItems.CHRONO_LEGGINGS;
                default -> ModItems.CHRONO_BOOTS;
            }));
        }
        ItemStack cell = new ItemStack(ModItems.POCKET_TIME_CELL);
        Chronon.set(cell, 1000);
        suspect.getInventory().setItem(5, cell);
        ParadoxPressure.set(suspect, 60.0F);
        ParadoxPressure.set(armored, 60.0F);
        TimeWarden warden = helper.spawn(ModEntities.TIME_WARDEN, new Vec3(12.5, 2, 8.5));
        warden.setNoAi(true);
        TimeWarden other = helper.spawn(ModEntities.TIME_WARDEN, new Vec3(14.5, 2, 8.5));
        other.setNoAi(true);
        warden.hunt(suspect);
        other.hunt(armored);
        // Frisch angemeldete Spieler sind drei Sekunden unverwundbar – erst danach zuschlagen
        int strike = 62;
        helper.runAfterDelay(strike, () -> {
            float before = ParadoxPressure.of(suspect);
            if (!warden.doHurtTarget(suspect) || !other.doHurtTarget(armored)) {
                helper.fail("Der Schlag des Wächters muss treffen");
            }
            near(helper, ParadoxPressure.of(suspect), before - TimeWarden.DEBT_PER_HIT, "Schuld nach einem Treffer");
            if (Chronon.stored(suspect.getInventory().getItem(5)) != 1000 - TimeWarden.FINE) {
                helper.fail("Ein Treffer zieht " + TimeWarden.FINE + " Chronon ein, übrig: " + Chronon.stored(suspect.getInventory().getItem(5)));
            }
        });
        ServerTimeEngine engine = ServerTimeEngine.of(helper.getLevel());
        helper.runAfterDelay(strike + 2, () -> {
            if (!engine.isFrozenPlayer(suspect)) {
                helper.fail("Der Getroffene muss im Zeitarrest stehen");
            }
            if (engine.isFrozenPlayer(armored)) {
                helper.fail("Die volle Chrono-Rüstung schützt vor dem Zeitarrest");
            }
        });
        helper.runAfterDelay(strike + TimeWarden.ARREST_TICKS + 6, () -> {
            if (engine.isFrozenPlayer(suspect)) {
                helper.fail("Der Zeitarrest endet nach " + TimeWarden.ARREST_TICKS + " Ticks");
            }
            warden.discard();
            other.discard();
            close(suspect);
            close(armored);
            helper.succeed();
        });
    }

    /** Fällt der Druck unter 25 %, ist der Fall erledigt: Der Wächter zieht sich zusammen und verschwindet. */
    @GameTest(template = ARENA, timeoutTicks = 120, setupTicks = 5, batch = "timemod_wardens")
    public void wardenLeavesWhenCaseIsClosed(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, new BlockPos(6, 2, 8));
        ParadoxPressure.set(player, 40.0F);
        TimeWarden warden = helper.spawn(ModEntities.TIME_WARDEN, new Vec3(16.5, 2, 8.5));
        warden.hunt(player);
        helper.runAfterDelay(30, () -> {
            if (!warden.isAlive() || warden.isFading()) {
                helper.fail("Bei 40 % bleibt der Fall offen");
            }
            ParadoxPressure.set(player, 20.0F);
        });
        helper.runAfterDelay(30 + 20 + TimeWarden.FADE_TICKS + 4, () -> {
            if (!warden.isRemoved()) {
                helper.fail("Unter 25 % muss der Wächter verschwunden sein");
            }
            close(player);
            helper.succeed();
        });
    }

    /** Ab 50 % erscheint ein Wächter in der Nähe; wer einen besiegt, beruhigt die Zeitlinie um 10 %. */
    @GameTest(template = ARENA, timeoutTicks = 120, setupTicks = 5, batch = "timemod_wardens")
    public void wardensComeForTheirSuspect(GameTestHelper helper) {
        if (TimeWardens.allowed(49) != 0 || TimeWardens.allowed(50) != 1 || TimeWardens.allowed(80) != 2 || TimeWardens.allowed(95) != 3) {
            helper.fail("Wächter zugleich: 0 unter 50 %, 1 ab 50 %, 2 ab 75 %, 3 ab 90 %");
        }
        ServerPlayer player = survivor(helper, new BlockPos(12, 2, 8));
        ParadoxPressure.set(player, 70.0F);
        TimeWardens.schedule(player, 1);
        helper.runAfterDelay(45, () -> {
            List<TimeWarden> hunters = TimeWardens.hunters(player);
            if (hunters.size() != 1) {
                helper.fail("Bei 70 % muss genau ein Wächter erschienen sein, gefunden: " + hunters.size());
                return;
            }
            TimeWarden warden = hunters.get(0);
            double distance = warden.distanceTo(player);
            if (distance < 6.0 || distance > 20.0) {
                helper.fail("Der Wächter erscheint 8–16 Blöcke entfernt, nicht " + distance);
            }
            float before = ParadoxPressure.of(player);
            warden.hurt(helper.getLevel().damageSources().playerAttack(player), 1000.0F);
            near(helper, ParadoxPressure.of(player), before - TimeWarden.DEFEAT_RELIEF, "Nach dem Sieg über einen Wächter");
            close(player);
            helper.getLevel().getEntitiesOfClass(TimeWarden.class, new AABB(player.blockPosition()).inflate(64)).forEach(TimeWarden::discard);
            helper.succeed();
        });
    }

    /** Zeigerklinge und Stasis-Zeiger lassen die Zeit der Getroffenen stocken. */
    @GameTest(template = ARENA, timeoutTicks = 80, setupTicks = 5, batch = "timemod_wardens")
    public void bladeAndBoltSlowTheirVictims(GameTestHelper helper) {
        ServerPlayer player = survivor(helper, new BlockPos(4, 2, 8));
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new Vec3(6.5, 2, 8.5));
        zombie.setNoAi(true);
        ItemStack blade = new ItemStack(ModItems.CLOCK_HAND_BLADE);
        blade.getItem().hurtEnemy(blade, zombie, player);
        Pig pig = helper.spawn(EntityType.PIG, new Vec3(20.5, 2, 8.5));
        pig.setNoAi(true);
        TimeWarden warden = helper.spawn(ModEntities.TIME_WARDEN, new Vec3(14.5, 2, 8.5));
        warden.setNoAi(true);
        Vec3 from = new Vec3(warden.getX() + 1.0, pig.getY(0.5), pig.getZ());
        StasisBolt bolt = new StasisBolt(helper.getLevel(), warden, pig.position().add(0, pig.getBbHeight() / 2, 0).subtract(from));
        bolt.setPos(from.x, from.y, from.z);
        helper.getLevel().addFreshEntity(bolt);
        ServerTimeEngine engine = ServerTimeEngine.of(helper.getLevel());
        helper.runAfterDelay(2, () -> {
            TimeZone zone = engine.resolveEntity(zombie);
            if (zone == null || Math.abs(zone.factor() - 0.5) > 1e-6) {
                helper.fail("Die Zeigerklinge lässt den Getroffenen mit halber Zeit laufen, Zone: " + zone);
            }
        });
        helper.succeedWhen(() -> {
            TimeZone zone = engine.resolveEntity(pig);
            if (zone == null || Math.abs(zone.factor() - StasisBolt.SLOW_FACTOR) > 1e-6) {
                helper.fail("Der Stasis-Zeiger muss das Schwein verlangsamen");
            }
            if (pig.getHealth() >= pig.getMaxHealth()) {
                helper.fail("Der Stasis-Zeiger richtet Schaden an");
            }
            warden.discard();
            close(player);
        });
    }
}
