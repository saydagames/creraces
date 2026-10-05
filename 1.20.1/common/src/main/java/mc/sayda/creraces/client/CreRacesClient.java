package mc.sayda.creraces.client;

import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.platform.Platform;
import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.particle.ParticleProviderRegistry;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.ability.EssenceRegistry;
import mc.sayda.creraces.ability.EssenceType;
import mc.sayda.creraces.block.EssenceCauldronBlock;
import mc.sayda.creraces.block.entity.EssenceCauldronBlockEntity;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.client.model.PoisonEmitterMobileModel;
import mc.sayda.creraces.client.model.PoisonEmitterModel;
import mc.sayda.creraces.client.model.RemainsModel;
import mc.sayda.creraces.client.model.TornadoModel;
import mc.sayda.creraces.client.model.TrollPillarModel;
import mc.sayda.creraces.client.particle.DamageCritParticle;
import mc.sayda.creraces.client.particle.EssenceParticle;
import mc.sayda.creraces.client.particle.MarkerParticle;
import mc.sayda.creraces.client.particle.PoisonEmitterParticle;
import mc.sayda.creraces.client.particle.VeilEmberParticle;
import mc.sayda.creraces.client.particle.VeilMistParticle;
import mc.sayda.creraces.client.render.DryadBoatRenderer;
import mc.sayda.creraces.client.render.ElysianVeilBloomBlockEntityRenderer;
import mc.sayda.creraces.client.render.EssenceVortexRenderer;
import mc.sayda.creraces.client.render.FloatingMoteRenderer;
import mc.sayda.creraces.client.render.MiniBlockEntityRenderer;
import mc.sayda.creraces.client.render.PoisonEmitterMobileRenderer;
import mc.sayda.creraces.client.render.PoisonEmitterRenderer;
import mc.sayda.creraces.client.render.RemainsRenderer;
import mc.sayda.creraces.client.render.SpiritRealmRenderer;
import mc.sayda.creraces.client.render.ToriiBellRenderer;
import mc.sayda.creraces.client.render.TornadoRenderer;
import mc.sayda.creraces.client.render.TrollPillarRenderer;
import mc.sayda.creraces.client.render.VeilMushroomBlockEntityRenderer;
import mc.sayda.creraces.client.render.VeilWillowBoatRenderer;
import mc.sayda.creraces.client.render.VeilWillowSaplingBlockEntityRenderer;
import mc.sayda.creraces.client.render.WaypointRenderer;
import mc.sayda.creraces.client.screen.BadAppleScreen;
import mc.sayda.creraces.client.screen.DebugScreen;
import mc.sayda.creraces.client.screen.DynamicMirrorScreen;
import mc.sayda.creraces.client.screen.EssenceBeltScreen;
import mc.sayda.creraces.client.screen.HUDEditorScreen;
import mc.sayda.creraces.client.screen.MenuGUIScreen;
import mc.sayda.creraces.client.screen.QuestBoardScreen;
import mc.sayda.creraces.client.screen.RaceDetailsScreen;
import mc.sayda.creraces.client.screen.RaceSelectionScreen;
import mc.sayda.creraces.client.screen.ResearchTableScreen;
import mc.sayda.creraces.client.screen.SkillWheelScreen;
import mc.sayda.creraces.client.screen.SubRaceScreen;
import mc.sayda.creraces.client.screen.TerritoryMapScreen;
import mc.sayda.creraces.client.waypoint.WaypointDiscovery;
import mc.sayda.creraces.client.waypoint.WaypointStore;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.item.EssenceBucketItem;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.network.CastAbilityPacket;
import mc.sayda.creraces.registry.ModBlocks;
import mc.sayda.creraces.registry.ModEntities;
import mc.sayda.creraces.registry.ModItems;
import mc.sayda.creraces.registry.ModMenuTypes;
import mc.sayda.creraces.registry.ModParticles;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class CreRacesClient {
    private static final ResourceLocation VEILWOOD_FOREST = new ResourceLocation("creraces", "veilwood_forest");
    // The server drops an invalid race pick without replying, so an unconfirmed pick only holds the
    // forced race menu back for this long.
    private static final int RACE_CONFIRM_TIMEOUT_TICKS = 100;

    private static Player lastPlayerInstance = null;
    private static int raceConfirmWaitTicks = 0;

    public static void init() {
        ModKeyMappings.register();
        BoundaryHandler.registerS2C();
        SpiritMobilityClient.init();

        registerParticles();
        registerVeilwoodEmbers();

        // Forge's menu types are not bound yet during mod construction, so Forge registers these
        // from client setup instead (see runClientSetupRegistrations).
        if (Platform.isFabric()) {
            registerMenuScreens();
        }

        registerEntityRenderers();
        registerClientSetupHandlers();

        ClientGuiEvent.RENDER_HUD.register((graphics, tickDelta) -> {
            // The HUD editor draws its own preview of the overlay
            if (!(Minecraft.getInstance().screen instanceof HUDEditorScreen)) {
                RaceOverlay.render(graphics, tickDelta);
            }
            SpiritRealmRenderer.renderScreenTint(graphics);
            WaypointRenderer.render(graphics);
        });

        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player -> {
            ClientAccess.lastSyncedPlayer = null;
            BoundaryHandler.sendSyncRequest();
            TerritoryMapScreen.clearCache();
            WaypointStore.get().onWorldJoin();
            CreRaces.LOGGER.info("CreRacesClient: Requested initial sync from server.");
        });

        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> {
            ClientAccess.lastSyncedPlayer = null;
            ClientAccess.isWaitingForRaceSelection = false;
            ActionRegistry.cleanup(player);
            SpiritMobilityClient.reset();
        });

        registerTickHandler();

        CreRaces.LOGGER.info("CreRaces Client initialized.");
    }

    private static void registerParticles() {
        ParticleProviderRegistry.register(ModParticles.MARKER, MarkerParticle.Provider::new);
        ParticleProviderRegistry.register(ModParticles.MARKER_MOVE, MarkerParticle.Provider::new);
        ParticleProviderRegistry.register(ModParticles.MARKER_ATTACK, MarkerParticle.Provider::new);
        ParticleProviderRegistry.register(ModParticles.POISON_EMITTER, PoisonEmitterParticle.Provider::new);
        ParticleProviderRegistry.register(ModParticles.MAGIC_DAMAGE, DamageCritParticle.Provider::new);
        ParticleProviderRegistry.register(ModParticles.PHYSICAL_DAMAGE, DamageCritParticle.Provider::new);
        ParticleProviderRegistry.register(ModParticles.TRUE_DAMAGE, DamageCritParticle.Provider::new);
        ParticleProviderRegistry.register(ModParticles.VEIL_EMBER, VeilEmberParticle.Provider::new);
        ParticleProviderRegistry.register(ModParticles.VEIL_MIST, VeilMistParticle.Provider::new);
        ParticleProviderRegistry.register(ModParticles.ESSENCE_PARTICLE, EssenceParticle.Provider::new);
    }

    /**
     * Night-only embers around the player in the Veilwood Forest. Spawned from a client tick
     * rather than the biome's ambient particle settings, which have no way to depend on the time.
     */
    private static void registerVeilwoodEmbers() {
        ClientTickEvent.CLIENT_POST.register(client -> {
            if (client.isPaused()) return;
            ClientLevel level = client.level;
            Player player = client.player;
            if (level == null || player == null) return;
            long timeOfDay = level.getDayTime() % 24000L;
            if (timeOfDay < 13000L || timeOfDay > 23000L) return; // daytime
            if (!level.getBiome(player.blockPosition()).is(VEILWOOD_FOREST)) return;
            int sparks = 2 + level.random.nextInt(2);
            for (int i = 0; i < sparks; i++) {
                level.addParticle(ModParticles.VEIL_EMBER.get(),
                        player.getX() + (level.random.nextDouble() - 0.5) * 20,
                        player.getY() + level.random.nextDouble() * 8,
                        player.getZ() + (level.random.nextDouble() - 0.5) * 20,
                        0, 0, 0);
            }
        });
    }

    private static void registerMenuScreens() {
        MenuRegistry.registerScreenFactory(ModMenuTypes.RESEARCH_TABLE.get(), ResearchTableScreen::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.ESSENCE_BELT.get(), EssenceBeltScreen::new);
        MenuRegistry.registerScreenFactory(ModMenuTypes.QUEST_BOARD.get(), QuestBoardScreen::new);
    }

    private static void registerEntityRenderers() {
        EntityRendererRegistry.register(ModEntities.FEATHER_PROJECTILE, ThrownItemRenderer::new);

        EntityModelLayerRegistry.register(TrollPillarModel.LAYER_LOCATION, TrollPillarModel::createBodyLayer);
        EntityRendererRegistry.register(ModEntities.TROLL_PILLAR, TrollPillarRenderer::new);

        EntityModelLayerRegistry.register(PoisonEmitterModel.LAYER_LOCATION, PoisonEmitterModel::createBodyLayer);
        EntityRendererRegistry.register(ModEntities.POISON_EMITTER, PoisonEmitterRenderer::new);

        EntityModelLayerRegistry.register(PoisonEmitterMobileModel.LAYER_LOCATION,
                PoisonEmitterMobileModel::createBodyLayer);
        EntityRendererRegistry.register(ModEntities.POISON_EMITTER_MOBILE, PoisonEmitterMobileRenderer::new);

        EntityModelLayerRegistry.register(TornadoModel.LAYER_LOCATION, TornadoModel::createBodyLayer);
        EntityRendererRegistry.register(ModEntities.TORNADO, TornadoRenderer::new);

        EntityModelLayerRegistry.register(RemainsModel.LAYER_LOCATION, RemainsModel::createBodyLayer);
        EntityRendererRegistry.register(ModEntities.REMAINS, RemainsRenderer::new);
        EntityRendererRegistry.register(ModEntities.REMAINS_UNDEAD, RemainsRenderer::new);

        EntityRendererRegistry.register(ModEntities.FLOATING_MOTE, FloatingMoteRenderer::new);

        EntityRendererRegistry.register(ModEntities.DRYAD_BOAT, ctx -> new DryadBoatRenderer(ctx, false));
        EntityRendererRegistry.register(ModEntities.DRYAD_CHEST_BOAT, ctx -> new DryadBoatRenderer(ctx, true));
        EntityRendererRegistry.register(ModEntities.VEIL_WILLOW_BOAT, ctx -> new VeilWillowBoatRenderer(ctx, false));
        EntityRendererRegistry.register(ModEntities.VEIL_WILLOW_CHEST_BOAT,
                ctx -> new VeilWillowBoatRenderer(ctx, true));
    }

    private static void registerClientSetupHandlers() {
        // Fabric can run this immediately: architectury-fabric's own client entrypoint may fire
        // CLIENT_SETUP before ours gets a chance to register a listener (Fabric doesn't guarantee
        // entrypoint order between mods), so waiting on the event risks silently missing it entirely.
        // Forge and NeoForge still need the deferred event, since their registry entries are not
        // bound yet during mod construction.
        if (Platform.isFabric()) {
            runClientSetupRegistrations();
        } else {
            ClientLifecycleEvent.CLIENT_SETUP.register(instance -> runClientSetupRegistrations());
        }
    }

    private static void runClientSetupRegistrations() {
        if (Platform.isForge()) {
            registerMenuScreens();
        }
        registerBlockEntityRenderers();
        registerColorHandlers();
        registerRenderTypes();
    }

    private static void registerBlockEntityRenderers() {
        BlockEntityRendererRegistry.register(ModBlocks.MICRO_BLOCK_ENTITY.get(), MiniBlockEntityRenderer::new);
        BlockEntityRendererRegistry.register(ModBlocks.TORII_BELL_ENTITY.get(), ToriiBellRenderer::new);
        BlockEntityRendererRegistry.register(ModBlocks.VEIL_MUSHROOM_BE.get(), VeilMushroomBlockEntityRenderer::new);
        BlockEntityRendererRegistry.register(ModBlocks.VEIL_BLOOM_BE.get(), ElysianVeilBloomBlockEntityRenderer::new);
        BlockEntityRendererRegistry.register(ModBlocks.ESSENCE_VORTEX_ENTITY.get(), EssenceVortexRenderer::new);
        BlockEntityRendererRegistry.register(ModBlocks.VEIL_WILLOW_SAPLING_BE.get(),
                VeilWillowSaplingBlockEntityRenderer::new);
    }

    private static void registerColorHandlers() {
        ColorHandlerRegistry.registerBlockColors(
                (state, world, pos, tintIndex) -> 0xFF00FFFF,
                ModBlocks.VEIL_WILLOW_LEAVES.get(),
                ModBlocks.VEIL_WILLOW_DRAPE.get());
        ColorHandlerRegistry.registerItemColors(
                (stack, tintIndex) -> 0xFF00FFFF,
                ModBlocks.VEIL_WILLOW_LEAVES.get(),
                ModBlocks.VEIL_WILLOW_DRAPE.get());

        // Essence cauldron: the liquid face (tintindex 0) takes the stored essence type's colour
        ColorHandlerRegistry.registerBlockColors(
                (state, world, pos, tintIndex) -> {
                    if (tintIndex != 0 || world == null || pos == null) return -1;
                    // Gate on the block state rather than the block entity alone: the state is
                    // always current at render time, while block entity data can lag its packet.
                    if (state.getValue(EssenceCauldronBlock.HAS_ESSENCE)
                            && world.getBlockEntity(pos) instanceof EssenceCauldronBlockEntity be
                            && be.getEssenceType() != null) {
                        return 0xFF000000 | be.getEssenceType().getColor();
                    }
                    return 0xFF3F76E4; // vanilla water blue
                },
                ModBlocks.ESSENCE_CAULDRON.get());
        ColorHandlerRegistry.registerItemColors((stack, tintIndex) -> -1, ModBlocks.ESSENCE_CAULDRON.get());

        // Essence bucket: the layer1 overlay takes the stored essence type's colour
        ColorHandlerRegistry.registerItemColors(
                (stack, tintIndex) -> {
                    if (tintIndex != 1) return -1;
                    EssenceType type = EssenceBucketItem.getEssenceType(stack);
                    return type != null ? (0xFF000000 | type.getColor()) : -1;
                },
                ModItems.ESSENCE_BUCKET.get());

        // Shards, bottles, clusters and vortexes share one texture per shape, tinted per essence type
        for (EssenceType type : EssenceType.values()) {
            // EssenceType stores plain RGB. 1.21 honours the alpha byte of tint colours (1.20.1
            // ignores it), so force it opaque or these would render fully transparent there.
            int color = 0xFF000000 | type.getColor();
            ColorHandlerRegistry.registerItemColors(
                    (stack, tintIndex) -> tintIndex == 0 ? color : -1,
                    EssenceRegistry.SHARDS.get(type).get(),
                    EssenceRegistry.BOTTLES.get(type).get(),
                    EssenceRegistry.CLUSTER_ITEMS.get(type).get(),
                    EssenceRegistry.VORTEX_ITEMS.get(type).get());
            ColorHandlerRegistry.registerBlockColors(
                    (state, world, pos, tintIndex) -> tintIndex == 0 ? color : -1,
                    EssenceRegistry.CLUSTERS.get(type).get(),
                    EssenceRegistry.VORTEXES.get(type).get());
        }
    }

    private static void registerRenderTypes() {
        RenderTypeRegistry.register(RenderType.cutout(),
                ModBlocks.TORII_BELL.get(),
                ModBlocks.WEATHERED_TORII_BELL.get(),
                ModBlocks.DRYAD_LEAVES.get(),
                ModBlocks.DRYAD_LEAVES_FLOWERING.get(),
                ModBlocks.DRYAD_LEAVES_FRUIT.get(),
                ModBlocks.DRYAD_LANTERN.get(),
                ModBlocks.RAT_HOLE.get(),
                ModBlocks.VEIL_BLOOM.get(),
                ModBlocks.VEIL_WILLOW_LEAVES.get(),
                ModBlocks.VEIL_WILLOW_DRAPE.get());

        // Clusters are cross-shaped with transparent pixels; vortexes are translucent so their
        // item alpha is respected
        for (EssenceType type : EssenceType.values()) {
            RenderTypeRegistry.register(RenderType.cutout(), EssenceRegistry.CLUSTERS.get(type).get());
            RenderTypeRegistry.register(RenderType.translucent(), EssenceRegistry.VORTEXES.get(type).get());
        }

        RenderTypeRegistry.register(RenderType.cutout(),
                ModBlocks.DRYAD_SAPLING.get(),
                ModBlocks.VEIL_WILLOW_SAPLING.get(),
                ModBlocks.VEIL_MUSHROOM.get());
        RenderTypeRegistry.register(RenderType.translucent(), ModBlocks.MICRO_BLOCK.get());

        // Fairy source liquid block, translucent like water
        RenderTypeRegistry.register(RenderType.translucent(), ModBlocks.FAIRY_SOURCE_BLOCK.get());
    }

    private static void registerTickHandler() {
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            LocalPlayer player = minecraft.player;
            if (player == null || player.isRemoved()) {
                return;
            }
            enforceRaceSelection(minecraft, player);
            WaypointStore.get().tick(player);
            WaypointDiscovery.tick(player);
            handleKeyPresses(minecraft);
        });
    }

    /** With forced selection on, keeps putting the race menu back up until a race is chosen. */
    private static void enforceRaceSelection(Minecraft minecraft, LocalPlayer player) {
        // A new player instance means a respawn or rejoin, so any earlier wait no longer applies
        if (player != lastPlayerInstance) {
            lastPlayerInstance = player;
            ClientAccess.isWaitingForRaceSelection = false;
        }

        // A picked race is on its way to the server. Until the sync confirming it clears the flag
        // (ClientAccess.handleSyncIncident), a sync sent before the pick must not reopen the menu.
        if (ClientAccess.isWaitingForRaceSelection && ++raceConfirmWaitTicks < RACE_CONFIRM_TIMEOUT_TICKS) {
            return;
        }
        ClientAccess.isWaitingForRaceSelection = false;
        raceConfirmWaitTicks = 0;

        DataUtils.getVariables(player).ifPresent(vars -> {
            boolean mustChoose = CreRacesConfig.FORCED_SELECTION.get()
                    && player.isAlive()
                    && !vars.hasChosenRace()
                    && ClientAccess.lastSyncedPlayer == player;
            if (mustChoose && !isExemptFromForcedSelection(minecraft.screen)) {
                minecraft.setScreen(new MenuGUIScreen());
            }
        });
    }

    /** Screens that forced selection leaves open rather than replacing with the race menu. */
    private static boolean isExemptFromForcedSelection(Screen screen) {
        return screen instanceof RaceSelectionScreen
                || screen instanceof RaceDetailsScreen
                || screen instanceof SubRaceScreen
                || screen instanceof MenuGUIScreen
                || screen instanceof DebugScreen
                || screen instanceof DynamicMirrorScreen
                || screen instanceof BadAppleScreen
                || screen instanceof PauseScreen
                || screen instanceof ConfirmLinkScreen;
    }

    private static void handleKeyPresses(Minecraft minecraft) {
        while (ModKeyMappings.WAYPOINT_TOGGLE.consumeClick()) {
            WaypointStore store = WaypointStore.get();
            store.setMarkersEnabled(!store.isMarkersEnabled());
        }
        while (ModKeyMappings.SKILL_WHEEL.consumeClick()) {
            minecraft.setScreen(new SkillWheelScreen());
        }
        castOnPress(ModKeyMappings.ABILITY_A1, AbilitySlot.A1);
        castOnPress(ModKeyMappings.ABILITY_A2, AbilitySlot.A2);
        castOnPress(ModKeyMappings.ABILITY_A3, AbilitySlot.A3);
        castOnPress(ModKeyMappings.ABILITY_A4, AbilitySlot.A4);
        castOnPress(ModKeyMappings.ABILITY_A5, AbilitySlot.A5);
        while (ModKeyMappings.MENU_GUI.consumeClick()) {
            minecraft.setScreen(new MenuGUIScreen());
        }
        while (ModKeyMappings.ESSENCE_BELT.consumeClick()) {
            BoundaryHandler.sendOpenEssenceBelt();
        }
    }

    private static void castOnPress(KeyMapping key, AbilitySlot slot) {
        while (key.consumeClick()) {
            BoundaryHandler.sendCastAbility(new CastAbilityPacket(slot));
        }
    }
}
