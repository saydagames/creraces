package mc.sayda.creraces.util;

import java.util.regex.Pattern;

/**
 * Utility to clean up wikitext artifacts like [[Page|Title]] and '''bold'''.
 */
public class WikitextUtil {
    private static final Pattern LINK_TITLED = Pattern.compile("\\[\\[[^|]+\\|([^\\]]+)\\]\\]");
    private static final Pattern LINK_SIMPLE = Pattern.compile("\\[\\[([^\\]]+)\\]\\]");
    private static final Pattern BOLD = Pattern.compile("'''");
    private static final Pattern ITALIC = Pattern.compile("''");
    private static final Pattern FILE_OR_CATEGORY = Pattern.compile("\\[\\[(File|Category):[^\\]]+\\]\\]",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_TAGS = Pattern.compile("<[^>]+>");

    public static String clean(String wikitext) {
        if (wikitext == null)
            return null;

        String cleaned = wikitext;

        // Strip File and Category links entirely
        cleaned = FILE_OR_CATEGORY.matcher(cleaned).replaceAll("");

        // Replace [[Page|Title]] with Title
        cleaned = LINK_TITLED.matcher(cleaned).replaceAll("$1");

        // Replace [[Page]] with Page
        cleaned = LINK_SIMPLE.matcher(cleaned).replaceAll("$1");

        // Strip bold and italic marks
        cleaned = BOLD.matcher(cleaned).replaceAll("");
        cleaned = ITALIC.matcher(cleaned).replaceAll("");

        // Strip HTML tags (like <br/> or <center>)
        cleaned = HTML_TAGS.matcher(cleaned).replaceAll("");

        return cleaned.trim();
    }
}
