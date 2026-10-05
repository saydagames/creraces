package mc.sayda.creraces.neoforge;

import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.client.CreRacesClient;
import mc.sayda.creraces.config.neoforge.NeoForgeConfig;
import mc.sayda.creraces.neoforge.compat.CuriosBeltCompat;
import mc.sayda.creraces.neoforge.migration.LegacyBlockRemaps;
import mc.sayda.creraces.neoforge.migration.LegacyMigrationHooks;
import mc.sayda.creraces.registry.ModItems;
import mc.sayda.creraces.registry.ModPotions;
import mc.sayda.creraces.util.PlatformServices;
import mc.sayda.creraces.worldgen.VeilwoodBiomeInjector;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Mod(CreRaces.MODID)
public class CreRacesNeoForge {

    /** Populated during mod construction; read by FairyFluidTypeMixin via getFluidType(). */
    public static DeferredHolder<FluidType, FluidType> FAIRY_FLUID_TYPE;
    public static DeferredHolder<FluidType, FluidType> ETERVEIL_FLUID_TYPE;

    /**
     * FluidTypes that should get full vanilla-water semantics (isInWater(), swim splash, fall-reset,
     * fire-clear, see WaterEquivalentFluidMixin), beyond just the #minecraft:water fluid tag. The
     * fluid rewrite hardcodes that behavior to literal identity with NeoForgeMod.WATER_TYPE, so a
     * modded FluidType has to opt in here explicitly. Fairy Source is deliberately NOT in this list,
     * fairy wings are meant to go soggy in real water, and their own fluid shouldn't trigger that.
     */
    public static final List<DeferredHolder<FluidType, FluidType>> WATER_EQUIVALENT_FLUID_TYPES = new ArrayList<>();

    public CreRacesNeoForge(IEventBus modBus, ModContainer container) {
        // Must run before CreRaces.init() below: it registers a PLAYER_JOIN handler that has to
        // resolve a migrated race before IncidentResolver's own PLAYER_JOIN handler syncs state
        // to the client, so registration order here matters.
        LegacyMigrationHooks.init();
        LegacyBlockRemaps.init();
        CreRacesNeoForgeVillagerTrades.init();
        CreRacesNeoForgeVillageStructures.init();

        PlatformServices.burnTimeHandler = stack -> stack.getBurnTime(null);
        // Curios is not a hard dependency (neoforge.mods.toml has no entry for it), so only assign the
        // Curios-backed lookup when it is actually loaded. The default beltFinder (Optional.empty())
        // otherwise avoids a NoClassDefFoundError on CuriosApi the moment a player uses the belt.
        if (Platform.isModLoaded("curios")) {
            PlatformServices.beltFinder = CuriosBeltCompat::findBelt;
        }

        container.registerConfig(ModConfig.Type.COMMON, NeoForgeConfig.COMMON_SPEC,
                "creraces/creraces-common.toml");
        container.registerConfig(ModConfig.Type.CLIENT, NeoForgeConfig.CLIENT_SPEC,
                "creraces/creraces-client.toml");
        container.registerConfig(ModConfig.Type.COMMON, NeoForgeConfig.ENTITIES_SPEC,
                "creraces/creraces-entities.toml");

        registerFluidTypes(modBus);

        // Brewing registration is a game-bus event, not a mod-bus one. The potion goes in as the
        // registry's own holder: a RegistrySupplier is not a Holder.Reference, and a potion stack
        // built from one fails to save ("Unregistered holder").
        NeoForge.EVENT_BUS.addListener((RegisterBrewingRecipesEvent event) ->
                event.getBuilder().addMix(Potions.AWKWARD, ModItems.VEIL_BLOOM_ITEM.get(),
                        BuiltInRegistries.POTION.wrapAsHolder(ModPotions.REVEALING.get())));

        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            VeilwoodBiomeInjector.init();
            if (VeilwoodBiomeInjector.isEnabled() && !Platform.isModLoaded("terrablender")) {
                CreRaces.LOGGER.warn(
                        "[CreRaces] Veilwood Forest requires TerraBlender on NeoForge. Install TerraBlender or set veilwood_forest_enabled=false in config.");
            }
        }));

        CreRaces.init();
        EnvExecutor.runInEnv(Env.CLIENT, () -> CreRacesClient::init);
    }

    /** One FluidType per fluid, shared by that fluid's source and flowing variants. */
    private static void registerFluidTypes(IEventBus modBus) {
        DeferredRegister<FluidType> fluidTypes =
                DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, CreRaces.MODID);

        FluidType.Properties fairyProps = FluidType.Properties.create().density(1000).viscosity(1000).lightLevel(7);
        FAIRY_FLUID_TYPE = fluidTypes.register("fairy_source", () -> new FluidType(fairyProps) {
            @Override
            public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                consumer.accept(new IClientFluidTypeExtensions() {
                    private static final ResourceLocation STILL =
                            ResourceLocation.fromNamespaceAndPath("creraces", "block/fairy_source_still");
                    private static final ResourceLocation FLOW =
                            ResourceLocation.fromNamespaceAndPath("creraces", "block/fairy_source_flow");
                    @Override public ResourceLocation getStillTexture() { return STILL; }
                    @Override public ResourceLocation getFlowingTexture() { return FLOW; }
                    /** 0xAA = ~67% opacity; preserves texture colours, adds water-like transparency. */
                    @Override public int getTintColor() { return 0xAAFFFFFF; }
                });
            }
        });

        FluidType.Properties eterveilProps = FluidType.Properties.create().density(1000).viscosity(1000).lightLevel(1);
        ETERVEIL_FLUID_TYPE = fluidTypes.register("eterveil", () -> new FluidType(eterveilProps) {
            @Override
            public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                consumer.accept(new IClientFluidTypeExtensions() {
                    private static final ResourceLocation STILL =
                            ResourceLocation.fromNamespaceAndPath("creraces", "block/eterveil_still");
                    private static final ResourceLocation FLOW =
                            ResourceLocation.fromNamespaceAndPath("creraces", "block/eterveil_flow");
                    @Override public ResourceLocation getStillTexture() { return STILL; }
                    @Override public ResourceLocation getFlowingTexture() { return FLOW; }
                });
            }
        });

        WATER_EQUIVALENT_FLUID_TYPES.add(ETERVEIL_FLUID_TYPE);
        fluidTypes.register(modBus);
    }
}
