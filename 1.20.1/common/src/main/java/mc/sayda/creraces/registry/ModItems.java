package mc.sayda.creraces.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.item.ScrollItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(CreRaces.MODID, Registries.ITEM);

    public static final RegistrySupplier<Item> ABILITY_SCROLL = ITEMS.register("ability_scroll",
            () -> new ScrollItem(new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> MIRROR = ITEMS.register("mirror",
            () -> new mc.sayda.creraces.item.MirrorItem(new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> HARPY_FEATHER = ITEMS.register("harpy_feather",
            () -> new Item(new Item.Properties()));

    public static void register() {
        ITEMS.register();
    }
}
