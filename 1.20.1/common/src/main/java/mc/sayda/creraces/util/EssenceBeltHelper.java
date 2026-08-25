package mc.sayda.creraces.util;

import mc.sayda.creraces.ability.EssenceType;
import mc.sayda.creraces.item.EssenceBeltItem;
import mc.sayda.creraces.item.EssenceBottleItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class EssenceBeltHelper {

    /** Belt-only count — used by screens that don't have level access. */
    public static int getEssenceCount(Player player, EssenceType type) {
        return findBelt(player)
                .map(belt -> belt.getItem() instanceof EssenceBeltItem item
                        ? item.getEssenceCount(belt, type) : 0)
                .orElse(0);
    }

    /** Combined count: adjacent storage + belt. Scans neighbors of one table-part position. */
    public static int getEssenceCount(Player player, EssenceType type, Level level, BlockPos tablePos) {
        return getEssenceCount(player, type, level, List.of(tablePos));
    }

    /**
     * Combined count scanning neighbors of all table-part positions (deduplicated).
     * Storage is counted separately from the belt; both contribute to the total.
     */
    public static int getEssenceCount(Player player, EssenceType type, Level level, Collection<BlockPos> tableParts) {
        return storageCount(type, level, tableParts) + getEssenceCount(player, type);
    }

    /** Belt-only consume. */
    public static boolean consumeEssence(ServerPlayer player, EssenceType type, int amount) {
        Optional<ItemStack> belt = findBelt(player);
        if (belt.isEmpty()) return false;
        ItemStack stack = belt.get();
        if (!(stack.getItem() instanceof EssenceBeltItem item)) return false;
        if (item.getEssenceCount(stack, type) < amount) return false;
        return item.consumeEssence(stack, type, amount);
    }

    /** Storage-first consume, belt handles the remainder. Scans neighbors of one table-part position. */
    public static boolean consumeEssence(ServerPlayer player, EssenceType type, int amount, Level level, BlockPos tablePos) {
        return consumeEssence(player, type, amount, level, List.of(tablePos));
    }

    /**
     * Storage-first consume scanning neighbors of all table-part positions (deduplicated).
     * Drains storage completely before touching the belt.
     */
    public static boolean consumeEssence(ServerPlayer player, EssenceType type, int amount, Level level, Collection<BlockPos> tableParts) {
        if (getEssenceCount(player, type, level, tableParts) < amount) return false;
        int remaining = drainFromStorage(type, level, tableParts, amount);
        if (remaining > 0) {
            Optional<ItemStack> belt = findBelt(player);
            if (belt.isEmpty()) return false;
            ItemStack beltStack = belt.get();
            if (!(beltStack.getItem() instanceof EssenceBeltItem beltItem)) return false;
            beltItem.consumeEssence(beltStack, type, remaining);
        }
        return true;
    }

    // ── Storage scanning ──────────────────────────────────────────────────────

    /** Public accessor used by the block entity to snapshot counts into the menu's extra data. */
    public static int getStorageCount(EssenceType type, Level level, Collection<BlockPos> tableParts) {
        return storageCount(type, level, tableParts);
    }

    private static int storageCount(EssenceType type, Level level, Collection<BlockPos> tableParts) {
        int count = 0;
        for (BlockPos neighborPos : neighborSet(tableParts)) {
            if (level.getBlockEntity(neighborPos) instanceof Container c) {
                count += countInContainer(c, type);
            }
        }
        return count;
    }

    private static int drainFromStorage(EssenceType type, Level level, Collection<BlockPos> tableParts, int amount) {
        int remaining = amount;
        for (BlockPos neighborPos : neighborSet(tableParts)) {
            if (remaining <= 0) break;
            if (level.getBlockEntity(neighborPos) instanceof Container c) {
                remaining = drainFromContainer(c, type, remaining);
            }
        }
        return remaining;
    }

    /** All distinct block positions adjacent to any table part, excluding the parts themselves. */
    private static Set<BlockPos> neighborSet(Collection<BlockPos> tableParts) {
        Set<BlockPos> neighbors = new HashSet<>();
        for (BlockPos part : tableParts) {
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = part.relative(dir);
                if (!tableParts.contains(neighbor)) {
                    neighbors.add(neighbor);
                }
            }
        }
        return neighbors;
    }

    private static int countInContainer(Container container, EssenceType type) {
        int count = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.getItem() instanceof EssenceBottleItem b && b.getEssenceType() == type) {
                count += stack.getMaxDamage() - stack.getDamageValue();
            }
        }
        return count;
    }

    private static int drainFromContainer(Container container, EssenceType type, int amount) {
        int remaining = amount;
        for (int i = 0; i < container.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = container.getItem(i);
            if (!(stack.getItem() instanceof EssenceBottleItem b) || b.getEssenceType() != type) continue;
            int charges = stack.getMaxDamage() - stack.getDamageValue();
            if (charges <= remaining) {
                remaining -= charges;
                container.setItem(i, new ItemStack(Items.GLASS_BOTTLE));
            } else {
                stack.setDamageValue(stack.getDamageValue() + remaining);
                remaining = 0;
            }
        }
        container.setChanged();
        return remaining;
    }

    private static Optional<ItemStack> findBelt(Player player) {
        return PlatformServices.findBelt(player);
    }
}
