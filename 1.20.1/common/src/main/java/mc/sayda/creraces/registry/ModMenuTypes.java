package mc.sayda.creraces.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.world.inventory.MenuGUIMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import dev.architectury.registry.menu.MenuRegistry;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(CreRaces.MODID, Registries.MENU);

    public static final RegistrySupplier<MenuType<MenuGUIMenu>> MENU_GUI = MENUS.register("menu_gui",
            () -> MenuRegistry.ofExtended((syncId, inventory, buf) -> new MenuGUIMenu(syncId, inventory, buf)));

    public static void register() {
        MENUS.register();
    }
}
