package mc.sayda.creraces.util;

import com.google.gson.JsonObject;

import javax.annotation.Nonnull;

/**
 * Configuration for fetching remote documentation.
 */
public class RemoteDocConfig {
    private final String source;
    private final String selector;
    private final String fallback;

    public RemoteDocConfig(@Nonnull String source, @Nonnull String selector, @Nonnull String fallback) {
        this.source = source;
        this.selector = selector;
        this.fallback = fallback;
    }

    public String source() {
        return source;
    }

    public String selector() {
        return selector;
    }

    public String fallback() {
        return fallback;
    }

    public static final String INFODOC_SELECTOR = "(?i)(?:\\|\\s*description\\s*=\\s*|==\\s*Description\\s*==[\\s\\r\\n]*)(.*?)(?=\\s*(?:\\||\\}\\}|==|$))";
    public static final String PASSIVE_SELECTOR = "(?i)==\\s*(?:Racial\\s+)?Passives?\\s*==[\\s\\r\\n]*(.*?)(?=[\\s\\r\\n]+==|$)";
    public static final String HEADERDOC_SELECTOR = "(?i)==\\s*Description\\s*==[\\s\\r\\n]*(.*?)(?=[\\s\\r\\n]+==|$)";
    public static final String RACE_DESCRIPTION_SELECTOR = INFODOC_SELECTOR;

    public static RemoteDocConfig fromJson(JsonObject json) {
        if (json == null)
            return null;
        return new RemoteDocConfig(
                GsonHelper.getAsString(json, "source", ""),
                GsonHelper.getAsString(json, "selector", ""),
                GsonHelper.getAsString(json, "fallback", ""));
    }

    public static RemoteDocConfig fromWikiPage(String pageName, String selector, String fallback) {
        return new RemoteDocConfig(WikiUtils.getWikiApiUrl(pageName), selector, fallback);
    }
}
