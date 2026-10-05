package mc.sayda.creraces.util;

import mc.sayda.creraces.CreRaces;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.zip.ZipFile;

/**
 * Custom RepositorySource that scans the mods folder for directories or .zip files containing a
 * pack.mcmeta. These are injected as "Race Packs" and act as both data and resource packs.
 */
public class RacePackProvider implements RepositorySource {
    /** Resolved against the game directory. */
    private static final String SCAN_DIR = "mods";

    // Labels these packs in the pack selection UI.
    public static final PackSource RACE_PACK_SOURCE = new PackSource() {
        @Override
        @Nonnull
        public Component decorate(@Nonnull Component component) {
            return Component.literal("Race Pack").append(" / ").append(component);
        }

        @Override
        public boolean shouldAddAutomatically() {
            return true;
        }
    };

    private final PackType packType;

    public RacePackProvider(PackType packType) {
        this.packType = packType;
    }

    @Override
    public void loadPacks(@Nonnull Consumer<Pack> consumer) {
        File dir = new File(SCAN_DIR);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        File[] files = dir.listFiles();
        if (files == null)
            return;

        for (File file : files) {
            boolean isZip = file.isFile() && file.getName().endsWith(".zip");
            // Mod jars are never considered, and folders or zips without a pack.mcmeta aren't packs.
            if (!(isZip || file.isDirectory()) || !hasPackMeta(file)) {
                continue;
            }

            String fileName = file.getName();
            String baseName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
            String id = "creraces_" + baseName.toLowerCase().replaceAll("[^a-z0-9_]", "_");

            Pack.ResourcesSupplier resourcesSupplier = (name) -> isZip
                ? new FilePackResources(id, file, false)
                : new PathPackResources(id, file.toPath(), false);

            Pack pack = Pack.readMetaAndCreate(
                    id,
                    Component.literal(file.getName()),
                    true, // required - forces it to be enabled and "fixed" in the UI
                    resourcesSupplier,
                    this.packType,
                    Pack.Position.TOP,
                    RACE_PACK_SOURCE);

            if (pack != null) {
                CreRaces.LOGGER.info("Discovered Race Pack ({}): {}", this.packType, id);
                consumer.accept(pack);
            }
        }
    }

    private boolean hasPackMeta(File file) {
        if (file.isDirectory()) {
            return new File(file, "pack.mcmeta").exists();
        } else if (file.isFile() && file.getName().endsWith(".zip")) {
            try (ZipFile zipFile = new ZipFile(file)) {
                return zipFile.getEntry("pack.mcmeta") != null;
            } catch (IOException e) {
                // An unreadable zip can't be loaded as a pack either.
                return false;
            }
        }
        return false;
    }
}
