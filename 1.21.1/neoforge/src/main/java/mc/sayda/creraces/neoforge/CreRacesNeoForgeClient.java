package mc.sayda.creraces.neoforge;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.client.particle.DamageCritParticle;
import mc.sayda.creraces.client.particle.EssenceParticle;
import mc.sayda.creraces.client.particle.MarkerParticle;
import mc.sayda.creraces.client.particle.PoisonEmitterParticle;
import mc.sayda.creraces.client.particle.VeilEmberParticle;
import mc.sayda.creraces.client.particle.VeilMistParticle;
import mc.sayda.creraces.registry.ModParticles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/**
 * Architectury's ParticleProviderRegistry never wires providers into NeoForge's real
 * RegisterParticleProvidersEvent (same known bug class as MenuRegistry/EntityRenderersEvent on
 * NeoForge 1.21.1+, see architectury-api#517/#641) - registering natively here bypasses it.
 */
@EventBusSubscriber(modid = CreRaces.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CreRacesNeoForgeClient {

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.MARKER.get(), MarkerParticle.Provider::new);
        event.registerSpriteSet(ModParticles.MARKER_MOVE.get(), MarkerParticle.Provider::new);
        event.registerSpriteSet(ModParticles.MARKER_ATTACK.get(), MarkerParticle.Provider::new);
        event.registerSpriteSet(ModParticles.POISON_EMITTER.get(), PoisonEmitterParticle.Provider::new);
        event.registerSpriteSet(ModParticles.MAGIC_DAMAGE.get(), DamageCritParticle.Provider::new);
        event.registerSpriteSet(ModParticles.PHYSICAL_DAMAGE.get(), DamageCritParticle.Provider::new);
        event.registerSpriteSet(ModParticles.TRUE_DAMAGE.get(), DamageCritParticle.Provider::new);
        event.registerSpriteSet(ModParticles.VEIL_EMBER.get(), VeilEmberParticle.Provider::new);
        event.registerSpriteSet(ModParticles.VEIL_MIST.get(), VeilMistParticle.Provider::new);
        event.registerSpriteSet(ModParticles.ESSENCE_PARTICLE.get(), EssenceParticle.Provider::new);
    }
}
