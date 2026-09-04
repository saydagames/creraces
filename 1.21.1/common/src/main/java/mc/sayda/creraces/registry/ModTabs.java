package mc.sayda.creraces.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilityRegistry;
import mc.sayda.creraces.ability.EssenceRegistry;
import mc.sayda.creraces.ability.EssenceType;
import mc.sayda.creraces.item.EssenceBucketItem;
import mc.sayda.creraces.item.ScrollItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(CreRaces.MODID,
            Registries.CREATIVE_MODE_TAB);

    // Positions are intentionally not hard-coded: dev.architectury.registry.CreativeTabRegistry.create
    // hands each platform an auto-positioning builder (Forge's no-arg CreativeModeTab.builder(),
    // Fabric's FabricItemGroup.builder()), so tabs registered in this order (main, scrolls, essence)
    // land adjacent to each other instead of all fighting over the same explicit row/column.
    public static final RegistrySupplier<CreativeModeTab> MAIN_TAB = TABS.register("main",
            () -> dev.architectury.registry.CreativeTabRegistry.create(builder -> builder
                    .icon(() -> new ItemStack(ModItems.DRYAD_SAPLING_ITEM.get()))
                    .title(Component.translatable("itemGroup.creraces.main"))
                    .displayItems((parameters, output) -> {
                        for (RegistrySupplier<net.minecraft.world.item.Item> itemSupplier : ModItems.ITEMS) {
                            net.minecraft.world.item.Item item = itemSupplier.get();
                            if (item instanceof ScrollItem) continue;
                            if (isEssenceItem(item)) continue;
                            output.accept(item);
                        }
                        output.accept(ModItems.ABILITY_SCROLL.get());
                    })));

    public static final RegistrySupplier<CreativeModeTab> SCROLLS_TAB = TABS.register("scrolls",
            () -> dev.architectury.registry.CreativeTabRegistry.create(builder -> builder
                    .icon(() -> new ItemStack(ModItems.ABILITY_SCROLL.get()))
                    .title(Component.translatable("itemGroup.creraces.scrolls"))
                    .displayItems((parameters, output) -> {
                        // Add default scroll
                        output.accept(ModItems.ABILITY_SCROLL.get());

                        // Add all specific scrolls
                        AbilityRegistry.getAll().forEach(ability -> {
                            output.accept(ScrollItem.create(ability.id()));
                        });
                    })));

    public static final RegistrySupplier<CreativeModeTab> ESSENCE_TAB = TABS.register("essence",
            () -> dev.architectury.registry.CreativeTabRegistry.create(builder -> builder
                    .icon(() -> new ItemStack(EssenceRegistry.BOTTLES.get(EssenceType.ARCANE).get()))
                    .title(Component.translatable("itemGroup.creraces.essence"))
                    .displayItems((parameters, output) -> {
                        for (EssenceType type : EssenceType.values()) {
                            output.accept(EssenceRegistry.SHARDS.get(type).get());
                            output.accept(EssenceRegistry.BOTTLES.get(type).get());
                            output.accept(EssenceBucketItem.of(type, null));
                            output.accept(EssenceRegistry.CLUSTER_ITEMS.get(type).get());
                            output.accept(EssenceRegistry.VORTEX_ITEMS.get(type).get());
                        }
                        output.accept(ModItems.ESSENCE_BELT.get());
                        output.accept(ModItems.ESSENCE_CAULDRON_ITEM.get());
                    })));

    private static boolean isEssenceItem(net.minecraft.world.item.Item item) {
        for (EssenceType type : EssenceType.values()) {
            if (EssenceRegistry.SHARDS.get(type).get() == item) return true;
            if (EssenceRegistry.BOTTLES.get(type).get() == item) return true;
            if (EssenceRegistry.CLUSTER_ITEMS.get(type).get() == item) return true;
            if (EssenceRegistry.VORTEX_ITEMS.get(type).get() == item) return true;
        }
        return item == ModItems.ESSENCE_BELT.get()
            || item == ModItems.ESSENCE_CAULDRON_ITEM.get()
            || item == ModItems.ESSENCE_BUCKET.get();
    }

    public static void register() {
        TABS.register();
    }
}
