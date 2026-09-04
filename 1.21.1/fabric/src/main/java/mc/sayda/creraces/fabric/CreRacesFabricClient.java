package mc.sayda.creraces.fabric;

import mc.sayda.creraces.client.CreRacesClient;
import mc.sayda.creraces.registry.ModFluids;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.minecraft.resources.ResourceLocation;

public class CreRacesFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Safe to call here: Fabric runs every "main" entrypoint (including TerraBlender's, which
        // loads TerraBlender.CONFIG) to completion before any "client" entrypoint starts.
        mc.sayda.creraces.worldgen.VeilwoodBiomeInjector.init();

        FluidRenderHandlerRegistry.INSTANCE.register(
                ModFluids.FAIRY_SOURCE.get(),
                ModFluids.FAIRY_SOURCE_FLOWING.get(),
                new SimpleFluidRenderHandler(
                        ResourceLocation.fromNamespaceAndPath("creraces", "block/fairy_source_still"),
                        ResourceLocation.fromNamespaceAndPath("creraces", "block/fairy_source_flow"),
                        ResourceLocation.fromNamespaceAndPath("creraces", "block/fairy_source_overlay")));

        FluidRenderHandlerRegistry.INSTANCE.register(
                ModFluids.ETERVEIL.get(),
                ModFluids.ETERVEIL_FLOWING.get(),
                new SimpleFluidRenderHandler(
                        ResourceLocation.fromNamespaceAndPath("creraces", "block/eterveil_still"),
                        ResourceLocation.fromNamespaceAndPath("creraces", "block/eterveil_flow")));

        // Block render layers are set in CreRacesClient via Architectury's RenderTypeRegistry, but
        // that only covers Blocks. The Fluid itself still needs its own translucent layer so the
        // in-world liquid alpha-blends the way water does.
        net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putFluids(
                net.minecraft.client.renderer.RenderType.translucent(),
                ModFluids.FAIRY_SOURCE.get(),
                ModFluids.FAIRY_SOURCE_FLOWING.get());

        CreRacesClient.init();
    }
}
