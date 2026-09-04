package mc.sayda.creraces.util;

import com.google.gson.JsonElement;
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
    private static HttpClient getClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(mc.sayda.creraces.config.CreRacesConfig.DOC_FETCH_TIMEOUT_SECONDS.get()))
                .build();
    }

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
                // MediaWiki API Handling
                if (url.contains("action=parse")) {
                    JsonElement jsonElement = JsonParser.parseString(content);
                    if (jsonElement.isJsonObject()) {
                        JsonObject json = jsonElement.getAsJsonObject();
                        if (json.has("parse") && json.get("parse").isJsonObject()) {
                            JsonObject parse = json.getAsJsonObject("parse");
                            if (parse.has("wikitext") && parse.get("wikitext").isJsonObject()) {
                                JsonObject wikitext = parse.getAsJsonObject("wikitext");
                                if (wikitext.has("*")) {
                                    content = wikitext.get("*").getAsString();
                                } else {
                                    CreRaces.LOGGER.warn("DocFetcher: parse wikitext object missing '*' field for {}",
                                            url);
                                    return null;
                                }
                            } else {
                                CreRaces.LOGGER.warn("DocFetcher: parse action returned no wikitext object for {}",
                                        url);
                                return null;
                            }
                        } else {
                            CreRaces.LOGGER.warn("DocFetcher: parse action returned no 'parse' object for {}", url);
                            return null;
                        }
                    } else {
                        CreRaces.LOGGER.warn("DocFetcher: Received malformed JSON (not an object) for {}", url);
                        return null;
                    }
                } else if (url.contains("action=cargoquery")) {
                    // Extract first result from cargoquery
                    JsonObject json = JsonParser.parseString(content).getAsJsonObject();
                    if (json.has("cargoquery")) {
                        var array = json.getAsJsonArray("cargoquery");
                        if (array.size() > 0) {
                            var titleObj = array.get(0).getAsJsonObject().getAsJsonObject("title");

                            if (currentSelector != null && !currentSelector.isEmpty()
                                    && titleObj.has(currentSelector)) {
                                content = titleObj.get(currentSelector).getAsString();
                                currentSelector = null; // Field found, skip regex processing
                            } else {
                                var entries = titleObj.entrySet();
                                if (!entries.isEmpty()) {
                                    content = entries.iterator().next().getValue().getAsString();
                                } else {
                                    CreRaces.LOGGER.warn("DocFetcher: cargoquery returned empty title object for {}",
                                            url);
                                    return null; // Field not found
                                }
                            }
                        } else {
                            CreRaces.LOGGER.debug("DocFetcher: cargoquery returned 0 results for {}", url);
                            return null; // Empty array
                        }
                    } else {
                        CreRaces.LOGGER.warn("DocFetcher: cargoquery action returned no cargoquery block for {}", url);
                        return null; // No cargoquery block
                    }
                }

                // Apply regex selector if provided
                if (currentSelector != null && !currentSelector.isEmpty() && content != null) {
                    Pattern pattern = Pattern.compile(currentSelector, Pattern.DOTALL);
                    Matcher matcher = pattern.matcher(content);
                    if (matcher.find()) {
                        // Return first group if exists, otherwise the whole match
                        String result = matcher.groupCount() > 0 ? matcher.group(1).trim() : matcher.group().trim();
                        CreRaces.LOGGER.debug("DocFetcher: Regex matched group for {}", url);
                        return WikitextUtil.clean(result);
                    }

                    CreRaces.LOGGER.warn("DocFetcher: Regex selector '{}' found no match in content for {}",
                            currentSelector,
                            url);
                    // If selector was provided but not found, return null to allow fallback
                    return null;
                }
                return WikitextUtil.clean(content != null ? content.trim() : null);
            } catch (Exception e) {
                CreRaces.LOGGER.error("DocFetcher failed for {}: {} ({})", url, e.getMessage(),
                        e.getClass().getSimpleName());
                if (CreRaces.LOGGER.isDebugEnabled()) {
                    e.printStackTrace();
                }
                return null;
            }
        });
    }
}
