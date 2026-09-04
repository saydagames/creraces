package mc.sayda.creraces.util;

import mc.sayda.creraces.CreRaces;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Resolves this mod's own small standalone per-world files under a single
 * {@code <world>/creraces/} folder, instead of scattering them loose in the world's root
 * directory alongside vanilla's own files. Path-based overload exists because some callers (e.g.
 * the singleplayer pre-load migration gate) need to resolve a world's data folder before a
 * MinecraftServer object even exists.
 */
public final class WorldDataPaths {
    private static final String DATA_DIR = "creraces";

    private WorldDataPaths() {}

    public static Path resolve(MinecraftServer server, String fileName) {
        return resolve(server.getWorldPath(Objects.requireNonNull(LevelResource.ROOT)), fileName);
    }

    public static Path resolve(Path worldRoot, String fileName) {
        Path dir = worldRoot.resolve(DATA_DIR);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            CreRaces.LOGGER.error("[CreRaces] Failed to create the creraces/ data folder: {}", e.getMessage());
        }
        return dir.resolve(fileName);
    }
}
