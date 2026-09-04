package mc.sayda.creraces.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import dev.architectury.registry.menu.MenuRegistry;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(CreRaces.MODID, Registries.MENU);

    public static final RegistrySupplier<MenuType<mc.sayda.creraces.world.inventory.ResearchTableMenu>> RESEARCH_TABLE = MENUS.register("research_table",
            () -> MenuRegistry.ofExtended((syncId, inventory, buf) -> new mc.sayda.creraces.world.inventory.ResearchTableMenu(syncId, inventory, buf)));

    public static final RegistrySupplier<MenuType<mc.sayda.creraces.world.inventory.QuestBoardMenu>> QUEST_BOARD = MENUS.register("quest_board",
            () -> MenuRegistry.ofExtended((syncId, inventory, buf) -> new mc.sayda.creraces.world.inventory.QuestBoardMenu(syncId, inventory, buf)));

    public static final RegistrySupplier<MenuType<mc.sayda.creraces.world.inventory.EssenceBeltMenu>> ESSENCE_BELT = MENUS.register("essence_belt",
            () -> MenuRegistry.ofExtended((syncId, inventory, buf) -> new mc.sayda.creraces.world.inventory.EssenceBeltMenu(
                    syncId, inventory,
                    mc.sayda.creraces.item.EssenceBeltItem.loadInventory(net.minecraft.world.item.ItemStack.EMPTY,
                            inventory.player.level().registryAccess()))));

    public static void register() {
        MENUS.register();
    }
}
