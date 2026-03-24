package mc.sayda.creraces.forge;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.client.CreRacesClient;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CreRaces.MODID)
public class CreRacesForge {

    public CreRacesForge() {
                IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
                net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                                net.minecraftforge.fml.config.ModConfig.Type.COMMON,
                                mc.sayda.creraces.config.forge.ForgeConfig.COMMON_SPEC,
                                "creraces/creraces-common.toml");
                net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                                net.minecraftforge.fml.config.ModConfig.Type.CLIENT,
                                mc.sayda.creraces.config.forge.ForgeConfig.CLIENT_SPEC,
                                "creraces/creraces-client.toml");
                net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                                net.minecraftforge.fml.config.ModConfig.Type.COMMON,
                                mc.sayda.creraces.config.forge.ForgeConfig.ENTITIES_SPEC,
                                "creraces/creraces-entities.toml");

                dev.architectury.platform.forge.EventBuses.registerModEventBus(CreRaces.MODID, modBus);
                mc.sayda.creraces.util.PlatformServices.burnTimeHandler = stack -> net.minecraftforge.common.ForgeHooks
                                .getBurnTime(stack, null);
                CreRaces.init();
                // Register Forge-specific player data events for authoritative NBT load/save
                // MinecraftForge.EVENT_BUS.register(this);
                net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                                () -> CreRacesClient::init);
        }
}
