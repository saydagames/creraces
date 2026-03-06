package mc.sayda.creraces.client;

import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.client.screen.SkillWheelScreen;
import dev.architectury.registry.menu.MenuRegistry;
import mc.sayda.creraces.capability.DataUtils;

public class CreRacesClient {
    private static long lastSelectionScreenTime = 0;

    public static void init() {
        ModKeyMappings.register();
        mc.sayda.creraces.network.BoundaryHandler.registerS2C();
        mc.sayda.creraces.client.SpiritMobilityClient.init();

        // Register renderers early so Architectury can hook into Forge events
        dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                mc.sayda.creraces.registry.ModEntities.FEATHER_PROJECTILE,
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);

        // TrollPillarEntity — stone pillar with custom Blockbench model
        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                mc.sayda.creraces.client.model.TrollPillarModel.LAYER_LOCATION,
                mc.sayda.creraces.client.model.TrollPillarModel::createBodyLayer);
        dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                mc.sayda.creraces.registry.ModEntities.TROLL_PILLAR,
                mc.sayda.creraces.client.render.TrollPillarRenderer::new);

        // PoisonEmitter — custom ratkin totem model
        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                mc.sayda.creraces.client.model.PoisonEmitterModel.LAYER_LOCATION,
                mc.sayda.creraces.client.model.PoisonEmitterModel::createBodyLayer);
        dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                mc.sayda.creraces.registry.ModEntities.POISON_EMITTER,
                mc.sayda.creraces.client.render.PoisonEmitterRenderer::new);
        dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                mc.sayda.creraces.registry.ModEntities.POISON_EMITTER_MOBILE,
                mc.sayda.creraces.client.render.PoisonEmitterRenderer::new);

        dev.architectury.event.events.client.ClientLifecycleEvent.CLIENT_SETUP.register(instance -> {
            // Register screen factory here instead of main init because
            // ModMenuTypes.MENU_GUI.get()
            // throws NullPointerException on Forge if called before registry
            // initialization.
            MenuRegistry.registerScreenFactory(mc.sayda.creraces.registry.ModMenuTypes.MENU_GUI.get(),
                    mc.sayda.creraces.client.screen.MenuGUIScreen::new);

            // Register microblock renderer
            dev.architectury.registry.client.rendering.BlockEntityRendererRegistry.register(
                    mc.sayda.creraces.registry.ModBlocks.MICRO_BLOCK_ENTITY.get(),
                    context -> new mc.sayda.creraces.client.render.MiniBlockEntityRenderer(context));

            // RenderType Registration
            dev.architectury.registry.client.rendering.RenderTypeRegistry.register(
                    net.minecraft.client.renderer.RenderType.cutout(),
                    mc.sayda.creraces.registry.ModBlocks.TORI_BELL.get(),
                    mc.sayda.creraces.registry.ModBlocks.WEATHERED_TORI_BELL.get(),
                    mc.sayda.creraces.registry.ModBlocks.DRYAD_LEAVES.get(),
                    mc.sayda.creraces.registry.ModBlocks.DRYAD_LEAVES_FLOWERING.get(),
                    mc.sayda.creraces.registry.ModBlocks.DRYAD_LEAVES_FRUIT.get(),
                    mc.sayda.creraces.registry.ModBlocks.DRYAD_SAPLING.get(),
                    mc.sayda.creraces.registry.ModBlocks.DRYAD_LANTERN.get(),
                    mc.sayda.creraces.registry.ModBlocks.RAT_HOLE.get());
            dev.architectury.registry.client.rendering.RenderTypeRegistry.register(
                    net.minecraft.client.renderer.RenderType.translucent(),
                    mc.sayda.creraces.registry.ModBlocks.MICRO_BLOCK.get());

            // Foliage Tints (Dryad leaves are natively green, so they don't need biome
            // tints)
            /*
             * net.minecraft.client.color.block.BlockColors blockColors =
             * net.minecraft.client.Minecraft.getInstance()
             * .getBlockColors();
             * blockColors.register((state, world, pos, tintIndex) -> {
             * if (world == null || pos == null) {
             * return net.minecraft.world.level.FoliageColor.getDefaultColor();
             * }
             * return
             * net.minecraft.client.renderer.BiomeColors.getAverageFoliageColor(world, pos);
             * }, ModBlocks.DRYAD_LEAVES.get(), ModBlocks.DRYAD_LEAVES_FLOWERING.get(),
             * ModBlocks.DRYAD_LEAVES_FRUIT.get());
             */
        });

        ClientGuiEvent.RENDER_HUD.register((graphics, tickDelta) -> {
            RaceOverlay.render(graphics, tickDelta);
            mc.sayda.creraces.client.render.SpiritRealmRenderer.renderScreenTint(graphics);
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
                    // Forced selection logic
                    if (mc.sayda.creraces.config.CreRacesConfig.FORCED_SELECTION.get() && !vars.hasChosenRace()
                            && mc.sayda.creraces.client.ClientAccess.hasReceivedInitialSync) {

                        // If already waiting or screen is open, update timer
                        if (minecraft.screen != null
                                || mc.sayda.creraces.client.ClientAccess.isWaitingForRaceSelection) {
                            lastSelectionScreenTime = System.currentTimeMillis();
                            mc.sayda.creraces.client.ClientAccess.isWaitingForRaceSelection = false;
                        } else {
                            // Enforce menu after a short delay (2 seconds) to avoid flicker if they just
                            // closed it
                            long now = System.currentTimeMillis();
                            if (now - lastSelectionScreenTime > 2000) {
                                mc.sayda.creraces.client.ClientAccess.isWaitingForRaceSelection = true;
                                mc.sayda.creraces.network.BoundaryHandler.sendOpenMenu();
                                lastSelectionScreenTime = now;
                            }
                        }
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
