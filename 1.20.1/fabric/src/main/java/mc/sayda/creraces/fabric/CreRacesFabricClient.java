package mc.sayda.creraces.fabric;

import mc.sayda.creraces.client.CreRacesClient;
import mc.sayda.creraces.item.SpiritCompassItem;
import mc.sayda.creraces.registry.ModFluids;
import mc.sayda.creraces.registry.ModItems;
import mc.sayda.creraces.worldgen.VeilwoodBiomeInjector;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;

public class CreRacesFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Safe to call here: Fabric runs every "main" entrypoint (including TerraBlender's, which
        // loads TerraBlender.CONFIG) to completion before any "client" entrypoint starts.
        VeilwoodBiomeInjector.init();

        FluidRenderHandlerRegistry.INSTANCE.register(
                ModFluids.FAIRY_SOURCE.get(),
                ModFluids.FAIRY_SOURCE_FLOWING.get(),
                new SimpleFluidRenderHandler(
                        new ResourceLocation("creraces", "block/fairy_source_still"),
                        new ResourceLocation("creraces", "block/fairy_source_flow"),
                        new ResourceLocation("creraces", "block/fairy_source_overlay")));

        FluidRenderHandlerRegistry.INSTANCE.register(
                ModFluids.ETERVEIL.get(),
                ModFluids.ETERVEIL_FLOWING.get(),
                new SimpleFluidRenderHandler(
                        new ResourceLocation("creraces", "block/eterveil_still"),
                        new ResourceLocation("creraces", "block/eterveil_flow")));

        // The fluids need their own translucent layer, separate from their blocks, so the in-world
        // liquid alpha-blends the way water does.
        BlockRenderLayerMap.INSTANCE.putFluids(
                RenderType.translucent(),
                ModFluids.FAIRY_SOURCE.get(),
                ModFluids.FAIRY_SOURCE_FLOWING.get(),
                ModFluids.ETERVEIL.get(),
                ModFluids.ETERVEIL_FLOWING.get());

        CreRacesClient.init();

        // Spirit Compass needle model predicate
        ItemProperties.register(
                ModItems.SPIRIT_COMPASS.get(),
                new ResourceLocation("creraces", "angle"),
                (stack, level, entity, seed) -> SpiritCompassItem.needleAngle(stack, level, entity));
    }
}
