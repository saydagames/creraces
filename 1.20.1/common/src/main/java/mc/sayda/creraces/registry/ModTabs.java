package mc.sayda.creraces.registry;

import dev.architectury.registry.CreativeTabRegistry;
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
            () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .icon(() -> new ItemStack(ModItems.ABILITY_SCROLL.get()))
                    .title(Component.translatable("itemGroup.creraces.main"))
                    .displayItems((parameters, output) -> {
                        // Add default items
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
