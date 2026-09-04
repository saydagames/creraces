package mc.sayda.creraces.util;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Custom RepositorySource that scans a specific directory (default: "mods")
 * for folders or .zip files containing a pack.mcmeta.
 * These are injected as "Race Packs" and act as both Data and Resource packs.
 */
public class RacePackProvider implements RepositorySource {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final PackType packType;

    public RacePackProvider(PackType packType) {
        this.packType = packType;
    }

    // Internal string for easy relocation
    public static String SCAN_DIR = "mods";

    // Custom PackSource to label these in the UI
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
            boolean isFolder = file.isDirectory();

            if (isZip || isFolder) {
                // Quick check for pack.mcmeta to avoid loading random jars
                if (hasPackMeta(file)) {
                    String fileName = file.getName();
                    String baseName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
                    String id = "creraces_" + baseName.toLowerCase().replaceAll("[^a-z0-9_]", "_");

                    // 1.21 moved the pack id/title/source into PackLocationInfo, and the
                    // zip-vs-folder split into vanilla-provided ResourcesSupplier types.
                    Pack.ResourcesSupplier resourcesSupplier = isZip
                        ? new FilePackResources.FileResourcesSupplier(file)
                        : new PathPackResources.PathResourcesSupplier(file.toPath());

                    net.minecraft.server.packs.PackLocationInfo locationInfo =
                            new net.minecraft.server.packs.PackLocationInfo(
                                    id,
                                    Component.literal(file.getName()),
                                    RACE_PACK_SOURCE,
                                    java.util.Optional.empty());

                    // required=true forces it enabled and "fixed" in the UI
                    net.minecraft.server.packs.PackSelectionConfig selectionConfig =
                            new net.minecraft.server.packs.PackSelectionConfig(true, Pack.Position.TOP, false);

                    Pack pack = Pack.readMetaAndCreate(locationInfo, resourcesSupplier, this.packType, selectionConfig);

                    if (pack != null) {
                        LOGGER.info("Discovered Race Pack ({}): {}", this.packType, id);
                        consumer.accept(pack);
                    }
                }
            }
        }
    }

    private boolean hasPackMeta(File file) {
        if (file.isDirectory()) {
            return new File(file, "pack.mcmeta").exists();
        } else if (file.isFile() && file.getName().endsWith(".zip")) {
            try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(file)) {
                return zipFile.getEntry("pack.mcmeta") != null;
            } catch (java.io.IOException e) {
                return false;
            }
        }
        return false;
    }
}
