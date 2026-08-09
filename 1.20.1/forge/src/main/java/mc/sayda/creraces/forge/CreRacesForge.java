package mc.sayda.creraces.forge;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.client.CreRacesClient;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CreRaces.MODID)
public class CreRacesForge {

    /** Populated during mod construction; read by FairyFluidTypeMixin via getFluidType(). */
    public static net.minecraftforge.registries.RegistryObject<net.minecraftforge.fluids.FluidType> FAIRY_FLUID_TYPE;

    public CreRacesForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.COMMON,
                mc.sayda.creraces.config.forge.ForgeConfig.COMMON_SPEC,
                "creraces/creraces-common.toml");
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.CLIENT,
                mc.sayda.creraces.config.forge.ForgeConfig.CLIENT_SPEC,
                "creraces/creraces-client.toml");
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.COMMON,
                mc.sayda.creraces.config.forge.ForgeConfig.ENTITIES_SPEC,
                "creraces/creraces-entities.toml");

        dev.architectury.platform.forge.EventBuses.registerModEventBus(CreRaces.MODID, modBus);
        mc.sayda.creraces.util.PlatformServices.burnTimeHandler = stack -> net.minecraftforge.common.ForgeHooks
                .getBurnTime(stack, null);

        // Register the single Forge FluidType shared by both source and flowing variants.
        net.minecraftforge.registries.DeferredRegister<net.minecraftforge.fluids.FluidType> fluidTypes =
                net.minecraftforge.registries.DeferredRegister.create(
                        net.minecraftforge.registries.ForgeRegistries.Keys.FLUID_TYPES, CreRaces.MODID);
        net.minecraftforge.fluids.FluidType.Properties fairyProps = net.minecraftforge.fluids.FluidType.Properties
                .create().density(1000).viscosity(1000).lightLevel(7);
        FAIRY_FLUID_TYPE = fluidTypes.register("fairy_source", () -> new net.minecraftforge.fluids.FluidType(fairyProps) {
            @Override
            public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions> consumer) {
                consumer.accept(new net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions() {
                    private static final net.minecraft.resources.ResourceLocation STILL =
                            new net.minecraft.resources.ResourceLocation("creraces", "block/fairy_source_still");
                    private static final net.minecraft.resources.ResourceLocation FLOW =
                            new net.minecraft.resources.ResourceLocation("creraces", "block/fairy_source_flow");
                    @Override public net.minecraft.resources.ResourceLocation getStillTexture() { return STILL; }
                    @Override public net.minecraft.resources.ResourceLocation getFlowingTexture() { return FLOW; }
                    /** 0xAA = ~67% opacity; preserves texture colours, adds water-like transparency. */
                    @Override public int getTintColor() { return 0xAAFFFFFF; }
                });
            }
        });
        fluidTypes.register(modBus);

        // Forge's FillBucketEvent fires before BucketPickup.pickupBlock and uses the fluid
        // capability to fill the bucket, bypassing our pickupBlock override. Cancel it at
        // HIGHEST priority so Forge's default handler never runs for fairy_source.
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                net.minecraftforge.eventbus.api.EventPriority.HIGHEST,
                (net.minecraftforge.event.entity.player.FillBucketEvent event) -> {
                    if (event.getTarget() instanceof net.minecraft.world.phys.BlockHitResult blockHit) {
                        net.minecraft.world.level.block.state.BlockState state =
                                event.getLevel().getBlockState(blockHit.getBlockPos());
                        if (state.getBlock() instanceof mc.sayda.creraces.block.FairySourceBlock) {
                            event.setCanceled(true);
                        }
                    }
                });

        // Register translucent render layer for the fluid so LiquidBlockRenderer uses alpha blending.
        modBus.addListener((net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) ->
            event.enqueueWork(() -> {
                net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                        mc.sayda.creraces.registry.ModFluids.FAIRY_SOURCE.get(),
                        net.minecraft.client.renderer.RenderType.translucent());
                net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                        mc.sayda.creraces.registry.ModFluids.FAIRY_SOURCE_FLOWING.get(),
                        net.minecraft.client.renderer.RenderType.translucent());
                net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                        mc.sayda.creraces.registry.ModBlocks.DRYAD_SAPLING.get(),
                        net.minecraft.client.renderer.RenderType.cutout());
            }));

        CreRaces.init();
        // Register Forge-specific player data events for authoritative NBT load/save
        // MinecraftForge.EVENT_BUS.register(this);
        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> CreRacesClient::init);
    }
}
