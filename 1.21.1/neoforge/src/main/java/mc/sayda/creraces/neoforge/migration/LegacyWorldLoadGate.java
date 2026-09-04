package mc.sayda.creraces.neoforge.migration;

import mc.sayda.creraces.client.screen.LegacyMigrationPromptScreen;
import net.minecraft.client.Minecraft;

import java.nio.file.Path;

/**
 * Gates singleplayer world loading, client-side, BEFORE WorldOpenFlows ever opens the level, see
 * WorldOpenFlowsMixin. This is the singleplayer counterpart to LegacyMigrationHooks' dedicated
 * server console prompt, and deliberately does not block any thread: the integrated server does
 * not exist yet at this point, so there is nothing to race or hang.
 *
 * 1.21 replaced loadLevel(Screen, String) with openWorld(String, Runnable), so the "go back"
 * path runs the vanilla onFail callback instead of restoring a screen we were handed.
 */
public final class LegacyWorldLoadGate {
    /** Set right before we re-invoke openWorld ourselves, so the mixin lets that one call through. */
    private static volatile boolean bypassNextCheck = false;

    private LegacyWorldLoadGate() {}

    /** Called from WorldOpenFlowsMixin. Returns true if the original openWorld call should be canceled. */
    public static boolean interceptOpenWorld(String levelId, Runnable onFail) {
        if (bypassNextCheck) {
            bypassNextCheck = false;
            return false;
        }

        Path worldRoot = Minecraft.getInstance().getLevelSource().getBaseDir().resolve(levelId);
        if (!LegacyDetection.classicWorldPresent(worldRoot) || LegacyDetection.alreadyHandled(worldRoot)) {
            return false;
        }

        Minecraft.getInstance().setScreen(new LegacyMigrationPromptScreen(
                choice -> onChoiceMade(choice, levelId, worldRoot, onFail)));
        return true;
    }

    private static void onChoiceMade(int choice, String levelId, Path worldRoot, Runnable onFail) {
        switch (choice) {
            case 1 -> onFail.run();
            case 2 -> {
                LegacyDetection.writeMarker(worldRoot, "migrate");
                proceed(levelId, onFail);
            }
            default -> {
                LegacyDetection.writeMarker(worldRoot, "skip");
                proceed(levelId, onFail);
            }
        }
    }

    private static void proceed(String levelId, Runnable onFail) {
        bypassNextCheck = true;
        Minecraft.getInstance().createWorldOpenFlows().openWorld(levelId, onFail);
    }
}
