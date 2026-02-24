package mc.sayda.creraces.client;

import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.client.screen.SkillWheelScreen;
import mc.sayda.creraces.client.screen.MenuGUIScreen;
import mc.sayda.creraces.world.inventory.MenuGUIMenu;
import dev.architectury.registry.menu.MenuRegistry;
import mc.sayda.creraces.registry.ModMenuTypes;
import mc.sayda.creraces.capability.DataUtils;
import net.minecraft.client.Minecraft;

public class CreRacesClient {
    public static void init() {
        ModKeyMappings.register();
        mc.sayda.creraces.network.BoundaryHandler.registerS2C();

        // Register renderers early so Architectury can hook into Forge events
        dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                mc.sayda.creraces.registry.ModEntities.FEATHER_PROJECTILE,
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);

        dev.architectury.event.events.client.ClientLifecycleEvent.CLIENT_SETUP.register(instance -> {
            MenuRegistry.registerScreenFactory(ModMenuTypes.MENU_GUI.get(), MenuGUIScreen::new);
        });

        ClientGuiEvent.RENDER_HUD.register((graphics, tickDelta) -> {
            RaceOverlay.render(graphics, tickDelta);
        });

        dev.architectury.event.events.client.ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player -> {
            mc.sayda.creraces.network.BoundaryHandler.sendSyncRequest();
            CreRaces.LOGGER.info("CreRacesClient: Requested initial sync from server.");
        });

        dev.architectury.event.events.client.ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> {
            mc.sayda.creraces.client.ClientAccess.hasReceivedInitialSync = false;
        });

        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            if (minecraft.player != null && !minecraft.player.isRemoved()) {
                DataUtils.getVariables(minecraft.player).ifPresent(vars -> {
                    // Client-side resource ticking for smooth UI updates
                    // PREDICTION: We run the ticker locally to predict resource regen/decay
                    mc.sayda.creraces.race.ResourceTicker.tick(minecraft.player);

                    // Forced selection logic
                    if (mc.sayda.creraces.config.CreRacesConfig.FORCED_SELECTION.get() && !vars.hasChosenRace()
                            && mc.sayda.creraces.client.ClientAccess.hasReceivedInitialSync
                            && !mc.sayda.creraces.client.ClientAccess.isWaitingForRaceSelection
                            && minecraft.screen == null) {
                        mc.sayda.creraces.network.BoundaryHandler.sendOpenMenu();
                    }
                });
            }

            while (ModKeyMappings.SKILL_WHEEL.consumeClick()) {
                minecraft.setScreen(new SkillWheelScreen());
            }

            while (ModKeyMappings.ABILITY_A1.consumeClick()) {
                mc.sayda.creraces.network.BoundaryHandler.sendCastAbility(
                        new mc.sayda.creraces.network.CastAbilityPacket(mc.sayda.creraces.ability.AbilitySlot.A1));
            }

            while (ModKeyMappings.ABILITY_A2.consumeClick()) {
                mc.sayda.creraces.network.BoundaryHandler.sendCastAbility(
                        new mc.sayda.creraces.network.CastAbilityPacket(mc.sayda.creraces.ability.AbilitySlot.A2));
            }

            while (ModKeyMappings.ABILITY_A3.consumeClick()) {
                mc.sayda.creraces.network.BoundaryHandler.sendCastAbility(
                        new mc.sayda.creraces.network.CastAbilityPacket(mc.sayda.creraces.ability.AbilitySlot.A3));
            }

            while (ModKeyMappings.ABILITY_A4.consumeClick()) {
                mc.sayda.creraces.network.BoundaryHandler.sendCastAbility(
                        new mc.sayda.creraces.network.CastAbilityPacket(mc.sayda.creraces.ability.AbilitySlot.A4));
            }

            while (ModKeyMappings.ABILITY_A5.consumeClick()) {
                mc.sayda.creraces.network.BoundaryHandler.sendCastAbility(
                        new mc.sayda.creraces.network.CastAbilityPacket(mc.sayda.creraces.ability.AbilitySlot.A5));
            }

            while (ModKeyMappings.MENU_GUI.consumeClick()) {
                mc.sayda.creraces.network.BoundaryHandler.sendOpenMenu();
            }
        });

        CreRaces.LOGGER.info("CreRaces Client initialized.");
    }
}
