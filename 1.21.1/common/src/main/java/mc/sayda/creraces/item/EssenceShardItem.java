package mc.sayda.creraces.item;

import mc.sayda.creraces.ability.EssenceType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class EssenceShardItem extends Item {

    private final EssenceType essenceType;

    public EssenceShardItem(EssenceType essenceType, Properties properties) {
        super(properties);
        this.essenceType = essenceType;
    }

    public EssenceType getEssenceType() {
        return essenceType;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.creraces.essence_shard",
                Component.translatable("essence.creraces." + essenceType.getSerializedName()));
    }
}
