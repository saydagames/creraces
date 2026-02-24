package mc.sayda.creraces.util;

import net.minecraft.network.chat.Component;

public class WikiUtils {
    public static String getAbilityUrl(Component name) {
        String nameStr = name.getString().replace(" ", "_");
        return getBaseWikiUrl() + mc.sayda.creraces.config.CreRacesConfig.WIKI_PAGE_PATH.get()
                + mc.sayda.creraces.config.CreRacesConfig.WIKI_ABILITY_NAMESPACE.get() + nameStr;
    }

    public static String getRaceUrl(Component name) {
        String nameStr = name.getString().replace(" ", "_");
        return getBaseWikiUrl() + mc.sayda.creraces.config.CreRacesConfig.WIKI_PAGE_PATH.get() + nameStr;
    }

    public static String getBaseWikiUrl() {
        return mc.sayda.creraces.config.CreRacesConfig.WIKI_BASE_URL.get();
    }
}
