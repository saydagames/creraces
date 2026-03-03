package mc.sayda.creraces.fabric;

import mc.sayda.creraces.CreRaces;
import net.fabricmc.api.ModInitializer;

public class CreRacesFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        mc.sayda.creraces.config.fabric.FabricConfig.load();
        mc.sayda.creraces.util.PlatformServices.burnTimeHandler = stack -> net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
                .getFuel().getOrDefault(stack.getItem(), 0);
        CreRaces.init();
    }
}
