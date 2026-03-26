package mc.sayda.creraces.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;

public class GsonHelper {
    @Nonnull
    public static String getAsString(JsonObject json, String memberName, @Nonnull String fallback) {
        if (!json.has(memberName) || json.get(memberName).isJsonNull())
            return fallback;
        return java.util.Objects.requireNonNull(json.get(memberName).getAsString());
    }

    @Nonnull
    public static String getAsString(JsonObject json, String memberName) {
        if (json.has(memberName))
            return java.util.Objects.requireNonNull(json.get(memberName).getAsString());
        throw new com.google.gson.JsonSyntaxException("Missing " + memberName);
    }

    @javax.annotation.Nullable
    public static String getNullableString(JsonObject json, String memberName,
            @javax.annotation.Nullable String fallback) {
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

    @Nonnull
    public static Map<ResourceLocation, JsonElement> getJsonFiles(ResourceManager resourceManager, String folder) {
        Map<ResourceLocation, JsonElement> map = new HashMap<>();
        resourceManager.listResources(folder, path -> path.getPath().endsWith(".json")).forEach((id, resource) -> {
            try (java.io.InputStream is = resource.open()) {
                JsonElement json = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(is));

                String path = id.getPath();
                // Find the index after the folder name and the following slash
                int startIndex = path.lastIndexOf(folder + "/");
                if (startIndex == -1) {
                    startIndex = path.indexOf("/") + 1;
                } else {
                    startIndex += folder.length() + 1;
                }

                String name = path.substring(startIndex, path.length() - 5);
                ResourceLocation registryId = new ResourceLocation(id.getNamespace(), name);

                map.put(registryId, json);
                mc.sayda.creraces.CreRaces.LOGGER.info("Discovered JSON file: {} -> {} from pack: {}", id, registryId,
                        resource.sourcePackId());
            } catch (Exception e) {
                mc.sayda.creraces.CreRaces.LOGGER.error("Failed to parse JSON file {}: {}", id, e.getMessage());
            }
        });

        // Developer Fallback: scan local filesystem if DEVELOPER_RESOURCE_PATH is set
        String devPathStr = mc.sayda.creraces.config.CreRacesConfig.DEVELOPER_RESOURCE_PATH.get();
        if (devPathStr != null && !devPathStr.isEmpty()) {
            java.io.File devDir = new java.io.File(devPathStr);
            if (devDir.exists() && devDir.isDirectory()) {
                // Expected structure: <devPath>/<namespace>/<folder>/...
                java.io.File[] namespaces = devDir.listFiles(java.io.File::isDirectory);
                if (namespaces != null) {
                    for (java.io.File namespaceDir : namespaces) {
                        String namespace = namespaceDir.getName();
                        java.io.File categoryDir = new java.io.File(namespaceDir, folder);
                        if (categoryDir.exists() && categoryDir.isDirectory()) {
                            // IF we are overriding a namespace with dev-path, clear existing entries for
                            // that namespace
                            // to ensure deletions in the source directory are reflected (ignoring stale
                            // build folder files).
                            map.keySet().removeIf(id -> id.getNamespace().equals(namespace));

                            scanDevDirectory(categoryDir, namespace, "", map);
                        }
                    }
                }
            }
        }

        return map;
    }

    private static void scanDevDirectory(java.io.File dir, String namespace, String prefix,
            Map<ResourceLocation, JsonElement> map) {
        java.io.File[] files = dir.listFiles();
        if (files == null)
            return;

        for (java.io.File file : files) {
            if (file.isDirectory()) {
                scanDevDirectory(file, namespace, prefix + file.getName() + "/", map);
            } else if (file.getName().endsWith(".json")) {
                try (java.io.FileReader reader = new java.io.FileReader(file)) {
                    JsonElement json = com.google.gson.JsonParser.parseReader(reader);
                    String name = prefix + file.getName().substring(0, file.getName().length() - 5);
                    ResourceLocation registryId = new ResourceLocation(namespace, name);
                    map.put(registryId, json);
                    mc.sayda.creraces.CreRaces.LOGGER.info("Discovered DEV JSON file: {} -> {}", file.getAbsolutePath(),
                            registryId);
                } catch (Exception e) {
                    mc.sayda.creraces.CreRaces.LOGGER.error("Failed to parse DEV JSON file {}: {}",
                            file.getAbsolutePath(), e.getMessage());
                }
            }
        }
    }
}

            
            

        
                            
                            