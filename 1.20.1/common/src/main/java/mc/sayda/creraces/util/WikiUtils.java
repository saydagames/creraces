package mc.sayda.creraces.util;

import mc.sayda.creraces.config.CreRacesConfig;
import net.minecraft.network.chat.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class WikiUtils {
    public static String getAbilityUrl(Component name) {
        String nameStr = name.getString().replace(" ", "_");
        String path = withTrailingSlash(CreRacesConfig.WIKI_PAGE_PATH.get());
        String namespace = CreRacesConfig.WIKI_ABILITY_NAMESPACE.get();

        // Wiki page titles start with a capital letter.
        if (!nameStr.isEmpty() && Character.isLowerCase(nameStr.charAt(0))) {
            nameStr = Character.toUpperCase(nameStr.charAt(0)) + nameStr.substring(1);
        }

        return getBaseWikiUrl() + path + namespace + ":" + nameStr;
    }

    public static String getRaceUrl(Component name) {
        String nameStr = name.getString().replace(" ", "_");
        String path = withTrailingSlash(CreRacesConfig.WIKI_PAGE_PATH.get());
        return getBaseWikiUrl() + path + nameStr;
    }

    public static String getBaseWikiUrl() {
        return withTrailingSlash(CreRacesConfig.WIKI_BASE_URL.get());
    }

    public static String getWikiApiUrl(String pageName) {
        String encodedPage = URLEncoder.encode(pageName, StandardCharsets.UTF_8).replace("+", "%20");
        String apiBase = withTrailingSlash(CreRacesConfig.WIKI_API_BASE.get());
        return apiBase + "api.php"
                + "?action=parse&format=json&prop=wikitext&redirects=true&page=" + encodedPage;
    }

    private static String withTrailingSlash(String url) {
        return url.endsWith("/") ? url : url + "/";
    }
}
