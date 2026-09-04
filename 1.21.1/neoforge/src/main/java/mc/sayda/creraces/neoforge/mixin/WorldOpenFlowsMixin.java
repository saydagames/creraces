package mc.sayda.creraces.neoforge.mixin;

import mc.sayda.creraces.neoforge.migration.LegacyWorldLoadGate;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts "Play Selected World" before any world/integrated-server loading begins, so a
 * leftover CreRaces Classic world can be gated client-side with no thread ever blocked, see
 * LegacyWorldLoadGate for why that matters. 1.20.1 hooked loadLevel(Screen, String); 1.21
 * renamed and reshaped that entry point to openWorld(String, Runnable).
 */
@Mixin(WorldOpenFlows.class)
public class WorldOpenFlowsMixin {

    @Inject(method = "openWorld", at = @At("HEAD"), cancellable = true)
    private void creraces$interceptOpenWorld(String levelId, Runnable onFail, CallbackInfo ci) {
        if (LegacyWorldLoadGate.interceptOpenWorld(levelId, onFail)) {
            ci.cancel();
        }
    }
}
