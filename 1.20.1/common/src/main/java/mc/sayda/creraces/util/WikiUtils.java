package mc.sayda.creraces.util;

import net.minecraft.network.chat.Component;

public class WikiUtils {
    public static String getAbilityUrl(Component name) {
        String nameStr = name.getString().replace(" ", "_");
        return getBaseWikiUrl() + "wiki/"
                + "abilities/" + nameStr;
    }

    public static String getRaceUrl(Component name) {
        String nameStr = name.getString().replace(" ", "_");
        return getBaseWikiUrl() + "wiki/" + nameStr;
    }

    public static String getBaseWikiUrl() {
        return "https://wiki.saydagames.com/";
    }

    public static String getWikiApiUrl(String pageName) {
        return "https://api.wiki.saydagames.com/api.php"
                + "?action=parse&format=json&prop=wikitext&page=" + pageName.replace(" ", "%20");
    }
}
