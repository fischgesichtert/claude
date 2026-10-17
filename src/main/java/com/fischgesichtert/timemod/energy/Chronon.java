package com.fischgesichtert.timemod.energy;

import com.fischgesichtert.timemod.armor.ChronoArmorItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

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
