package mc.sayda.creraces.util;

import java.util.regex.Pattern;

/**
 * Utility to clean up wikitext artifacts like [[Page|Title]] and '''bold'''.
 */
public class WikitextUtil {
    private static final Pattern LINK_TITLED = Pattern.compile("\\[\\[[^|]+\\|([^\\]]+)\\]\\]");
    private static final Pattern LINK_SIMPLE = Pattern.compile("\\[\\[([^\\]]+)\\]\\]");
    private static final Pattern WIKI_BOLD = Pattern.compile("'''");
    private static final Pattern WIKI_ITALIC = Pattern.compile("''");
    private static final Pattern WIKI_LIST = Pattern.compile("(?m)^[\\t ]*\\*[\\t ]+");
    private static final Pattern HTML_BOLD = Pattern.compile("<(b|strong)>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_BOLD_END = Pattern.compile("</(b|strong)>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_ITALIC = Pattern.compile("<(i|em)>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_ITALIC_END = Pattern.compile("</(i|em)>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_BR = Pattern.compile("<br\\s*/?>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_SPAN_COLOR = Pattern.compile("<span style=\"color:#([0-9a-fA-F]{6})\">",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_SPAN_END = Pattern.compile("</span>", Pattern.CASE_INSENSITIVE);
    private static final Pattern FILE_OR_CATEGORY = Pattern.compile("\\[\\[(File|Category):[^\\]]+\\]\\]",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern REMAINING_HTML = Pattern.compile("<[^>]+>");

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

        // Convert Wiki bullet points to actual bullets
        cleaned = WIKI_LIST.matcher(cleaned).replaceAll("  \u00A77\u2022 \u00A7r ");

        // Convert formatting
        cleaned = WIKI_BOLD.matcher(cleaned).replaceAll("\u00A7l");
        cleaned = WIKI_ITALIC.matcher(cleaned).replaceAll("\u00A7o");
        cleaned = HTML_BOLD.matcher(cleaned).replaceAll("\u00A7l");
        cleaned = HTML_BOLD_END.matcher(cleaned).replaceAll("\u00A7r");
        cleaned = HTML_ITALIC.matcher(cleaned).replaceAll("\u00A7o");
        cleaned = HTML_ITALIC_END.matcher(cleaned).replaceAll("\u00A7r");
        cleaned = HTML_BR.matcher(cleaned).replaceAll("\n");

        // Process color spans
        java.util.regex.Matcher colorMatcher = HTML_SPAN_COLOR.matcher(cleaned);
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;
        while (colorMatcher.find()) {
            sb.append(cleaned, lastEnd, colorMatcher.start());
            String hex = colorMatcher.group(1).toLowerCase();
            // Minecraft hex color format: §x§r§r§g§g§b§b
            sb.append("\u00A7x");
            for (char c : hex.toCharArray()) {
                sb.append("\u00A7").append(c);
            }
            lastEnd = colorMatcher.end();
        }
        sb.append(cleaned.substring(lastEnd));
        cleaned = sb.toString();

        cleaned = HTML_SPAN_END.matcher(cleaned).replaceAll("\u00A7r");

        // Strip remaining HTML tags
        cleaned = REMAINING_HTML.matcher(cleaned).replaceAll("");

        return cleaned.trim();
    }

    /**
     * Converts a string with legacy formatting codes (\u00A7) into a Component.
     */
    @javax.annotation.Nonnull
    public static net.minecraft.network.chat.Component toComponent(String text) {
        if (text == null || text.isEmpty())
            return net.minecraft.network.chat.Component.empty();

        net.minecraft.network.chat.MutableComponent root = net.minecraft.network.chat.Component.literal("");
        net.minecraft.network.chat.Style currentStyle = net.minecraft.network.chat.Style.EMPTY;
        StringBuilder currentText = new StringBuilder();

        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '\u00A7' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));

                // Check for hex: §x§r§r§g§g§b§b
                if (code == 'x' && i + 13 < text.length()) {
                    boolean isHex = true;
                    StringBuilder hex = new StringBuilder();
                    for (int j = 0; j < 6; j++) {
                        if (text.charAt(i + 2 + j * 2) == '\u00A7') {
                            hex.append(text.charAt(i + 3 + j * 2));
                        } else {
                            isHex = false;
                            break;
                        }
                    }
                    if (isHex) {
                        try {
                            int colorValue = Integer.parseInt(hex.toString(), 16);

                            // Flush current text with previous style
                            if (currentText.length() > 0) {
                                root.append(net.minecraft.network.chat.Component.literal(currentText.toString())
                                        .withStyle(currentStyle));
                                currentText.setLength(0);
                            }

                            currentStyle = currentStyle.withColor(colorValue);
                            i += 14;
                            continue;
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }

                // Flush previous text with previous style
                if (currentText.length() > 0) {
                    root.append(net.minecraft.network.chat.Component.literal(currentText.toString())
                            .withStyle(currentStyle));
                    currentText.setLength(0);
                }

                // Handle standard codes
                switch (code) {
                    case '0' -> currentStyle = currentStyle.withColor(0x000000);
                    case '1' -> currentStyle = currentStyle.withColor(0x0000AA);
                    case '2' -> currentStyle = currentStyle.withColor(0x00AA00);
                    case '3' -> currentStyle = currentStyle.withColor(0x00AAAA);
                    case '4' -> currentStyle = currentStyle.withColor(0xAA0000);
                    case '5' -> currentStyle = currentStyle.withColor(0xAA00AA);
                    case '6' -> currentStyle = currentStyle.withColor(0xFFAA00);
                    case '7' -> currentStyle = currentStyle.withColor(0xAAAAAA);
                    case '8' -> currentStyle = currentStyle.withColor(0x555555);
                    case '9' -> currentStyle = currentStyle.withColor(0x5555FF);
                    case 'a' -> currentStyle = currentStyle.withColor(0x55FF55);
                    case 'b' -> currentStyle = currentStyle.withColor(0x55FFFF);
                    case 'c' -> currentStyle = currentStyle.withColor(0xFF5555);
                    case 'd' -> currentStyle = currentStyle.withColor(0xFF55FF);
                    case 'e' -> currentStyle = currentStyle.withColor(0xFFFF55);
                    case 'f' -> currentStyle = currentStyle.withColor(0xFFFFFF);
                    case 'l' -> currentStyle = currentStyle.withBold(true);
                    case 'm' -> currentStyle = currentStyle.withStrikethrough(true);
                    case 'n' -> currentStyle = currentStyle.withUnderlined(true);
                    case 'o' -> currentStyle = currentStyle.withItalic(true);
                    case 'k' -> currentStyle = currentStyle.withObfuscated(true);
                    case 'r' -> currentStyle = net.minecraft.network.chat.Style.EMPTY;
                    default -> currentText.append(c).append(text.charAt(i + 1)); // Just treat as text if unknown
                }
                i += 2;
            } else {
                currentText.append(c);
                i++;
            }
        }

        if (currentText.length() > 0) {
            root.append(net.minecraft.network.chat.Component.literal(currentText.toString()).withStyle(currentStyle));
        }

        return root;
    }
}
