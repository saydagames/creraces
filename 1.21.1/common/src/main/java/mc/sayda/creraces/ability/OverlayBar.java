package mc.sayda.creraces.ability;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.engine.Resources;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * A HUD bar, declared in a race's or ability's "creraces:overlay_bars" list:
 * <pre>{ "source": "state:&lt;id&gt;" | "cooldown:&lt;id&gt;" | "resource:&lt;name&gt;", "max": 100,
 *   "color": "#FF4500", "name": "Fury Stacks" }</pre>
 * "name" may be a translation key. The bar is hidden while its value is 0.
 */
@SuppressWarnings("null")
public record OverlayBar(Kind kind, @Nullable ResourceLocation id, @Nullable Resources.Resource resource, int max,
        int color, String name) {

    public enum Kind { STATE, COOLDOWN, RESOURCE }

    private static final int DEFAULT_COLOR = 0xFFAAAAAA;

    public double getValue(Player player, IPlayerVariables vars) {
        return switch (kind) {
            case STATE -> vars.getPersistentState(id);
            case COOLDOWN -> vars.getCooldown(id);
            case RESOURCE -> resource.read(player);
        };
    }

    /** Reads the "creraces:overlay_bars" list; a broken entry is reported and left out. */
    public static List<OverlayBar> listFromJson(JsonObject json) {
        List<OverlayBar> bars = new ArrayList<>();
        if (!json.has("creraces:overlay_bars")) {
            return bars;
        }
        for (JsonElement element : json.getAsJsonArray("creraces:overlay_bars")) {
            try {
                bars.add(fromJson(element.getAsJsonObject()));
            } catch (Exception e) {
                CreRaces.LOGGER.error("Skipping overlay bar {}: {}", element, e.getMessage());
            }
        }
        return bars;
    }

    private static OverlayBar fromJson(JsonObject json) {
        String source = GsonHelper.getAsString(json, "source");
        int colon = source.indexOf(':');
        String prefix = colon > 0 ? source.substring(0, colon) : "";
        String rest = colon > 0 ? source.substring(colon + 1) : "";
        int max = GsonHelper.getAsInt(json, "max", 100);
        int color = GsonHelper.getAsColor(json, "color", DEFAULT_COLOR);
        String name = GsonHelper.getAsString(json, "name", rest);
        return switch (prefix) {
            case "state", "cooldown" -> {
                ResourceLocation id = ResourceLocation.tryParse(rest.contains(":") ? rest : CreRaces.MODID + ":" + rest);
                if (id == null) {
                    throw new IllegalArgumentException("malformed id in source '" + source + "'");
                }
                yield new OverlayBar(prefix.equals("state") ? Kind.STATE : Kind.COOLDOWN, id, null, max, color, name);
            }
            case "resource" -> {
                Resources.Resource resource = Resources.get(rest);
                if (resource == null) {
                    throw new IllegalArgumentException("unknown resource in source '" + source + "'");
                }
                yield new OverlayBar(Kind.RESOURCE, null, resource, max, color, name);
            }
            default -> throw new IllegalArgumentException(
                    "source '" + source + "' should start with state:, cooldown: or resource:");
        };
    }
}
