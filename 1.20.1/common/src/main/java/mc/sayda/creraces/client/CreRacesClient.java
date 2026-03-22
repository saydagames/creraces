package mc.sayda.creraces.client;

import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.client.screen.SkillWheelScreen;
import dev.architectury.registry.menu.MenuRegistry;
import mc.sayda.creraces.capability.DataUtils;
import dev.architectury.platform.Platform;

public class CreRacesClient {
        private static net.minecraft.world.entity.player.Player lastPlayerInstance = null;
        private static long lastSelectionScreenTime = 0;

        public static void init() {
                ModKeyMappings.register();
                mc.sayda.creraces.network.BoundaryHandler.registerS2C();
                mc.sayda.creraces.client.SpiritMobilityClient.init();

                // Particles
                dev.architectury.registry.client.particle.ParticleProviderRegistry.register(
                                mc.sayda.creraces.registry.ModParticles.MARKER,
                                mc.sayda.creraces.client.particle.MarkerParticle.Provider::new);
                dev.architectury.registry.client.particle.ParticleProviderRegistry.register(
                                mc.sayda.creraces.registry.ModParticles.MARKER_MOVE,
                                mc.sayda.creraces.client.particle.MarkerMoveParticle.Provider::new);
                dev.architectury.registry.client.particle.ParticleProviderRegistry.register(
                                mc.sayda.creraces.registry.ModParticles.MARKER_ATTACK,
                                mc.sayda.creraces.client.particle.MarkerAttackParticle.Provider::new);
                dev.architectury.registry.client.particle.ParticleProviderRegistry.register(
                                mc.sayda.creraces.registry.ModParticles.POISON_EMITTER,
                                mc.sayda.creraces.client.particle.PoisonEmitterParticle.Provider::new);

                // Menu Registration (Directly in init for Fabric as requested, Forge is handled in CLIENT_SETUP below)
                if (Platform.isFabric()) {
                        MenuRegistry.registerScreenFactory(mc.sayda.creraces.registry.ModMenuTypes.MENU_GUI.get(),
                                        mc.sayda.creraces.client.screen.MenuGUIScreen::new);
                        MenuRegistry.registerScreenFactory(mc.sayda.creraces.registry.ModMenuTypes.MIRROR_GUI.get(),
                                        mc.sayda.creraces.client.screen.DynamicMirrorScreen::new);
                }

                // Register renderers early so Architectury can hook into Forge events
                dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                                mc.sayda.creraces.registry.ModEntities.FEATHER_PROJECTILE,
                                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);

                // TrollPillarEntity - stone pillar with custom Blockbench model
                dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                                mc.sayda.creraces.client.model.TrollPillarModel.LAYER_LOCATION,
                                mc.sayda.creraces.client.model.TrollPillarModel::createBodyLayer);
                dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                                mc.sayda.creraces.registry.ModEntities.TROLL_PILLAR,
                                mc.sayda.creraces.client.render.TrollPillarRenderer::new);

                // PoisonEmitter - custom ratkin totem model
                dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                                mc.sayda.creraces.client.model.PoisonEmitterModel.LAYER_LOCATION,
                                mc.sayda.creraces.client.model.PoisonEmitterModel::createBodyLayer);
                dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                                mc.sayda.creraces.registry.ModEntities.POISON_EMITTER,
                                mc.sayda.creraces.client.render.PoisonEmitterRenderer::new);

                // PoisonEmitter Mobile - totem with wheels
                dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                                mc.sayda.creraces.client.model.PoisonEmitterMobileModel.LAYER_LOCATION,
                                mc.sayda.creraces.client.model.PoisonEmitterMobileModel::createBodyLayer);
                dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                                mc.sayda.creraces.registry.ModEntities.POISON_EMITTER_MOBILE,
                                mc.sayda.creraces.client.render.PoisonEmitterMobileRenderer::new);

                // Tornado - aria legacy entity
                dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                                mc.sayda.creraces.client.model.TornadoModel.LAYER_LOCATION,
                                mc.sayda.creraces.client.model.TornadoModel::createBodyLayer);
                dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                                mc.sayda.creraces.registry.ModEntities.TORNADO,
                                mc.sayda.creraces.client.render.TornadoRenderer::new);

                // Undead Remains
                dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                                mc.sayda.creraces.client.model.RemainsModel.LAYER_LOCATION,
                                mc.sayda.creraces.client.model.RemainsModel::createBodyLayer);
                dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                                mc.sayda.creraces.registry.ModEntities.REMAINS,
                                mc.sayda.creraces.client.render.RemainsRenderer::new);
                dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                                mc.sayda.creraces.registry.ModEntities.REMAINS_UNDEAD,
                                mc.sayda.creraces.client.render.RemainsRenderer::new);

                // Torii Bell - custom model with per-face textures
                dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                                mc.sayda.creraces.client.render.ToriBellRenderer.LAYER_LOCATION,
                                mc.sayda.creraces.client.render.ToriBellRenderer::createBodyLayer);

                dev.architectury.event.events.client.ClientLifecycleEvent.CLIENT_SETUP.register(instance -> {
                        // Menu Registration (Forge needs this late to avoid registry race conditions)
                        if (Platform.isForge()) {
                                MenuRegistry.registerScreenFactory(mc.sayda.creraces.registry.ModMenuTypes.MENU_GUI.get(),
                                                mc.sayda.creraces.client.screen.MenuGUIScreen::new);
                                MenuRegistry.registerScreenFactory(mc.sayda.creraces.registry.ModMenuTypes.MIRROR_GUI.get(),
                                                mc.sayda.creraces.client.screen.DynamicMirrorScreen::new);
                        }

                        // Register microblock renderer
                        dev.architectury.registry.client.rendering.BlockEntityRendererRegistry.register(
                                        mc.sayda.creraces.registry.ModBlocks.MICRO_BLOCK_ENTITY.get(),
                                        context -> new mc.sayda.creraces.client.render.MiniBlockEntityRenderer(
                                                        context));

                        dev.architectury.registry.client.rendering.BlockEntityRendererRegistry.register(
                                        mc.sayda.creraces.registry.ModBlocks.TORI_BELL_ENTITY.get(),
                                        mc.sayda.creraces.client.render.ToriBellRenderer::new);

                        // RenderType Registration
                        dev.architectury.registry.client.rendering.RenderTypeRegistry.register(
                                        net.minecraft.client.renderer.RenderType.cutout(),
                                        mc.sayda.creraces.registry.ModBlocks.TORI_BELL.get(),
                                        mc.sayda.creraces.registry.ModBlocks.WEATHERED_TORI_BELL.get(),
                                        mc.sayda.creraces.registry.ModBlocks.DRYAD_LEAVES.get(),
                                        mc.sayda.creraces.registry.ModBlocks.DRYAD_LEAVES_FLOWERING.get(),
                                        mc.sayda.creraces.registry.ModBlocks.DRYAD_LEAVES_FRUIT.get(),
                                        mc.sayda.creraces.registry.ModBlocks.DRYAD_LANTERN.get(),
                                        mc.sayda.creraces.registry.ModBlocks.RAT_HOLE.get());

                        // Dryad Sapling specifically isolated to ensure correct cutout rendering
                        dev.architectury.registry.client.rendering.RenderTypeRegistry.register(
                                        net.minecraft.client.renderer.RenderType.cutout(),
                                        mc.sayda.creraces.registry.ModBlocks.DRYAD_SAPLING.get());
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
                        mc.sayda.creraces.engine.ActionRegistry.cleanup(player);
                });

                ClientTickEvent.CLIENT_POST.register(minecraft -> {
                        if (minecraft.player != null && !minecraft.player.isRemoved()) {
                                // Detect player change (respawn/join)
                                if (minecraft.player != lastPlayerInstance) {
                                        lastPlayerInstance = minecraft.player;
                                        mc.sayda.creraces.client.ClientAccess.hasReceivedInitialSync = false;
                                        mc.sayda.creraces.client.ClientAccess.isWaitingForRaceSelection = false;
                                        lastSelectionScreenTime = System.currentTimeMillis();
                                }

                                DataUtils.getVariables(minecraft.player).ifPresent(vars -> {
                                        // Forced selection logic
                                        if (mc.sayda.creraces.config.CreRacesConfig.FORCED_SELECTION.get()
                                                        && minecraft.player.isAlive()
                                                        && !vars.hasChosenRace()
                                                        && !mc.sayda.creraces.client.ClientAccess.isWaitingForRaceSelection
                                                        && mc.sayda.creraces.client.ClientAccess.hasReceivedInitialSync) {

                                                boolean isCreScreen = minecraft.screen instanceof mc.sayda.creraces.client.screen.RaceSelectionScreen
                                                                || minecraft.screen instanceof mc.sayda.creraces.client.screen.RaceDetailsScreen
                                                                || minecraft.screen instanceof mc.sayda.creraces.client.screen.SubRaceScreen
                                                                || minecraft.screen instanceof mc.sayda.creraces.client.screen.MenuGUIScreen
                                                                || minecraft.screen instanceof mc.sayda.creraces.client.screen.DebugScreen
                                                                || minecraft.screen instanceof mc.sayda.creraces.client.screen.DynamicMirrorScreen;

                                                long now = System.currentTimeMillis();
                                                if (!isCreScreen && now - lastSelectionScreenTime > 1000) {
                                                        // Direct open selection screen instead of menu for reliability
                                                        mc.sayda.creraces.network.BoundaryHandler.sendOpenMenu();
                                                        lastSelectionScreenTime = now;
                                                }
                                        } else {
                                                mc.sayda.creraces.client.ClientAccess.isWaitingForRaceSelection = false;
                                        }
                                });
                        }

                        while (ModKeyMappings.SKILL_WHEEL.consumeClick()) {
                                minecraft.setScreen(new SkillWheelScreen());
                        }

                        while (ModKeyMappings.ABILITY_A1.consumeClick()) {
                                mc.sayda.creraces.network.BoundaryHandler.sendCastAbility(
                                                new mc.sayda.creraces.network.CastAbilityPacket(
                                                                mc.sayda.creraces.ability.AbilitySlot.A1));
                        }

                        while (ModKeyMappings.ABILITY_A2.consumeClick()) {
                                mc.sayda.creraces.network.BoundaryHandler.sendCastAbility(
                                                new mc.sayda.creraces.network.CastAbilityPacket(
                                                                mc.sayda.creraces.ability.AbilitySlot.A2));
                        }

                        while (ModKeyMappings.ABILITY_A3.consumeClick()) {
                                mc.sayda.creraces.network.BoundaryHandler.sendCastAbility(
                                                new mc.sayda.creraces.network.CastAbilityPacket(
                                                                mc.sayda.creraces.ability.AbilitySlot.A3));
                        }

                        while (ModKeyMappings.ABILITY_A4.consumeClick()) {
                                mc.sayda.creraces.network.BoundaryHandler.sendCastAbility(
                                                new mc.sayda.creraces.network.CastAbilityPacket(
                                                                mc.sayda.creraces.ability.AbilitySlot.A4));
                        }

                        while (ModKeyMappings.ABILITY_A5.consumeClick()) {
                                mc.sayda.creraces.network.BoundaryHandler.sendCastAbility(
                                                new mc.sayda.creraces.network.CastAbilityPacket(
                                                                mc.sayda.creraces.ability.AbilitySlot.A5));
                        }

                        while (ModKeyMappings.MENU_GUI.consumeClick()) {
                                mc.sayda.creraces.network.BoundaryHandler.sendOpenMenu();
                        }
                });

                CreRaces.LOGGER.info("CreRaces Client initialized.");
        }
}
