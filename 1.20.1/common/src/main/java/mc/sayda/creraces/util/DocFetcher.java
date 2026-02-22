package mc.sayda.creraces.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mc.sayda.creraces.CreRaces;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for fetching documentation from remote sources (Wiki API, GitHub,
 * etc.)
 */
public class DocFetcher {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Fetches documentation based on a RemoteDocConfig.
     * 
     * @param config The configuration for fetching.
     * @return A CompletableFuture with the fetched content.
     */
    public static CompletableFuture<String> fetch(RemoteDocConfig config) {
        return fetch(config.source(), config.selector()).thenApply(content -> {
            if (content == null || content.isEmpty()) {
                return config.fallback();
            }
            return content;
        });
    }

    /**
     * Fetches content from a URL asynchronously.
     * 
     * @param url      The URL to fetch from (e.g. MediaWiki API)
     * @param selector A regex selector to extract a specific portion of the
     *                 response.
     * @return A CompletableFuture with the extracted text.
     */
    public static CompletableFuture<String> fetch(String url, String selector) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "CreRaces-Minecraft-Mod/1.0")
                        .GET()
                        .build();

                HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    CreRaces.LOGGER.warn("DocFetcher: HTTP error {} for {}", response.statusCode(), url);
                    return null;
                }

                String content = response.body();

                // MediaWiki API Handling
                if (url.contains("action=parse")) {
                    JsonObject json = JsonParser.parseString(content).getAsJsonObject();
                    if (json.has("parse") && json.getAsJsonObject("parse").has("wikitext")) {
                        content = json.getAsJsonObject("parse").getAsJsonObject("wikitext").get("*").getAsString();
                    }
                } else if (url.contains("action=cargoquery")) {
                    // Extract first result from cargoquery
                    JsonObject json = JsonParser.parseString(content).getAsJsonObject();
                    if (json.has("cargoquery")) {
                        var array = json.getAsJsonArray("cargoquery");
                        if (array.size() > 0) {
                            var titleObj = array.get(0).getAsJsonObject().getAsJsonObject("title");
                            // Extract first available field
                            var entries = titleObj.entrySet();
                            if (!entries.isEmpty()) {
                                content = entries.iterator().next().getValue().getAsString();
                            }
                        }
                    }
                }

                // Apply regex selector if provided
                if (selector != null && !selector.isEmpty() && content != null) {
                    Pattern pattern = Pattern.compile(selector, Pattern.DOTALL);
                    Matcher matcher = pattern.matcher(content);
                    if (matcher.find()) {
                        // Return first group if exists, otherwise the whole match
                        return matcher.groupCount() > 0 ? matcher.group(1) : matcher.group();
                    }
                }

                return content;
            } catch (Exception e) {
                CreRaces.LOGGER.error("DocFetcher failed for {}: {}", url, e.getMessage());
                return null;
            }
        });
    }
}
