package mc.sayda.creraces.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(CreRaces.MODID, Registries.PARTICLE_TYPE);

    public static final RegistrySupplier<SimpleParticleType> MARKER = PARTICLES.register("marker", () -> new SimpleParticleType(true) {});
    public static final RegistrySupplier<SimpleParticleType> MARKER_MOVE = PARTICLES.register("marker_move", () -> new SimpleParticleType(true) {});
    public static final RegistrySupplier<SimpleParticleType> MARKER_ATTACK = PARTICLES.register("marker_attack", () -> new SimpleParticleType(true) {});
    public static final RegistrySupplier<SimpleParticleType> POISON_EMITTER = PARTICLES.register("poison_emitter", () -> new SimpleParticleType(true) {});

    public static void register() {
        PARTICLES.register();
    }
}
