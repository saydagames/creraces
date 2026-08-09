package mc.sayda.creraces.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilityRegistry;
import mc.sayda.creraces.item.ScrollItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(CreRaces.MODID,
            Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> MAIN_TAB = TABS.register("main",
            () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 10)
                    .icon(() -> new ItemStack(ModItems.DRYAD_SAPLING_ITEM.get()))
                    .title(Component.translatable("itemGroup.creraces.main"))
                    .displayItems((parameters, output) -> {
                        // Add all items except scrolls
                        for (RegistrySupplier<net.minecraft.world.item.Item> itemSupplier : ModItems.ITEMS) {
                            if (!(itemSupplier.get() instanceof ScrollItem)) {
                                output.accept(itemSupplier.get());
                            }
                        }
                        // Fallback: add the basic scroll item to the main tab too
                        output.accept(ModItems.ABILITY_SCROLL.get());
                    }).build());

    public static final RegistrySupplier<CreativeModeTab> SCROLLS_TAB = TABS.register("scrolls",
            () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 11)
                    .icon(() -> new ItemStack(ModItems.ABILITY_SCROLL.get()))
                    .title(Component.translatable("itemGroup.creraces.scrolls"))
                    .displayItems((parameters, output) -> {
                        // Add default scroll
                        output.accept(ModItems.ABILITY_SCROLL.get());

                        // Add all specific scrolls
                        AbilityRegistry.getAll().forEach(ability -> {
                            output.accept(ScrollItem.create(ability.id()));
                        });
                    }).build());

    public static void register() {
        TABS.register();
    }
}
