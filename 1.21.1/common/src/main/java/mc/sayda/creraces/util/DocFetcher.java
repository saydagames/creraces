package mc.sayda.creraces.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.config.CreRacesConfig;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetches documentation text from remote sources (MediaWiki parse and cargoquery APIs, or plain
 * pages), optionally narrowed down by a regex selector.
 */
public class DocFetcher {
    private static HttpClient getClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(CreRacesConfig.DOC_FETCH_TIMEOUT_SECONDS.get()))
                .build();
    }

    /**
     * Fetches {@code url} asynchronously. For a cargoquery URL the selector may name a result
     * field; otherwise it is a regex whose first group (or whole match) is returned. Completes
     * with null on any failure so callers can fall back.
     */
    public static CompletableFuture<String> fetch(String url, String selector) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                CreRaces.LOGGER.debug("DocFetcher: Fetching {}", url);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url.replace(" ", "%20")))
                        .header("User-Agent", "CreRaces-Minecraft-Mod/1.0")
                        .GET()
                        .build();

                HttpResponse<String> response = getClient().send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    CreRaces.LOGGER.warn("DocFetcher: HTTP error {} for {}", response.statusCode(), url);
                    return null;
                }

                String content = response.body();
                CreRaces.LOGGER.debug("DocFetcher: Received content length {} for {}", content.length(), url);

                String currentSelector = selector;
                if (url.contains("action=parse")) {
                    content = extractParsedWikitext(content, url);
                    if (content == null) return null;
                } else if (url.contains("action=cargoquery")) {
                    JsonObject titleObj = firstCargoResult(content, url);
                    if (titleObj == null) return null;
                    if (currentSelector != null && !currentSelector.isEmpty() && titleObj.has(currentSelector)) {
                        content = titleObj.get(currentSelector).getAsString();
                        currentSelector = null; // The selector named a field, so there is nothing left to match.
                    } else {
                        Set<Map.Entry<String, JsonElement>> entries = titleObj.entrySet();
                        if (entries.isEmpty()) {
                            CreRaces.LOGGER.warn("DocFetcher: cargoquery returned empty title object for {}", url);
                            return null;
                        }
                        content = entries.iterator().next().getValue().getAsString();
                    }
                }

                if (currentSelector != null && !currentSelector.isEmpty() && content != null) {
                    Matcher matcher = Pattern.compile(currentSelector, Pattern.DOTALL).matcher(content);
                    if (matcher.find()) {
                        String result = matcher.groupCount() > 0 ? matcher.group(1).trim() : matcher.group().trim();
                        CreRaces.LOGGER.debug("DocFetcher: Regex matched group for {}", url);
                        return WikitextUtil.clean(result);
                    }

                    CreRaces.LOGGER.warn("DocFetcher: Regex selector '{}' found no match in content for {}",
                            currentSelector, url);
                    // A selector that finds nothing yields null so the caller shows its fallback.
                    return null;
                }
                return WikitextUtil.clean(content != null ? content.trim() : null);
            } catch (Exception e) {
                // Runs on a pool thread with nothing above to catch it; any failure just means no remote text.
                CreRaces.LOGGER.error("DocFetcher failed for {}: {} ({})", url, e.getMessage(),
                        e.getClass().getSimpleName());
                CreRaces.LOGGER.debug("DocFetcher stack trace", e);
                return null;
            }
        });
    }

    /** Pulls the raw wikitext out of a MediaWiki action=parse response, or null if it is missing. */
    @Nullable
    private static String extractParsedWikitext(String body, String url) {
        JsonElement jsonElement = JsonParser.parseString(body);
        if (!jsonElement.isJsonObject()) {
            CreRaces.LOGGER.warn("DocFetcher: Received malformed JSON (not an object) for {}", url);
            return null;
        }
        JsonObject json = jsonElement.getAsJsonObject();
        if (!json.has("parse") || !json.get("parse").isJsonObject()) {
            CreRaces.LOGGER.warn("DocFetcher: parse action returned no 'parse' object for {}", url);
            return null;
        }
        JsonObject parse = json.getAsJsonObject("parse");
        if (!parse.has("wikitext") || !parse.get("wikitext").isJsonObject()) {
            CreRaces.LOGGER.warn("DocFetcher: parse action returned no wikitext object for {}", url);
            return null;
        }
        JsonObject wikitext = parse.getAsJsonObject("wikitext");
        if (!wikitext.has("*")) {
            CreRaces.LOGGER.warn("DocFetcher: parse wikitext object missing '*' field for {}", url);
            return null;
        }
        return wikitext.get("*").getAsString();
    }

    /** The "title" object of the first cargoquery result, or null if there are no results. */
    @Nullable
    private static JsonObject firstCargoResult(String body, String url) {
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
        if (!json.has("cargoquery")) {
            CreRaces.LOGGER.warn("DocFetcher: cargoquery action returned no cargoquery block for {}", url);
            return null;
        }
        JsonArray array = json.getAsJsonArray("cargoquery");
        if (array.size() == 0) {
            CreRaces.LOGGER.debug("DocFetcher: cargoquery returned 0 results for {}", url);
            return null;
        }
        return array.get(0).getAsJsonObject().getAsJsonObject("title");
    }
}
