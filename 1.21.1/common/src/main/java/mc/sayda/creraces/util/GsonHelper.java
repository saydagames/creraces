package mc.sayda.creraces.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.config.CreRacesConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GsonHelper {
    @Nonnull
    public static String getAsString(JsonObject json, String memberName, @Nonnull String fallback) {
        if (!json.has(memberName) || json.get(memberName).isJsonNull())
            return fallback;
        return Objects.requireNonNull(json.get(memberName).getAsString());
    }

    @Nonnull
    public static String getAsString(JsonObject json, String memberName) {
        if (json.has(memberName))
            return Objects.requireNonNull(json.get(memberName).getAsString());
        throw new JsonSyntaxException("Missing " + memberName);
    }

    @Nullable
    public static String getNullableString(JsonObject json, String memberName, @Nullable String fallback) {
        if (!json.has(memberName) || json.get(memberName).isJsonNull())
            return fallback;
        return json.get(memberName).getAsString();
    }

    public static int getAsInt(JsonObject json, String memberName, int fallback) {
        return json.has(memberName) ? json.get(memberName).getAsInt() : fallback;
    }

    public static double getAsDouble(JsonObject json, String memberName, double fallback) {
        return json.has(memberName) ? json.get(memberName).getAsDouble() : fallback;
    }

    public static float getAsFloat(JsonObject json, String memberName, float fallback) {
        return json.has(memberName) ? json.get(memberName).getAsFloat() : fallback;
    }

    public static boolean getAsBoolean(JsonObject json, String memberName, boolean fallback) {
        return json.has(memberName) ? json.get(memberName).getAsBoolean() : fallback;
    }

    /**
     * Reads every .json under {@code folder} from all loaded packs, keyed by namespace and the path
     * below the folder. When DEVELOPER_RESOURCE_PATH is set, that directory's files replace the
     * packaged ones namespace by namespace.
     */
    @Nonnull
    public static Map<ResourceLocation, JsonElement> getJsonFiles(ResourceManager resourceManager, String folder) {
        Map<ResourceLocation, JsonElement> map = new HashMap<>();
        resourceManager.listResources(folder, path -> path.getPath().endsWith(".json")).forEach((id, resource) -> {
            // JSON is UTF-8; the platform default charset (Java 17 on Windows) would mangle anything non-ASCII.
            try (InputStream is = resource.open()) {
                JsonElement json = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8));

                String path = id.getPath();
                // Find the index after the folder name and the following slash
                int startIndex = path.lastIndexOf(folder + "/");
                if (startIndex == -1) {
                    startIndex = path.indexOf("/") + 1;
                } else {
                    startIndex += folder.length() + 1;
                }

                String name = path.substring(startIndex, path.length() - 5);
                ResourceLocation registryId = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), name);

                map.put(registryId, json);
                CreRaces.LOGGER.info("Discovered JSON file: {} -> {} from pack: {}", id, registryId,
                        resource.sourcePackId());
            } catch (Exception e) {
                // Covers I/O, Gson and bad-id failures alike; one broken file must not stop the reload.
                CreRaces.LOGGER.error("Failed to parse JSON file {}: {}", id, e.getMessage());
            }
        });

        String devPathStr = CreRacesConfig.DEVELOPER_RESOURCE_PATH.get();
        if (devPathStr != null && !devPathStr.isEmpty()) {
            File devDir = new File(devPathStr);
            if (devDir.exists() && devDir.isDirectory()) {
                // Expected structure: <devPath>/<namespace>/<folder>/...
                File[] namespaces = devDir.listFiles(File::isDirectory);
                if (namespaces != null) {
                    for (File namespaceDir : namespaces) {
                        String namespace = namespaceDir.getName();
                        File categoryDir = new File(namespaceDir, folder);
                        if (categoryDir.exists() && categoryDir.isDirectory()) {
                            // Clear this namespace's entries first so dev-path deletions aren't masked by stale packaged files.
                            map.keySet().removeIf(id -> id.getNamespace().equals(namespace));

                            scanDevDirectory(categoryDir, namespace, "", map);
                        }
                    }
                }
            }
        }

        return map;
    }

    private static void scanDevDirectory(File dir, String namespace, String prefix,
            Map<ResourceLocation, JsonElement> map) {
        File[] files = dir.listFiles();
        if (files == null)
            return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanDevDirectory(file, namespace, prefix + file.getName() + "/", map);
            } else if (file.getName().endsWith(".json")) {
                try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                    JsonElement json = JsonParser.parseReader(reader);
                    String name = prefix + file.getName().substring(0, file.getName().length() - 5);
                    ResourceLocation registryId = ResourceLocation.fromNamespaceAndPath(namespace, name);
                    map.put(registryId, json);
                    CreRaces.LOGGER.info("Discovered DEV JSON file: {} -> {}", file.getAbsolutePath(),
                            registryId);
                } catch (Exception e) {
                    CreRaces.LOGGER.error("Failed to parse DEV JSON file {}: {}",
                            file.getAbsolutePath(), e.getMessage());
                }
            }
        }
    }
}
