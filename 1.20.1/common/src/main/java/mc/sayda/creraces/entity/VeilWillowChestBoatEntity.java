package mc.sayda.creraces.entity;

import mc.sayda.creraces.registry.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class VeilWillowChestBoatEntity extends ChestBoat {
    public VeilWillowChestBoatEntity(EntityType<? extends ChestBoat> type, Level level) {
        super(type, level);
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.VEIL_WILLOW_CHEST_BOAT_ITEM.get());
    }
}
