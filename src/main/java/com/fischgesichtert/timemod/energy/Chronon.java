package com.fischgesichtert.timemod.energy;

import com.fischgesichtert.timemod.armor.ChronoArmorItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Chronon – die Energie der Zeit. Werkzeuge speichern sie im Item selbst und ziehen bei Bedarf aus
 * Zeitzellen im Inventar nach.
 */
public final class Chronon {
    public static final String KEY = "Chronon";

    private Chronon() {
    }

    public static int stored(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt(KEY) : 0;
    }

    public static int capacity(ItemStack stack) {
        return stack.getItem() instanceof ChrononHolder holder ? holder.chrononCapacity(stack) : 0;
    }

    public static void set(ItemStack stack, int amount) {
        stack.getOrCreateTag().putInt(KEY, Math.max(0, Math.min(capacity(stack), amount)));
    }

    /** Lädt bis zu {@code amount} auf; liefert die tatsächlich aufgenommene Menge. */
    public static int insert(ItemStack stack, int amount) {
        int space = capacity(stack) - stored(stack);
        int moved = Math.max(0, Math.min(space, amount));
        if (moved > 0) {
            set(stack, stored(stack) + moved);
        }
        return moved;
    }

    /** Entnimmt bis zu {@code amount}; liefert die tatsächlich entnommene Menge. */
    public static int extract(ItemStack stack, int amount) {
        int moved = Math.max(0, Math.min(stored(stack), amount));
        if (moved > 0) {
            set(stack, stored(stack) - moved);
        }
        return moved;
    }

    /**
     * Bezahlt {@code cost} für ein Werkzeug: erst aus dem Werkzeug selbst, dann aus Zeitzellen im
     * Inventar. Im Kreativmodus kostenlos. Ohne genug Chronon passiert nichts.
     */
    public static boolean pay(Player player, ItemStack tool, int cost) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        cost = discounted(player, cost);
        int available = stored(tool);
        for (ItemStack stack : player.getInventory().items) {
            if (stack != tool && stack.getItem() instanceof ChrononCellItem) {
                available += stored(stack);
            }
        }
        if (available < cost) {
            return false;
        }
        int remaining = cost - extract(tool, cost);
        for (ItemStack stack : player.getInventory().items) {
            if (remaining <= 0) {
                break;
            }
            if (stack != tool && stack.getItem() instanceof ChrononCellItem) {
                remaining -= extract(stack, remaining);
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ Bilanz (Zurückspulen)

    /** Chronon in einem Behälter (nur Items, die Chronon speichern). */
    public static long total(Container container) {
        long sum = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.getItem() instanceof ChrononHolder) {
                sum += stored(stack);
            }
        }
        return sum;
    }

    /** Entnimmt bis zu {@code amount} aus den Chronon-Items eines Behälters. */
    public static int drain(Container container, int amount) {
        int taken = 0;
        for (int i = 0; i < container.getContainerSize() && taken < amount; i++) {
            ItemStack stack = container.getItem(i);
            if (stack.getItem() instanceof ChrononHolder) {
                taken += extract(stack, amount - taken);
            }
        }
        if (taken > 0) {
            container.setChanged();
        }
        return taken;
    }

    /** Chronon, das ein Spieler bei sich hat: Inventar, Rüstung, zweite Hand und Endertruhe. */
    public static long total(Player player) {
        return total(player.getInventory()) + total(player.getEnderChestInventory());
    }

    public static int drain(Player player, int amount) {
        int taken = drain(player.getInventory(), amount);
        return taken + drain(player.getEnderChestInventory(), amount - taken);
    }

    private static long storedIfHolder(ItemStack stack) {
        return stack.getItem() instanceof ChrononHolder ? stored(stack) : 0;
    }

    /** Chronon, das eine Entity trägt: Item am Boden, im Rahmen, in Kisten-Loren, in der Ausrüstung. */
    public static long total(Entity entity) {
        if (entity instanceof ItemEntity item) {
            return storedIfHolder(item.getItem());
        }
        if (entity instanceof ItemFrame frame) {
            return storedIfHolder(frame.getItem());
        }
        if (entity instanceof Container container) {
            return total(container);
        }
        if (entity instanceof LivingEntity living && !(entity instanceof Player)) {
            long sum = 0;
            for (ItemStack stack : living.getAllSlots()) {
                sum += storedIfHolder(stack);
            }
            return sum;
        }
        return 0;
    }

    public static int drain(Entity entity, int amount) {
        if (entity instanceof ItemEntity item && item.getItem().getItem() instanceof ChrononHolder) {
            ItemStack stack = item.getItem().copy();
            int taken = extract(stack, amount);
            item.setItem(stack);
            return taken;
        }
        if (entity instanceof ItemFrame frame && frame.getItem().getItem() instanceof ChrononHolder) {
            ItemStack stack = frame.getItem().copy();
            int taken = extract(stack, amount);
            frame.setItem(stack, false);
            return taken;
        }
        if (entity instanceof Container container) {
            return drain(container, amount);
        }
        int taken = 0;
        if (entity instanceof LivingEntity living && !(entity instanceof Player)) {
            for (ItemStack stack : living.getAllSlots()) {
                if (taken >= amount) {
                    break;
                }
                if (stack.getItem() instanceof ChrononHolder) {
                    taken += extract(stack, amount - taken);
                }
            }
        }
        return taken;
    }

    /** Chronon in einem Block: Behälterinhalt oder Maschinen-Puffer. */
    public static long total(@Nullable BlockEntity blockEntity) {
        if (blockEntity instanceof ChrononStorage storage) {
            return storage.storedChronon();
        }
        return blockEntity instanceof Container container ? total(container) : 0;
    }

    public static int drain(@Nullable BlockEntity blockEntity, int amount) {
        if (blockEntity instanceof ChrononStorage storage) {
            int taken = storage.drainChronon(amount);
            blockEntity.setChanged();
            return taken;
        }
        return blockEntity instanceof Container container ? drain(container, amount) : 0;
    }

    /** Chrono-Rüstung spart je Teil 5 %. */
    public static int discounted(Player player, int cost) {
        int pieces = ChronoArmorItem.pieces(player);
        return pieces == 0 ? cost : (int) Math.ceil(cost * (1.0 - ChronoArmorItem.SAVING_PER_PIECE * pieces));
    }

    public static String format(int amount) {
        if (amount >= 10_000) {
            return String.format(java.util.Locale.ROOT, "%.1fk", amount / 1000.0);
        }
        return String.valueOf(amount);
    }
}
