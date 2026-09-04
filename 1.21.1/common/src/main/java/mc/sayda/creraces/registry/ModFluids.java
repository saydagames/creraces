package mc.sayda.creraces.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.fluid.EterveilFluid;
import mc.sayda.creraces.fluid.FairySourceFluid;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.FlowingFluid;

public class ModFluids {

    public static final DeferredRegister<net.minecraft.world.level.material.Fluid> FLUIDS =
            DeferredRegister.create(CreRaces.MODID, Registries.FLUID);

    public static final RegistrySupplier<FlowingFluid> FAIRY_SOURCE =
            FLUIDS.register("fairy_source", FairySourceFluid.Source::new);

    public static final RegistrySupplier<FlowingFluid> FAIRY_SOURCE_FLOWING =
            FLUIDS.register("fairy_source_flowing", FairySourceFluid.Flowing::new);

    // Registry name intentionally diverges from Classic's "blessed_water", see LegacyBlockRemaps.
    public static final RegistrySupplier<FlowingFluid> ETERVEIL =
            FLUIDS.register("eterveil", EterveilFluid.Source::new);

    public static final RegistrySupplier<FlowingFluid> ETERVEIL_FLOWING =
            FLUIDS.register("eterveil_flowing", EterveilFluid.Flowing::new);

    public static void register() {
        FLUIDS.register();
    }
}
