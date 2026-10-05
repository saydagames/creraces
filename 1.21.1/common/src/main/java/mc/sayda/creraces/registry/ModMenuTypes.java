package mc.sayda.creraces.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.item.EssenceBeltItem;
import mc.sayda.creraces.world.inventory.EssenceBeltMenu;
import mc.sayda.creraces.world.inventory.QuestBoardMenu;
import mc.sayda.creraces.world.inventory.ResearchTableMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.world.item.ItemStack;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(CreRaces.MODID, Registries.MENU);

    public static final RegistrySupplier<MenuType<ResearchTableMenu>> RESEARCH_TABLE = MENUS.register("research_table",
            () -> MenuRegistry.ofExtended((syncId, inventory, buf) -> new ResearchTableMenu(syncId, inventory, buf)));

    public static final RegistrySupplier<MenuType<QuestBoardMenu>> QUEST_BOARD = MENUS.register("quest_board",
            () -> MenuRegistry.ofExtended((syncId, inventory, buf) -> new QuestBoardMenu(syncId, inventory, buf)));

    public static final RegistrySupplier<MenuType<EssenceBeltMenu>> ESSENCE_BELT = MENUS.register("essence_belt",
            () -> MenuRegistry.ofExtended((syncId, inventory, buf) -> new EssenceBeltMenu(
                    syncId, inventory,
                    EssenceBeltItem.loadInventory(ItemStack.EMPTY,
                            inventory.player.level().registryAccess()))));

    public static void register() {
        MENUS.register();
    }
}
