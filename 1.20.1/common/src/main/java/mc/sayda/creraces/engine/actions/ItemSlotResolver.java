package mc.sayda.creraces.engine.actions;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Resolves a named equipment/inventory slot ("mainhand", "head", a numeric inventory index, ...) to its item. */
final class ItemSlotResolver {
    private ItemSlotResolver() {
    }

    static ItemStack getItemInSlot(LivingEntity entity, String slot) {
        if (slot.equalsIgnoreCase("mainhand")) return entity.getMainHandItem();
        if (slot.equalsIgnoreCase("offhand")) return entity.getOffhandItem();
        if (slot.equalsIgnoreCase("head")) return entity.getItemBySlot(EquipmentSlot.HEAD);
        if (slot.equalsIgnoreCase("chest")) return entity.getItemBySlot(EquipmentSlot.CHEST);
        if (slot.equalsIgnoreCase("legs")) return entity.getItemBySlot(EquipmentSlot.LEGS);
        if (slot.equalsIgnoreCase("feet")) return entity.getItemBySlot(EquipmentSlot.FEET);

        if (entity instanceof Player player) {
            try {
                int index = Integer.parseInt(slot);
                if (index >= 0 && index < player.getInventory().getContainerSize()) {
                    return player.getInventory().getItem(index);
                }
            } catch (NumberFormatException ignored) {}
        }

        return ItemStack.EMPTY;
    }
}
