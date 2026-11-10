package mc.sayda.creraces.fabric;

import mc.sayda.creraces.client.CreRacesClient;
import mc.sayda.creraces.client.particle.DamageCritParticle;
import mc.sayda.creraces.client.particle.EssenceParticle;
import mc.sayda.creraces.client.particle.MarkerParticle;
import mc.sayda.creraces.client.particle.PoisonEmitterParticle;
import mc.sayda.creraces.client.particle.VeilEmberParticle;
import mc.sayda.creraces.client.particle.VeilMistParticle;
import mc.sayda.creraces.registry.ModFluids;
import mc.sayda.creraces.registry.ModParticles;
import mc.sayda.creraces.worldgen.VeilwoodBiomeInjector;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.minecraft.resources.ResourceLocation;

public class CreRacesFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Safe to call here: Fabric runs every "main" entrypoint (including TerraBlender's, which
        // loads TerraBlender.CONFIG) to completion before any "client" entrypoint starts.
        VeilwoodBiomeInjector.init();
        registerParticleProviders();

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

    // Registered with Fabric directly: Architectury 13's ParticleProviderRegistry is an empty stub.
    private static void registerParticleProviders() {
        ParticleFactoryRegistry particles = ParticleFactoryRegistry.getInstance();
        particles.register(ModParticles.MARKER.get(), MarkerParticle.Provider::new);
        particles.register(ModParticles.MARKER_MOVE.get(), MarkerParticle.Provider::new);
        particles.register(ModParticles.MARKER_ATTACK.get(), MarkerParticle.Provider::new);
        particles.register(ModParticles.POISON_EMITTER.get(), PoisonEmitterParticle.Provider::new);
        particles.register(ModParticles.MAGIC_DAMAGE.get(), DamageCritParticle.Provider::new);
        particles.register(ModParticles.PHYSICAL_DAMAGE.get(), DamageCritParticle.Provider::new);
        particles.register(ModParticles.TRUE_DAMAGE.get(), DamageCritParticle.Provider::new);
        particles.register(ModParticles.VEIL_EMBER.get(), VeilEmberParticle.Provider::new);
        particles.register(ModParticles.VEIL_MIST.get(), VeilMistParticle.Provider::new);
        particles.register(ModParticles.ESSENCE_PARTICLE.get(), EssenceParticle.Provider::new);
    }
}
