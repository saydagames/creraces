package mc.sayda.creraces.engine.traits;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.TraitRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public class DoubleJumpTrait implements TraitRegistry.RaceTrait {
    private final mc.sayda.creraces.engine.ScalingValue maxJumps;

    public DoubleJumpTrait(mc.sayda.creraces.engine.ScalingValue maxJumps) {
        this.maxJumps = maxJumps;
    }

    public mc.sayda.creraces.engine.ScalingValue getMaxJumps() {
        return maxJumps;
    }

    public static void register() {
        TraitRegistry.register(new ResourceLocation(CreRaces.MODID, "double_jump"), json -> {
            mc.sayda.creraces.engine.ScalingValue jumps = mc.sayda.creraces.engine.ScalingValue.fromJson(json,
                    "max_jumps", 1.0);
            return new DoubleJumpTrait(jumps);
        });
    }
}
