package mc.sayda.creraces.client;

import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.client.screen.HUDEditorScreen;
import mc.sayda.creraces.client.screen.SkillWheelScreen;
import net.minecraft.client.Minecraft;
import dev.architectury.registry.menu.MenuRegistry;
import mc.sayda.creraces.capability.DataUtils;
import dev.architectury.platform.Platform;

public class CreRacesClient {
        private static net.minecraft.world.entity.player.Player lastPlayerInstance = null;

        public static void init() {
                ModKeyMappings.register();
                mc.sayda.creraces.network.BoundaryHandler.registerS2C();
                mc.sayda.creraces.client.SpiritMobilityClient.init();

                registerParticles();

                // Menu Registration - Fabric can register immediately; Forge needs CLIENT_SETUP to avoid a registry race condition (see the Forge branch below).
                if (Platform.isFabric()) {
                        MenuRegistry.registerScreenFactory(mc.sayda.creraces.registry.ModMenuTypes.MENU_GUI.get(),
                                        mc.sayda.creraces.client.screen.MenuGUIScreen::new);
                        MenuRegistry.registerScreenFactory(mc.sayda.creraces.registry.ModMenuTypes.MIRROR_GUI.get(),
                                        mc.sayda.creraces.client.screen.DynamicMirrorScreen::new);
                        MenuRegistry.registerScreenFactory(mc.sayda.creraces.registry.ModMenuTypes.RESEARCH_TABLE.get(),
                                        mc.sayda.creraces.client.screen.ResearchTableScreen::new);
                        MenuRegistry.registerScreenFactory(mc.sayda.creraces.registry.ModMenuTypes.ESSENCE_BELT.get(),
                                        mc.sayda.creraces.client.screen.EssenceBeltScreen::new);
                }

                registerEntityRenderers();
                registerClientSetupHandlers();

                ClientGuiEvent.RENDER_HUD.register((graphics, tickDelta) -> {
                        // Skip normal HUD rendering while the editor is open (it renders itself)
                        if (!(Minecraft.getInstance().screen instanceof HUDEditorScreen)) {
                                RaceOverlay.render(graphics, tickDelta);
                        }
                        mc.sayda.creraces.client.render.SpiritRealmRenderer.renderScreenTint(graphics);
                });

                dev.architectury.event.events.client.ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player -> {
                        mc.sayda.creraces.client.ClientAccess.lastSyncedPlayer = null;
                        mc.sayda.creraces.network.BoundaryHandler.sendSyncRequest();
                        mc.sayda.creraces.client.screen.TerritoryMapScreen.clearCache();
                        CreRaces.LOGGER.info("CreRacesClient: Requested initial sync from server.");
                });

                dev.architectury.event.events.client.ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> {
                        mc.sayda.creraces.client.ClientAccess.lastSyncedPlayer = null;
                        mc.sayda.creraces.client.ClientAccess.isWaitingForRaceSelection = false;
                        mc.sayda.creraces.engine.ActionRegistry.cleanup(player);
                        mc.sayda.creraces.client.SpiritMobilityClient.reset();
                });

                registerTickHandler();

                CreRaces.LOGGER.info("CreRaces Client initialized.");
        }

        private static void registerParticles() {
                // Particles
                dev.architectury.registry.client.particle.ParticleProviderRegistry.register(
                                mc.sayda.creraces.registry.ModParticles.MARKER,
                                mc.sayda.creraces.client.particle.MarkerParticle.Provider::new);
                dev.architectury.registry.client.particle.ParticleProviderRegistry.register(
                                mc.sayda.creraces.registry.ModParticles.MARKER_MOVE,
                                mc.sayda.creraces.client.particle.MarkerParticle.Provider::new);
                dev.architectury.registry.client.particle.ParticleProviderRegistry.register(
                                mc.sayda.creraces.registry.ModParticles.MARKER_ATTACK,
                                mc.sayda.creraces.client.particle.MarkerParticle.Provider::new);
                dev.architectury.registry.client.particle.ParticleProviderRegistry.register(
                                mc.sayda.creraces.registry.ModParticles.POISON_EMITTER,
                                mc.sayda.creraces.client.particle.PoisonEmitterParticle.Provider::new);
                dev.architectury.registry.client.particle.ParticleProviderRegistry.register(
                                mc.sayda.creraces.registry.ModParticles.MAGIC_DAMAGE,
                                mc.sayda.creraces.client.particle.DamageCritParticle.Provider::new);
                dev.architectury.registry.client.particle.ParticleProviderRegistry.register(
                                mc.sayda.creraces.registry.ModParticles.PHYSICAL_DAMAGE,
                                mc.sayda.creraces.client.particle.DamageCritParticle.Provider::new);
                dev.architectury.registry.client.particle.ParticleProviderRegistry.register(
                                mc.sayda.creraces.registry.ModParticles.TRUE_DAMAGE,
                                mc.sayda.creraces.client.particle.DamageCritParticle.Provider::new);
                dev.architectury.registry.client.particle.ParticleProviderRegistry.register(
                                mc.sayda.creraces.registry.ModParticles.VEIL_EMBER,
                                mc.sayda.creraces.client.particle.VeilEmberParticle.Provider::new);
                dev.architectury.registry.client.particle.ParticleProviderRegistry.register(
                                mc.sayda.creraces.registry.ModParticles.VEIL_MIST,
                                mc.sayda.creraces.client.particle.VeilMistParticle.Provider::new);
                dev.architectury.registry.client.particle.ParticleProviderRegistry.register(
                                mc.sayda.creraces.registry.ModParticles.ESSENCE_PARTICLE,
                                mc.sayda.creraces.client.particle.EssenceParticle.Provider::new);

                // Veilwood night-only ambient particles (replaces biome JSON particle so we can gate by time)
                // 0.004 in biome JSON is checked ~667×/tick by vanilla; we check once/tick so use 0.09 (~1.8/sec)
                ClientTickEvent.CLIENT_POST.register(client -> {
                        if (client.isPaused()) return;
                        net.minecraft.client.multiplayer.ClientLevel level = client.level;
                        net.minecraft.world.entity.player.Player player = client.player;
                        if (level == null || player == null) return;
                        long timeOfDay = level.getDayTime() % 24000L;
                        if (timeOfDay < 13000L || timeOfDay > 23000L) return; // daytime
                        if (!level.getBiome(player.blockPosition())
                                .is(new net.minecraft.resources.ResourceLocation("creraces", "veilwood_forest"))) return;
                        int sparks = 2 + level.random.nextInt(2);
                        for (int i = 0; i < sparks; i++) {
                                level.addParticle(mc.sayda.creraces.registry.ModParticles.VEIL_EMBER.get(),
                                        player.getX() + (level.random.nextDouble() - 0.5) * 20,
                                        player.getY() + level.random.nextDouble() * 8,
                                        player.getZ() + (level.random.nextDouble() - 0.5) * 20,
                                        0, 0, 0);
                        }
                });
        }

        private static void registerEntityRenderers() {
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

                // Floating Mote - billboard orb ambient entity
                dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                                mc.sayda.creraces.registry.ModEntities.FLOATING_MOTE,
                                mc.sayda.creraces.client.render.FloatingMoteRenderer::new);

                // Custom boats
                dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                                mc.sayda.creraces.registry.ModEntities.DRYAD_BOAT,
                                ctx -> new mc.sayda.creraces.client.render.DryadBoatRenderer(ctx, false));
                dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                                mc.sayda.creraces.registry.ModEntities.DRYAD_CHEST_BOAT,
                                ctx -> new mc.sayda.creraces.client.render.DryadBoatRenderer(ctx, true));
                dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                                mc.sayda.creraces.registry.ModEntities.VEIL_WILLOW_BOAT,
                                ctx -> new mc.sayda.creraces.client.render.VeilWillowBoatRenderer(ctx, false));
                dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                                mc.sayda.creraces.registry.ModEntities.VEIL_WILLOW_CHEST_BOAT,
                                ctx -> new mc.sayda.creraces.client.render.VeilWillowBoatRenderer(ctx, true));
        }

        private static void registerClientSetupHandlers() {
                // Fabric can run this immediately: architectury-fabric's own client entrypoint may fire
                // CLIENT_SETUP before ours gets a chance to register a listener (Fabric doesn't guarantee
                // entrypoint order between mods), so waiting on the event risks silently missing it entirely.
                // Forge/NeoForge still need the deferred event to avoid registry race conditions.
                if (Platform.isFabric()) {
                        runClientSetupRegistrations();
                } else {
                        dev.architectury.event.events.client.ClientLifecycleEvent.CLIENT_SETUP.register(instance -> runClientSetupRegistrations());
                }
        }

        private static void runClientSetupRegistrations() {
                {
                        // Menu Registration (Forge needs this late to avoid registry race conditions)
                        if (Platform.isForge()) {
                                MenuRegistry.registerScreenFactory(
                                                mc.sayda.creraces.registry.ModMenuTypes.MENU_GUI.get(),
                                                mc.sayda.creraces.client.screen.MenuGUIScreen::new);
                                MenuRegistry.registerScreenFactory(
                                                mc.sayda.creraces.registry.ModMenuTypes.MIRROR_GUI.get(),
                                                mc.sayda.creraces.client.screen.DynamicMirrorScreen::new);
                                MenuRegistry.registerScreenFactory(
                                                mc.sayda.creraces.registry.ModMenuTypes.RESEARCH_TABLE.get(),
                                                mc.sayda.creraces.client.screen.ResearchTableScreen::new);
                                MenuRegistry.registerScreenFactory(
                                                mc.sayda.creraces.registry.ModMenuTypes.ESSENCE_BELT.get(),
                                                mc.sayda.creraces.client.screen.EssenceBeltScreen::new);
                        }

                        // Register microblock renderer
                        dev.architectury.registry.client.rendering.BlockEntityRendererRegistry.register(
                                        mc.sayda.creraces.registry.ModBlocks.MICRO_BLOCK_ENTITY.get(),
                                        context -> new mc.sayda.creraces.client.render.MiniBlockEntityRenderer(
                                                        context));

                        dev.architectury.registry.client.rendering.BlockEntityRendererRegistry.register(
                                        mc.sayda.creraces.registry.ModBlocks.TORII_BELL_ENTITY.get(),
                                        mc.sayda.creraces.client.render.ToriiBellRenderer::new);

                        dev.architectury.registry.client.rendering.BlockEntityRendererRegistry.register(
                                        mc.sayda.creraces.registry.ModBlocks.VEIL_MUSHROOM_BE.get(),
                                        mc.sayda.creraces.client.render.VeilMushroomBlockEntityRenderer::new);

                        dev.architectury.registry.client.rendering.BlockEntityRendererRegistry.register(
                                        mc.sayda.creraces.registry.ModBlocks.VEIL_BLOOM_BE.get(),
                                        mc.sayda.creraces.client.render.ElysianVeilBloomBlockEntityRenderer::new);

                        dev.architectury.registry.client.rendering.BlockEntityRendererRegistry.register(
                                        mc.sayda.creraces.registry.ModBlocks.ESSENCE_VORTEX_ENTITY.get(),
                                        mc.sayda.creraces.client.render.EssenceVortexRenderer::new);


                        dev.architectury.registry.client.rendering.BlockEntityRendererRegistry.register(
                                        mc.sayda.creraces.registry.ModBlocks.VEIL_WILLOW_SAPLING_BE.get(),
                                        mc.sayda.creraces.client.render.VeilWillowSaplingBlockEntityRenderer::new);

                        // Block color tinting
                        dev.architectury.registry.client.rendering.ColorHandlerRegistry.registerBlockColors(
                                        (state, world, pos, tintIndex) -> 0x00FFFF,
                                        mc.sayda.creraces.registry.ModBlocks.VEIL_WILLOW_LEAVES.get(),
                                        mc.sayda.creraces.registry.ModBlocks.VEIL_WILLOW_DRAPE.get());
                        dev.architectury.registry.client.rendering.ColorHandlerRegistry.registerItemColors(
                                        (stack, tintIndex) -> 0x00FFFF,
                                        mc.sayda.creraces.registry.ModBlocks.VEIL_WILLOW_LEAVES.get(),
                                        mc.sayda.creraces.registry.ModBlocks.VEIL_WILLOW_DRAPE.get());

                        // Essence cauldron: tint liquid face (tintindex 0) by stored essence type
                        dev.architectury.registry.client.rendering.ColorHandlerRegistry.registerBlockColors(
                                        (state, world, pos, tintIndex) -> {
                                            if (tintIndex != 0 || world == null || pos == null) return -1;
                                            // Read HAS_ESSENCE from block state (always current at render time)
                                            // to avoid stale block entity data during packet timing
                                            if (state.getValue(mc.sayda.creraces.block.EssenceCauldronBlock.HAS_ESSENCE)) {
                                                if (world.getBlockEntity(pos) instanceof mc.sayda.creraces.block.entity.EssenceCauldronBlockEntity be
                                                        && be.getEssenceType() != null) {
                                                    return be.getEssenceType().getColor();
                                                }
                                            }
                                            return 0x3F76E4; // vanilla water blue
                                        },
                                        mc.sayda.creraces.registry.ModBlocks.ESSENCE_CAULDRON.get());
                        dev.architectury.registry.client.rendering.ColorHandlerRegistry.registerItemColors(
                                        (stack, tintIndex) -> -1,
                                        mc.sayda.creraces.registry.ModBlocks.ESSENCE_CAULDRON.get());
                        // Essence bucket: layer1 overlay tinted by stored essence type
                        dev.architectury.registry.client.rendering.ColorHandlerRegistry.registerItemColors(
                                        (stack, tintIndex) -> {
                                            if (tintIndex != 1) return -1;
                                            mc.sayda.creraces.ability.EssenceType type =
                                                    mc.sayda.creraces.item.EssenceBucketItem.getEssenceType(stack);
                                            return type != null ? type.getColor() : -1;
                                        },
                                        mc.sayda.creraces.registry.ModItems.ESSENCE_BUCKET.get());

                        // Essence type tinting: all shards, bottles, clusters, vortexes share one texture per shape
                        for (mc.sayda.creraces.ability.EssenceType type : mc.sayda.creraces.ability.EssenceType.values()) {
                                int color = type.getColor();
                                dev.architectury.registry.client.rendering.ColorHandlerRegistry.registerItemColors(
                                        (stack, tintIndex) -> tintIndex == 0 ? color : -1,
                                        mc.sayda.creraces.ability.EssenceRegistry.SHARDS.get(type).get(),
                                        mc.sayda.creraces.ability.EssenceRegistry.BOTTLES.get(type).get(),
                                        mc.sayda.creraces.ability.EssenceRegistry.CLUSTER_ITEMS.get(type).get(),
                                        mc.sayda.creraces.ability.EssenceRegistry.VORTEX_ITEMS.get(type).get());
                                dev.architectury.registry.client.rendering.ColorHandlerRegistry.registerBlockColors(
                                        (state, world, pos, tintIndex) -> tintIndex == 0 ? color : -1,
                                        mc.sayda.creraces.ability.EssenceRegistry.CLUSTERS.get(type).get(),
                                        mc.sayda.creraces.ability.EssenceRegistry.VORTEXES.get(type).get());
                        }

                        // RenderType Registration
                        dev.architectury.registry.client.rendering.RenderTypeRegistry.register(
                                        net.minecraft.client.renderer.RenderType.cutout(),
                                        mc.sayda.creraces.registry.ModBlocks.TORII_BELL.get(),
                                        mc.sayda.creraces.registry.ModBlocks.WEATHERED_TORII_BELL.get(),
                                        mc.sayda.creraces.registry.ModBlocks.DRYAD_LEAVES.get(),
                                        mc.sayda.creraces.registry.ModBlocks.DRYAD_LEAVES_FLOWERING.get(),
                                        mc.sayda.creraces.registry.ModBlocks.DRYAD_LEAVES_FRUIT.get(),
                                        mc.sayda.creraces.registry.ModBlocks.DRYAD_LANTERN.get(),
                                        mc.sayda.creraces.registry.ModBlocks.RAT_HOLE.get(),
                                        mc.sayda.creraces.registry.ModBlocks.VEIL_BLOOM.get(),
                                        mc.sayda.creraces.registry.ModBlocks.VEIL_WILLOW_LEAVES.get(),
                                        mc.sayda.creraces.registry.ModBlocks.VEIL_WILLOW_DRAPE.get());

                        // Essence clusters are cross-shaped with transparent pixels
                        // Essence vortexes use translucent so item alpha is respected
                        for (mc.sayda.creraces.ability.EssenceType type : mc.sayda.creraces.ability.EssenceType.values()) {
                                dev.architectury.registry.client.rendering.RenderTypeRegistry.register(
                                        net.minecraft.client.renderer.RenderType.cutout(),
                                        mc.sayda.creraces.ability.EssenceRegistry.CLUSTERS.get(type).get());
                                dev.architectury.registry.client.rendering.RenderTypeRegistry.register(
                                        net.minecraft.client.renderer.RenderType.translucent(),
                                        mc.sayda.creraces.ability.EssenceRegistry.VORTEXES.get(type).get());
                        }

                        // Saplings isolated to ensure correct cutout rendering
                        dev.architectury.registry.client.rendering.RenderTypeRegistry.register(
                                        net.minecraft.client.renderer.RenderType.cutout(),
                                        mc.sayda.creraces.registry.ModBlocks.DRYAD_SAPLING.get(),
                                        mc.sayda.creraces.registry.ModBlocks.VEIL_WILLOW_SAPLING.get(),
                                        mc.sayda.creraces.registry.ModBlocks.VEIL_MUSHROOM.get());
                        dev.architectury.registry.client.rendering.RenderTypeRegistry.register(
                                        net.minecraft.client.renderer.RenderType.translucent(),
                                        mc.sayda.creraces.registry.ModBlocks.MICRO_BLOCK.get());

                        // Fairy source liquid block, rendered translucent like water
                        dev.architectury.registry.client.rendering.RenderTypeRegistry.register(
                                        net.minecraft.client.renderer.RenderType.translucent(),
                                        mc.sayda.creraces.registry.ModBlocks.FAIRY_SOURCE_BLOCK.get());

                }
        }

        private static void registerTickHandler() {
                ClientTickEvent.CLIENT_POST.register(minecraft -> {
                        if (minecraft.player != null && !minecraft.player.isRemoved()) {
                                // Detect player change (respawn/join)
                                if (minecraft.player != lastPlayerInstance) {
                                        lastPlayerInstance = minecraft.player;
                                        mc.sayda.creraces.client.ClientAccess.isWaitingForRaceSelection = false;
                                }

                                DataUtils.getVariables(minecraft.player).ifPresent(vars -> {
                                        // Forced selection logic
                                        if (mc.sayda.creraces.config.CreRacesConfig.FORCED_SELECTION.get()
                                                        && minecraft.player.isAlive()
                                                        && !vars.hasChosenRace()
                                                        && !mc.sayda.creraces.client.ClientAccess.isWaitingForRaceSelection
                                                        && mc.sayda.creraces.client.ClientAccess.lastSyncedPlayer == minecraft.player) {

                                                boolean isCreScreen = minecraft.screen instanceof mc.sayda.creraces.client.screen.RaceSelectionScreen
                                                                || minecraft.screen instanceof mc.sayda.creraces.client.screen.RaceDetailsScreen
                                                                || minecraft.screen instanceof mc.sayda.creraces.client.screen.SubRaceScreen
                                                                || minecraft.screen instanceof mc.sayda.creraces.client.screen.MenuGUIScreen
                                                                || minecraft.screen instanceof mc.sayda.creraces.client.screen.DebugScreen
                                                                || minecraft.screen instanceof mc.sayda.creraces.client.screen.DynamicMirrorScreen
                                                                || minecraft.screen instanceof net.minecraft.client.gui.screens.PauseScreen
                                                                || minecraft.screen instanceof net.minecraft.client.gui.screens.ConfirmLinkScreen;

                                                if (!isCreScreen) {
                                                        // Direct open selection screen instead of menu for reliability
                                                        mc.sayda.creraces.network.BoundaryHandler.sendOpenMenu();
                                                }
                                        } else {
                                                mc.sayda.creraces.client.ClientAccess.isWaitingForRaceSelection = false;
                                        }
                                });
                        }

                        if (minecraft.player != null && !minecraft.player.isRemoved()) {
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

                                while (ModKeyMappings.ESSENCE_BELT.consumeClick()) {
                                        mc.sayda.creraces.network.BoundaryHandler.sendOpenEssenceBelt();
                                }
                        }

                });
        }

}
