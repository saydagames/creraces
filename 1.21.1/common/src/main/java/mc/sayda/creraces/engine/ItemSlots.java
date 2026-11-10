package mc.sayda.creraces.engine;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads the "slot" names the engine uses: mainhand, offhand, head, chest, legs, feet, "armor" (the
 * four armour slots), "hands", "any" (all equipment), or a player inventory index.
 */
public final class ItemSlots {
    private ItemSlots() {
    }

    /** Every stack the slot name covers; empty stacks included, unknown names give none. */
    public static List<ItemStack> stacks(LivingEntity entity, String slot) {
        List<ItemStack> stacks = new ArrayList<>();
        switch (slot) {
            case "mainhand" -> stacks.add(entity.getMainHandItem());
            case "offhand" -> stacks.add(entity.getOffhandItem());
            case "hands" -> {
                stacks.add(entity.getMainHandItem());
                stacks.add(entity.getOffhandItem());
            }
            case "head" -> stacks.add(entity.getItemBySlot(EquipmentSlot.HEAD));
            case "chest" -> stacks.add(entity.getItemBySlot(EquipmentSlot.CHEST));
            case "legs" -> stacks.add(entity.getItemBySlot(EquipmentSlot.LEGS));
            case "feet" -> stacks.add(entity.getItemBySlot(EquipmentSlot.FEET));
            case "armor" -> entity.getArmorSlots().forEach(stacks::add);
            case "any" -> entity.getAllSlots().forEach(stacks::add);
            default -> {
                if (entity instanceof Player player) {
                    try {
                        int index = Integer.parseInt(slot);
                        if (index >= 0 && index < player.getInventory().getContainerSize()) {
                            stacks.add(player.getInventory().getItem(index));
                        }
                    } catch (NumberFormatException ignored) {
                        // Neither a slot name nor an inventory index.
                    }
                }
            }
        }
        return stacks;
    }

    /** The single stack in a one-slot name, or EMPTY when the name covers no slot. */
    public static ItemStack stack(LivingEntity entity, String slot) {
        List<ItemStack> stacks = stacks(entity, slot);
        return stacks.isEmpty() ? ItemStack.EMPTY : stacks.get(0);
    }
}
