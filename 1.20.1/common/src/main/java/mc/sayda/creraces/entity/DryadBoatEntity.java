package mc.sayda.creraces.entity;

import mc.sayda.creraces.registry.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class DryadBoatEntity extends Boat {
    public DryadBoatEntity(EntityType<? extends Boat> type, Level level) {
        super(type, level);
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.DRYAD_BOAT_ITEM.get());
    }
}
