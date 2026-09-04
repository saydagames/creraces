package mc.sayda.creraces.ability;

import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.block.EssenceClusterBlock;
import mc.sayda.creraces.block.EssenceVortexBlock;
import mc.sayda.creraces.item.EssenceBottleItem;
import mc.sayda.creraces.item.EssenceShardItem;
import mc.sayda.creraces.registry.ModBlocks;
import mc.sayda.creraces.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.EnumMap;

public class EssenceRegistry {

    public static final EnumMap<EssenceType, RegistrySupplier<Item>>  SHARDS   = new EnumMap<>(EssenceType.class);
    public static final EnumMap<EssenceType, RegistrySupplier<Item>>  BOTTLES  = new EnumMap<>(EssenceType.class);
    public static final EnumMap<EssenceType, RegistrySupplier<Block>> CLUSTERS = new EnumMap<>(EssenceType.class);
    public static final EnumMap<EssenceType, RegistrySupplier<Block>> VORTEXES = new EnumMap<>(EssenceType.class);

    public static final EnumMap<EssenceType, RegistrySupplier<Item>>  CLUSTER_ITEMS = new EnumMap<>(EssenceType.class);
    public static final EnumMap<EssenceType, RegistrySupplier<Item>>  VORTEX_ITEMS  = new EnumMap<>(EssenceType.class);

    public static void registerItems() {
        for (EssenceType type : EssenceType.values()) {
            String id = type.getSerializedName();
            SHARDS.put(type,
                ModItems.ITEMS.register(id + "_essence_shard",
                    () -> new EssenceShardItem(type, new Item.Properties())));
            BOTTLES.put(type,
                ModItems.ITEMS.register(id + "_essence_bottle",
                    () -> new EssenceBottleItem(type, new Item.Properties().durability(16))));
        }
    }

    public static void registerBlocks() {
        for (EssenceType type : EssenceType.values()) {
            String id = type.getSerializedName();
            RegistrySupplier<Block> cluster = ModBlocks.BLOCKS.register(id + "_essence_cluster",
                () -> new EssenceClusterBlock(type));
            RegistrySupplier<Block> vortex = ModBlocks.BLOCKS.register(id + "_essence_vortex",
                () -> new EssenceVortexBlock(type));

            CLUSTERS.put(type, cluster);
            VORTEXES.put(type, vortex);

            CLUSTER_ITEMS.put(type, ModItems.ITEMS.register(id + "_essence_cluster",
                () -> new EssenceBlockItem(cluster.get(), type, "block.creraces.essence_cluster")));
            VORTEX_ITEMS.put(type, ModItems.ITEMS.register(id + "_essence_vortex",
                () -> new EssenceBlockItem(vortex.get(), type, "block.creraces.essence_vortex")));
        }
    }

    public static EssenceType typeFromShard(Item item) {
        for (var entry : SHARDS.entrySet()) {
            if (entry.getValue().get() == item) return entry.getKey();
        }
        return null;
    }

    public static EssenceType typeFromBottle(Item item) {
        for (var entry : BOTTLES.entrySet()) {
            if (entry.getValue().get() == item) return entry.getKey();
        }
        return null;
    }

    private static class EssenceBlockItem extends BlockItem {
        private final EssenceType essenceType;
        private final String langKey;

        EssenceBlockItem(Block block, EssenceType type, String langKey) {
            super(block, new Item.Properties());
            this.essenceType = type;
            this.langKey = langKey;
        }

        @Override
        public Component getName(ItemStack stack) {
            return Component.translatable(langKey,
                    Component.translatable("essence.creraces." + essenceType.getSerializedName()));
        }
    }

}
