package mc.sayda.creraces.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import java.util.HashMap;
import java.util.Map;

public class GsonHelper {
    public static String getAsString(JsonObject json, String memberName, String fallback) {
        if (!json.has(memberName) || json.get(memberName).isJsonNull())
            return fallback;
        return json.get(memberName).getAsString();
    }

    public static String getAsString(JsonObject json, String memberName) {
        if (json.has(memberName))
            return json.get(memberName).getAsString();
        throw new com.google.gson.JsonSyntaxException("Missing " + memberName);
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

    public static Map<ResourceLocation, JsonElement> getJsonFiles(ResourceManager resourceManager, String folder) {
        Map<ResourceLocation, JsonElement> map = new HashMap<>();
        resourceManager.listResources(folder, path -> path.getPath().endsWith(".json")).forEach((id, resource) -> {
            try (java.io.InputStream is = resource.open()) {
                JsonElement json = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(is));
                // Convert resource path to id (stripping races/ and .json)
                String path = id.getPath();
                String raceId = path.substring(folder.length() + 1, path.length() - 5);
                map.put(new ResourceLocation(id.getNamespace(), raceId), json);
            } catch (Exception e) {
                // Log error
            }
        });
        return map;
    }
}
