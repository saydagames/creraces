package mc.sayda.creraces.util;

import mc.sayda.creraces.config.CreRacesConfig;
import net.minecraft.network.chat.Component;

public class WikiUtils {
    public static String getAbilityUrl(Component name) {
        String nameStr = name.getString().replace(" ", "_");
        String path = CreRacesConfig.WIKI_PAGE_PATH.get();
        String namespace = CreRacesConfig.WIKI_ABILITY_NAMESPACE.get();

        // Ensure proper capitalization for wiki links
        if (!nameStr.isEmpty() && Character.isLowerCase(nameStr.charAt(0))) {
            nameStr = Character.toUpperCase(nameStr.charAt(0)) + nameStr.substring(1);
        }

        String baseUrl = getBaseWikiUrl();
        if (!baseUrl.endsWith("/"))
            baseUrl += "/";
        if (!path.endsWith("/"))
            path += "/";

        return baseUrl + path + namespace + ":" + nameStr;
    }

    public static String getRaceUrl(Component name) {
        String nameStr = name.getString().replace(" ", "_");
        String path = CreRacesConfig.WIKI_PAGE_PATH.get();
        if (!path.endsWith("/"))
            path += "/";
        return getBaseWikiUrl() + path + nameStr;
    }

    public static String getBaseWikiUrl() {
        String url = CreRacesConfig.WIKI_BASE_URL.get();
        if (!url.endsWith("/"))
            url += "/";
        return url;
    }

    public static String getWikiApiUrl(String pageName) {
        try {
            String encodedPage = java.net.URLEncoder.encode(pageName, java.nio.charset.StandardCharsets.UTF_8.toString());
            encodedPage = encodedPage.replace("+", "%20");

            String apiBase = CreRacesConfig.WIKI_API_BASE.get();
            if (!apiBase.endsWith("/"))
                apiBase += "/";

            return apiBase + "api.php"
                    + "?action=parse&format=json&prop=wikitext&redirects=true&page=" + encodedPage;
        } catch (java.io.UnsupportedEncodingException e) {
            String apiBase = CreRacesConfig.WIKI_API_BASE.get();
            if (!apiBase.endsWith("/"))
                apiBase += "/";
            return apiBase + "api.php"
                    + "?action=parse&format=json&prop=wikitext&page=" + pageName.replace(" ", "%20");
        }
    }
}
