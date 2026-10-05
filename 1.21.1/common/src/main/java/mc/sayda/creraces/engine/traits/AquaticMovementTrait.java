package mc.sayda.creraces.engine.traits;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;

/** Water movement settings read by AquaticMovementHandler. */
public class AquaticMovementTrait implements TraitRegistry.RaceTrait {
    private final boolean neutralBuoyancy;

    public AquaticMovementTrait(boolean neutralBuoyancy) {
        this.neutralBuoyancy = neutralBuoyancy;
    }

    public boolean isNeutralBuoyancy() {
        return neutralBuoyancy;
    }

    public static void register() {
        TraitRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "aquatic_movement"), json -> {
            boolean buoyancy = GsonHelper.getAsBoolean(json, "neutral_buoyancy", false);
            return new AquaticMovementTrait(buoyancy);
        });
    }
}
