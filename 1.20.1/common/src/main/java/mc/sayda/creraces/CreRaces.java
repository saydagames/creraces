package mc.sayda.creraces;

import com.mojang.logging.LogUtils;
import dev.architectury.registry.ReloadListenerRegistry;
import mc.sayda.creraces.ability.AbilityManager;
import mc.sayda.creraces.network.BoundaryHandler;
import net.minecraft.server.packs.PackType;
import org.slf4j.Logger;

/**
 * Main mod class for CreRaces (Common).
 * Handles initialization and event registration using Architectury API.
 */
public class CreRaces {
    public static final String MODID = "creraces";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {
        LOGGER.info("CreRaces is loading...");

        // Documentation Cache
        mc.sayda.creraces.util.DocCache.init(dev.architectury.platform.Platform.getConfigFolder());

        // Register data reload listeners
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new AbilityManager());
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new mc.sayda.creraces.race.RaceManager());

        // Network
        BoundaryHandler.registerC2S();
        mc.sayda.creraces.ability.ModAbilities.registerExecutors();

        // Engine Registries
        mc.sayda.creraces.engine.ActionRegistry.init();
        mc.sayda.creraces.engine.TraitRegistry.init();

        // Events
        IncidentResolver.init();

        // Register Commands
        dev.architectury.event.events.common.CommandRegistrationEvent.EVENT
                .register((dispatcher, registry, selection) -> {
                    mc.sayda.creraces.commands.CreracesCommand.register(dispatcher);
                });

        // Attributes
        mc.sayda.creraces.registry.ModAttributes.init();

        // Enchantments
        mc.sayda.creraces.registry.ModEnchantments.register();

        // Mob Effects
        mc.sayda.creraces.registry.ModMobEffects.register();

        // Entities
        mc.sayda.creraces.registry.ModEntities.register();

        // Items
        mc.sayda.creraces.registry.ModItems.register();
        // Tabs
        mc.sayda.creraces.registry.ModTabs.register();
        // Menus
        mc.sayda.creraces.registry.ModMenuTypes.register();

        LOGGER.info("CreRaces initialized (Common).");
    }
}