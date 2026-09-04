package mc.sayda.creraces.forge.migration;

import mc.sayda.creraces.client.screen.LegacyMigrationPromptScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.nio.file.Path;

/**
 * Gates singleplayer world loading, client-side, BEFORE WorldOpenFlows.loadLevel ever runs, see
 * WorldOpenFlowsMixin. This is the singleplayer counterpart to LegacyMigrationHooks' dedicated
 * server console prompt, and deliberately does not block any thread: the integrated server
 * doesn't exist yet at this point, so there is nothing to race or hang.
 */
public final class LegacyWorldLoadGate {
    /** Set right before we re-invoke loadLevel ourselves, so the mixin lets that one call through. */
    private static volatile boolean bypassNextCheck = false;

    private LegacyWorldLoadGate() {}

    /** Called from WorldOpenFlowsMixin. Returns true if the original loadLevel call should be canceled. */
    public static boolean interceptLoadLevel(Screen previousScreen, String levelId) {
        if (bypassNextCheck) {
            bypassNextCheck = false;
            return false;
        }

        Path worldRoot = Minecraft.getInstance().getLevelSource().getBaseDir().resolve(levelId);
        if (!LegacyDetection.classicWorldPresent(worldRoot) || LegacyDetection.alreadyHandled(worldRoot)) {
            return false;
        }

        Minecraft.getInstance().setScreen(new LegacyMigrationPromptScreen(
                choice -> onChoiceMade(choice, previousScreen, levelId, worldRoot)));
        return true;
    }

    private static void onChoiceMade(int choice, Screen previousScreen, String levelId, Path worldRoot) {
        switch (choice) {
            case 1 -> Minecraft.getInstance().setScreen(previousScreen);
            case 2 -> {
                LegacyDetection.writeMarker(worldRoot, "migrate");
                proceed(previousScreen, levelId);
            }
            default -> {
                LegacyDetection.writeMarker(worldRoot, "skip");
                proceed(previousScreen, levelId);
            }
        }
    }

    private static void proceed(Screen previousScreen, String levelId) {
        bypassNextCheck = true;
        Minecraft.getInstance().createWorldOpenFlows().loadLevel(previousScreen, levelId);
    }
}
