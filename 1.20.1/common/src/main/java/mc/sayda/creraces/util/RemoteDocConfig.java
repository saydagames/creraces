package mc.sayda.creraces.util;

import com.google.gson.JsonObject;

import javax.annotation.Nonnull;

/**
 * Configuration for fetching remote documentation.
 */
public record RemoteDocConfig(@Nonnull String source, @Nonnull String selector, @Nonnull String fallback) {
    public static RemoteDocConfig fromJson(JsonObject json) {
        if (json == null)
            return null;
        return new RemoteDocConfig(
                GsonHelper.getAsString(json, "source", ""),
                GsonHelper.getAsString(json, "selector", ""),
                GsonHelper.getAsString(json, "fallback", ""));
    }
}
