package mc.sayda.creraces.neoforge;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.client.CreRacesClient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(CreRaces.MODID)
public class CreRacesNeoForge {

    /** Populated during mod construction; read by FairyFluidTypeMixin via getFluidType(). */
    public static net.neoforged.neoforge.registries.DeferredHolder<net.neoforged.neoforge.fluids.FluidType, net.neoforged.neoforge.fluids.FluidType> FAIRY_FLUID_TYPE;
    public static net.neoforged.neoforge.registries.DeferredHolder<net.neoforged.neoforge.fluids.FluidType, net.neoforged.neoforge.fluids.FluidType> ETERVEIL_FLUID_TYPE;

    /**
     * FluidTypes that should get full vanilla-water semantics (isInWater(), swim splash, fall-reset,
     * fire-clear, see WaterEquivalentFluidMixin), beyond just the #minecraft:water fluid tag. The
     * fluid rewrite hardcodes that behavior to literal identity with NeoForgeMod.WATER_TYPE, so a
     * modded FluidType has to opt in here explicitly. Fairy Source is deliberately NOT in this list,
     * fairy wings are meant to go soggy in real water, and their own fluid shouldn't trigger that.
     */
    public static final java.util.List<net.neoforged.neoforge.registries.DeferredHolder<net.neoforged.neoforge.fluids.FluidType, net.neoforged.neoforge.fluids.FluidType>> WATER_EQUIVALENT_FLUID_TYPES = new java.util.ArrayList<>();

    public CreRacesNeoForge(IEventBus modBus, net.neoforged.fml.ModContainer container) {
        // Must run before CreRaces.init() below: it registers a PLAYER_JOIN handler that has to
        // resolve a migrated race before IncidentResolver's own PLAYER_JOIN handler syncs state
        // to the client, so registration order here matters.
        mc.sayda.creraces.neoforge.migration.LegacyMigrationHooks.init();
        mc.sayda.creraces.neoforge.migration.LegacyBlockRemaps.init();
        CreRacesNeoForgeVillagerTrades.init();
        CreRacesNeoForgeVillageStructures.init();

        mc.sayda.creraces.util.PlatformServices.burnTimeHandler = stack -> stack.getBurnTime(null);
        // Curios is not a hard dependency (neoforge.mods.toml has no entry for it), so only assign the
        // Curios-backed lookup when it is actually loaded. The default beltFinder (Optional.empty())
        // otherwise avoids a NoClassDefFoundError on CuriosApi the moment a player uses the belt.
        if (dev.architectury.platform.Platform.isModLoaded("curios")) {
                mc.sayda.creraces.util.PlatformServices.beltFinder = mc.sayda.creraces.neoforge.compat.CuriosBeltCompat::findBelt;
        }

        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON,
                mc.sayda.creraces.config.neoforge.NeoForgeConfig.COMMON_SPEC,
                "creraces/creraces-common.toml");
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT,
                mc.sayda.creraces.config.neoforge.NeoForgeConfig.CLIENT_SPEC,
                "creraces/creraces-client.toml");
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON,
                mc.sayda.creraces.config.neoforge.NeoForgeConfig.ENTITIES_SPEC,
                "creraces/creraces-entities.toml");

        registerFluidTypes(modBus);

        // Brewing registration is a game-bus event, not a mod-bus one.
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent event) ->
                event.getBuilder().addMix(
                        net.minecraft.world.item.alchemy.Potions.AWKWARD,
                        mc.sayda.creraces.registry.ModItems.VEIL_BLOOM_ITEM.get(),
                        mc.sayda.creraces.registry.ModPotions.REVEALING));

        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) ->
                event.enqueueWork(() -> {
                    mc.sayda.creraces.worldgen.VeilwoodBiomeInjector.init();
                    if (mc.sayda.creraces.worldgen.VeilwoodBiomeInjector.isEnabled()
                            && !dev.architectury.platform.Platform.isModLoaded("terrablender")) {
                        CreRaces.LOGGER.warn(
                                "[CreRaces] Veilwood Forest requires TerraBlender on NeoForge. Install TerraBlender or set veilwood_forest_enabled=false in config.");
                    }
                }));

        CreRaces.init();
        dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> CreRacesClient::init);
    }

    /** One FluidType per fluid, shared by that fluid's source and flowing variants. */
    private static void registerFluidTypes(IEventBus modBus) {
        net.neoforged.neoforge.registries.DeferredRegister<net.neoforged.neoforge.fluids.FluidType> fluidTypes =
                net.neoforged.neoforge.registries.DeferredRegister.create(
                        net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.FLUID_TYPES, CreRaces.MODID);

        net.neoforged.neoforge.fluids.FluidType.Properties fairyProps = net.neoforged.neoforge.fluids.FluidType.Properties
                .create().density(1000).viscosity(1000).lightLevel(7);
        FAIRY_FLUID_TYPE = fluidTypes.register("fairy_source", () -> new net.neoforged.neoforge.fluids.FluidType(fairyProps) {
            @Override
            public void initializeClient(java.util.function.Consumer<net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions> consumer) {
                consumer.accept(new net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions() {
                    private static final net.minecraft.resources.ResourceLocation STILL =
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("creraces", "block/fairy_source_still");
                    private static final net.minecraft.resources.ResourceLocation FLOW =
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("creraces", "block/fairy_source_flow");
                    @Override public net.minecraft.resources.ResourceLocation getStillTexture() { return STILL; }
                    @Override public net.minecraft.resources.ResourceLocation getFlowingTexture() { return FLOW; }
                    /** 0xAA = ~67% opacity; preserves texture colours, adds water-like transparency. */
                    @Override public int getTintColor() { return 0xAAFFFFFF; }
                });
            }
        });

        net.neoforged.neoforge.fluids.FluidType.Properties eterveilProps = net.neoforged.neoforge.fluids.FluidType.Properties
                .create().density(1000).viscosity(1000).lightLevel(1);
        ETERVEIL_FLUID_TYPE = fluidTypes.register("eterveil", () -> new net.neoforged.neoforge.fluids.FluidType(eterveilProps) {
            @Override
            public void initializeClient(java.util.function.Consumer<net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions> consumer) {
                consumer.accept(new net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions() {
                    private static final net.minecraft.resources.ResourceLocation STILL =
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("creraces", "block/eterveil_still");
                    private static final net.minecraft.resources.ResourceLocation FLOW =
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("creraces", "block/eterveil_flow");
                    @Override public net.minecraft.resources.ResourceLocation getStillTexture() { return STILL; }
                    @Override public net.minecraft.resources.ResourceLocation getFlowingTexture() { return FLOW; }
                });
            }
        });

        WATER_EQUIVALENT_FLUID_TYPES.add(ETERVEIL_FLUID_TYPE);
        fluidTypes.register(modBus);
    }
}
