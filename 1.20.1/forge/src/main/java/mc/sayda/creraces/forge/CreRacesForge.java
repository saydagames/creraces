package mc.sayda.creraces.forge;

import dev.architectury.platform.Platform;
import dev.architectury.platform.forge.EventBuses;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.block.EterveilBlock;
import mc.sayda.creraces.block.FairySourceBlock;
import mc.sayda.creraces.client.CreRacesClient;
import mc.sayda.creraces.config.forge.ForgeConfig;
import mc.sayda.creraces.forge.compat.CuriosBeltCompat;
import mc.sayda.creraces.forge.migration.LegacyBlockRemaps;
import mc.sayda.creraces.forge.migration.LegacyMigrationHooks;
import mc.sayda.creraces.item.SpiritCompassItem;
import mc.sayda.creraces.registry.ModFluids;
import mc.sayda.creraces.registry.ModItems;
import mc.sayda.creraces.registry.ModPotions;
import mc.sayda.creraces.util.PlatformServices;
import mc.sayda.creraces.worldgen.VeilwoodBiomeInjector;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Mod(CreRaces.MODID)
public class CreRacesForge {

    /** Populated during mod construction; read by FairyFluidTypeMixin via getFluidType(). */
    public static RegistryObject<FluidType> FAIRY_FLUID_TYPE;
    public static RegistryObject<FluidType> ETERVEIL_FLUID_TYPE;

    /**
     * FluidTypes that should get full vanilla-water semantics (isInWater(), swim splash, fall-reset,
     * fire-clear, see WaterEquivalentFluidMixin), beyond just the #minecraft:water fluid tag. Forge's
     * fluid rewrite hardcodes that behavior to literal identity with ForgeMod.WATER_TYPE, so a modded
     * FluidType has to opt in here explicitly. Fairy Source is deliberately NOT in this list, fairy
     * wings are meant to go soggy in real water, and their own fluid shouldn't trigger that.
     */
    public static final List<RegistryObject<FluidType>> WATER_EQUIVALENT_FLUID_TYPES = new ArrayList<>();

    public CreRacesForge() {
        // Must run before CreRaces.init() below: it registers a PLAYER_JOIN handler that has to
        // resolve a migrated race before IncidentResolver's own PLAYER_JOIN handler syncs state
        // to the client, so registration order here matters.
        LegacyMigrationHooks.init();
        LegacyBlockRemaps.init();
        CreRacesForgeVillagerTrades.init();
        CreRacesForgeVillageStructures.init();

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ForgeConfig.COMMON_SPEC,
                "creraces/creraces-common.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ForgeConfig.CLIENT_SPEC,
                "creraces/creraces-client.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ForgeConfig.ENTITIES_SPEC,
                "creraces/creraces-entities.toml");

        EventBuses.registerModEventBus(CreRaces.MODID, modBus);
        PlatformServices.burnTimeHandler = stack -> ForgeHooks.getBurnTime(stack, null);
        // Curios is not a hard dependency (mods.toml has no entry for it), so only assign the
        // Curios-backed lookup when it is actually loaded. The default beltFinder (Optional.empty())
        // otherwise avoids a NoClassDefFoundError on CuriosApi the moment a player uses the belt.
        if (Platform.isModLoaded("curios")) {
            PlatformServices.beltFinder = CuriosBeltCompat::findBelt;
        }

        registerFluidTypes(modBus);

        // Forge's FillBucketEvent fires before BucketPickup.pickupBlock and uses the fluid
        // capability to fill the bucket, bypassing our pickupBlock override. Cancel it at
        // HIGHEST priority so Forge's default handler never runs for fairy_source/eterveil.
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (FillBucketEvent event) -> {
            if (event.getTarget() instanceof BlockHitResult blockHit) {
                BlockState state = event.getLevel().getBlockState(blockHit.getBlockPos());
                if (state.getBlock() instanceof FairySourceBlock || state.getBlock() instanceof EterveilBlock) {
                    event.setCanceled(true);
                }
            }
        });

        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> {
            // Translucent layer for the fluids themselves, so LiquidBlockRenderer alpha-blends them
            ItemBlockRenderTypes.setRenderLayer(ModFluids.FAIRY_SOURCE.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.FAIRY_SOURCE_FLOWING.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.ETERVEIL.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.ETERVEIL_FLOWING.get(), RenderType.translucent());
            // Spirit Compass needle model predicate
            ItemProperties.register(ModItems.SPIRIT_COMPASS.get(), new ResourceLocation("creraces", "angle"),
                    (stack, level, entity, seed) -> SpiritCompassItem.needleAngle(stack, level, entity));
        }));

        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            VeilwoodBiomeInjector.init();
            if (VeilwoodBiomeInjector.isEnabled() && !Platform.isModLoaded("terrablender")) {
                CreRaces.LOGGER.warn(
                        "[CreRaces] Veilwood Forest requires TerraBlender on Forge. Install TerraBlender or set veilwood_forest_enabled=false in config.");
            }
            BrewingRecipeRegistry.addRecipe(
                    Ingredient.of(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.AWKWARD)),
                    Ingredient.of(ModItems.VEIL_BLOOM_ITEM.get()),
                    PotionUtils.setPotion(new ItemStack(Items.POTION), ModPotions.REVEALING.get()));
        }));

        CreRaces.init();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> CreRacesClient::init);
    }

    /** One FluidType per fluid, shared by that fluid's source and flowing variants. */
    private static void registerFluidTypes(IEventBus modBus) {
        DeferredRegister<FluidType> fluidTypes =
                DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, CreRaces.MODID);

        FluidType.Properties fairyProps = FluidType.Properties.create().density(1000).viscosity(1000).lightLevel(7);
        FAIRY_FLUID_TYPE = fluidTypes.register("fairy_source", () -> new FluidType(fairyProps) {
            @Override
            public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                consumer.accept(new IClientFluidTypeExtensions() {
                    private static final ResourceLocation STILL =
                            new ResourceLocation("creraces", "block/fairy_source_still");
                    private static final ResourceLocation FLOW =
                            new ResourceLocation("creraces", "block/fairy_source_flow");
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
                            new ResourceLocation("creraces", "block/eterveil_still");
                    private static final ResourceLocation FLOW =
                            new ResourceLocation("creraces", "block/eterveil_flow");
                    @Override public ResourceLocation getStillTexture() { return STILL; }
                    @Override public ResourceLocation getFlowingTexture() { return FLOW; }
                });
            }
        });

        WATER_EQUIVALENT_FLUID_TYPES.add(ETERVEIL_FLUID_TYPE);
        fluidTypes.register(modBus);
    }
}
