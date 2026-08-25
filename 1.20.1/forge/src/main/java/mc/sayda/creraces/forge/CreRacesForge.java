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
        // Curios isn't a hard dependency (mods.toml has no [[dependencies]] entry for it), so it can
        // be absent. Only assign the Curios-backed lookup when it's actually loaded; the default
        // beltFinder (Optional.empty()) otherwise avoids a NoClassDefFoundError on CuriosApi the
        // moment a player tries to use the belt.
        if (dev.architectury.platform.Platform.isModLoaded("curios")) {
                mc.sayda.creraces.util.PlatformServices.beltFinder = mc.sayda.creraces.forge.compat.CuriosBeltCompat::findBelt;
        }

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

        modBus.addListener((net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) ->
            event.enqueueWork(() -> {
                // Register translucent render layer for the fluid so LiquidBlockRenderer uses alpha blending.
                net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                        mc.sayda.creraces.registry.ModFluids.FAIRY_SOURCE.get(),
                        net.minecraft.client.renderer.RenderType.translucent());
                net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                        mc.sayda.creraces.registry.ModFluids.FAIRY_SOURCE_FLOWING.get(),
                        net.minecraft.client.renderer.RenderType.translucent());
                // Saplings have transparent pixels and must use cutout so they don't render black.
                net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                        mc.sayda.creraces.registry.ModBlocks.DRYAD_SAPLING.get(),
                        net.minecraft.client.renderer.RenderType.cutout());
                net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                        mc.sayda.creraces.registry.ModBlocks.VEIL_WILLOW_SAPLING.get(),
                        net.minecraft.client.renderer.RenderType.cutout());
                // Spirit Compass angle model predicate
                net.minecraft.client.renderer.item.ItemProperties.register(
                        mc.sayda.creraces.registry.ModItems.SPIRIT_COMPASS.get(),
                        new net.minecraft.resources.ResourceLocation("creraces", "angle"),
                        (stack, level, entity, seed) -> {
                            if (entity == null) return 0f;
                            net.minecraft.nbt.CompoundTag tag = stack.getTag();
                            if (tag == null || !tag.getBoolean("HasTarget")) {
                                return level != null ? (float) ((level.getGameTime() % 32) / 32.0) : 0f;
                            }
                            double dx = tag.getInt("TargetX") - entity.getX();
                            double dz = tag.getInt("TargetZ") - entity.getZ();
                            double worldAngle = Math.toDegrees(Math.atan2(dx, dz));
                            double relative = ((worldAngle - entity.getYRot()) % 360 + 360) % 360;
                            return (float) (relative / 360.0);
                        });
            }));

        modBus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) ->
                event.enqueueWork(() -> {
                    mc.sayda.creraces.worldgen.VeilwoodBiomeInjector.init();
                    if (mc.sayda.creraces.worldgen.VeilwoodBiomeInjector.isEnabled()
                            && !dev.architectury.platform.Platform.isModLoaded("terrablender")) {
                        mc.sayda.creraces.CreRaces.LOGGER.warn(
                                "[CreRaces] Veilwood Forest requires TerraBlender on Forge. Install TerraBlender or set veilwood_forest_enabled=false in config.");
                    }
                    net.minecraftforge.common.brewing.BrewingRecipeRegistry.addRecipe(
                        net.minecraft.world.item.crafting.Ingredient.of(
                            net.minecraft.world.item.alchemy.PotionUtils.setPotion(
                                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.POTION),
                                net.minecraft.world.item.alchemy.Potions.AWKWARD)),
                        net.minecraft.world.item.crafting.Ingredient.of(
                            mc.sayda.creraces.registry.ModItems.VEIL_BLOOM_ITEM.get()),
                        net.minecraft.world.item.alchemy.PotionUtils.setPotion(
                            new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.POTION),
                            mc.sayda.creraces.registry.ModPotions.REVEALING.get()));
                }));

        CreRaces.init();
        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> CreRacesClient::init);
    }
}
