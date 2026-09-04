package mc.sayda.creraces.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.worldgen.EssenceVortexFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class ModFeatures {

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final DeferredRegister<Feature<?>> FEATURES =
            (DeferredRegister<Feature<?>>) (DeferredRegister) DeferredRegister.create(CreRaces.MODID, Registries.FEATURE);

    public static final RegistrySupplier<Feature<NoneFeatureConfiguration>> ESSENCE_VORTEX =
            FEATURES.register("essence_vortex", EssenceVortexFeature::new);

    public static void register() {
        FEATURES.register();
    }
}
