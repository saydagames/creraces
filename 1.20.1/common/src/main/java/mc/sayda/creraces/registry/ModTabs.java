package mc.sayda.creraces.registry;

import dev.architectury.registry.CreativeTabRegistry;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(CreRaces.MODID,
            Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> MAIN_TAB = TABS.register("tab_1_main",
            () -> CreativeTabRegistry.create(builder -> builder
                    .icon(() -> new ItemStack(ModItems.DRYAD_SAPLING_ITEM.get()))
                    .title(Component.translatable("itemGroup.creraces.main"))
                    .displayItems((parameters, output) -> {
                        for (RegistrySupplier<Item> itemSupplier : ModItems.ITEMS) {
                            Item item = itemSupplier.get();
                            if (item instanceof ScrollItem) continue;
                            if (isEssenceItem(item)) continue;
                            output.accept(item);
                        }
                        output.accept(ModItems.ABILITY_SCROLL.get());
                    })));

    public static final RegistrySupplier<CreativeModeTab> SCROLLS_TAB = TABS.register("tab_2_scrolls",
            () -> CreativeTabRegistry.create(builder -> {
                withTabsBefore(builder, MAIN_TAB.getId());
                builder.icon(() -> new ItemStack(ModItems.ABILITY_SCROLL.get()))
                    .title(Component.translatable("itemGroup.creraces.scrolls"))
                    .displayItems((parameters, output) -> {
                        // Add default scroll
                        output.accept(ModItems.ABILITY_SCROLL.get());

                        // Add all specific scrolls
                        AbilityRegistry.getAll().forEach(ability -> {
                            output.accept(ScrollItem.create(ability.id()));
                        });
                    });
            }));

    public static final RegistrySupplier<CreativeModeTab> ESSENCE_TAB = TABS.register("tab_3_essence",
            () -> CreativeTabRegistry.create(builder -> {
                withTabsBefore(builder, SCROLLS_TAB.getId());
                builder.icon(() -> new ItemStack(EssenceRegistry.BOTTLES.get(EssenceType.ARCANE).get()))
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
                    });
            }));

    // Forge only: makes `id` sort immediately before this tab, chaining main -> scrolls -> essence
    // adjacent. Reflection because Fabric's builder has no equivalent method (no-ops there).
    // Fabric instead sorts mod tabs alphabetically by registry path when nothing else orders them,
    // which is why the three paths above are numbered tab_1_/tab_2_/tab_3_. That numbering is what
    // keeps the order correct on Fabric; this call is what keeps it correct on Forge.
    private static void withTabsBefore(CreativeModeTab.Builder builder, ResourceLocation id) {
        try {
            builder.getClass().getMethod("withTabsBefore", ResourceLocation[].class)
                    .invoke(builder, (Object) new ResourceLocation[]{id});
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static boolean isEssenceItem(Item item) {
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
