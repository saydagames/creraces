package mc.sayda.creraces.fabric;

import mc.sayda.creraces.client.CreRacesClient;
import mc.sayda.creraces.registry.ModFluids;
import mc.sayda.creraces.worldgen.VeilwoodBiomeInjector;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
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
                        ResourceLocation.fromNamespaceAndPath("creraces", "block/fairy_source_still"),
                        ResourceLocation.fromNamespaceAndPath("creraces", "block/fairy_source_flow"),
                        ResourceLocation.fromNamespaceAndPath("creraces", "block/fairy_source_overlay")));

        FluidRenderHandlerRegistry.INSTANCE.register(
                ModFluids.ETERVEIL.get(),
                ModFluids.ETERVEIL_FLOWING.get(),
                new SimpleFluidRenderHandler(
                        ResourceLocation.fromNamespaceAndPath("creraces", "block/eterveil_still"),
                        ResourceLocation.fromNamespaceAndPath("creraces", "block/eterveil_flow")));

        // The fluids' translucent layer and the Spirit Compass predicate are registered in CreRacesClient
        CreRacesClient.init();
    }
}
