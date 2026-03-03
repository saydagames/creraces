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
        mc.sayda.creraces.engine.SpiritSpawningHandler.init();

        dev.architectury.event.events.common.BlockEvent.BREAK.register((level, pos, state, player, xp) -> {
            if (state.is(mc.sayda.creraces.registry.ModBlocks.MICRO_BLOCK.get())) {
                boolean isSmallBuild = mc.sayda.creraces.capability.DataUtils.getVariables(player)
                        .map(mc.sayda.creraces.capability.IPlayerVariables::isSmallBuild)
                        .orElse(false);

                if (isSmallBuild) {
                    // Minibuild users are strictly forbidden from breaking the host block
                    return dev.architectury.event.EventResult.interruptFalse();
                } else if (!player.isShiftKeyDown()) {
                    // Global rule: Non-minibuild users must sneak to break it
                    return dev.architectury.event.EventResult.interruptFalse();
                }
            }
            return dev.architectury.event.EventResult.pass();
        });

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

        // Blocks
        mc.sayda.creraces.registry.ModBlocks.register();
        // Sounds
        mc.sayda.creraces.registry.ModSounds.register();
        // Items
        mc.sayda.creraces.registry.ModItems.register();
        // Tabs
        mc.sayda.creraces.registry.ModTabs.register();
        // Menus
        mc.sayda.creraces.registry.ModMenuTypes.register();

        LOGGER.info("CreRaces initialized (Common).");
    }
}