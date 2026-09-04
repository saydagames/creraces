package mc.sayda.creraces.forge.mixin;

import mc.sayda.creraces.forge.migration.LegacyWorldLoadGate;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts "Play Selected World" (WorldOpenFlows.loadLevel) before any world/integrated-server
 * loading begins, so a leftover CreRaces Classic world can be gated client-side with no thread
 * ever blocked, see LegacyWorldLoadGate for why that matters.
 */
@Mixin(WorldOpenFlows.class)
public class WorldOpenFlowsMixin {

    @Inject(method = "loadLevel", at = @At("HEAD"), cancellable = true)
    private void creraces$interceptLoadLevel(Screen screen, String levelId, CallbackInfo ci) {
        if (LegacyWorldLoadGate.interceptLoadLevel(screen, levelId)) {
            ci.cancel();
        }
    }
}
