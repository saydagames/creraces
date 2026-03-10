package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class AquaticMovementTrait implements TraitRegistry.RaceTrait {
    private final ScalingValue speed;
    private final boolean neutralBuoyancy;

    public AquaticMovementTrait(ScalingValue speed, boolean neutralBuoyancy) {
        this.speed = speed;
        this.neutralBuoyancy = neutralBuoyancy;
    }

    public ScalingValue getSpeed() {
        return speed;
    }

    public boolean isNeutralBuoyancy() {
        return neutralBuoyancy;
    }

    public static void register() {
        TraitRegistry.register(new ResourceLocation(CreRaces.MODID, "aquatic_movement"), json -> {
            ScalingValue speed = ScalingValue.fromJson(json, "speed", 0.05);
            boolean buoyancy = GsonHelper.getAsBoolean(json, "neutral_buoyancy", false);
            return new AquaticMovementTrait(speed, buoyancy);
        });
    }
}
