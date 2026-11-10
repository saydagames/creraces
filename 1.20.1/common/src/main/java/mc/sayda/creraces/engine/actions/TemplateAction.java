package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.TemplateManager;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs the action "template" (see TemplateManager) with "args" filled into its placeholders.
 * A list entry that is only a placeholder takes a list argument's entries, so callers can add
 * their own actions into a template. The filled template is built on first use and again after
 * every data reload.
 */
public class TemplateAction implements ActionRegistry.RaceAction {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\(([a-z0-9_]+)\\)");

    private final ResourceLocation templateId;
    private final JsonObject args;
    @Nullable
    private List<ActionRegistry.RaceAction> actions;
    private int builtFor = -1;

    public TemplateAction(ResourceLocation templateId, JsonObject args) {
        this.templateId = templateId;
        this.args = args;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        // Built once per reload; a missing template is reported once rather than on every use.
        if (builtFor != TemplateManager.generation()) {
            actions = build();
            builtFor = TemplateManager.generation();
        }
        return actions != null && ActionRegistry.runChain(actions, player, target, slot, interactPos);
    }

    @Nullable
    private List<ActionRegistry.RaceAction> build() {
        JsonObject template = TemplateManager.get(templateId);
        if (template == null) {
            CreRaces.LOGGER.error("Unknown action template '{}'", templateId);
            return null;
        }
        JsonObject filled = new JsonObject();
        try {
            filled.add("actions", fill(template.get("actions")));
        } catch (IllegalArgumentException e) {
            CreRaces.LOGGER.error("Template '{}': {}", templateId, e.getMessage());
            return null;
        }
        return ActionRegistry.listFromJson(filled, "actions");
    }

    private JsonElement fill(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject copy = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                copy.add(entry.getKey(), fill(entry.getValue()));
            }
            return copy;
        }
        if (element.isJsonArray()) {
            JsonArray copy = new JsonArray();
            for (JsonElement child : element.getAsJsonArray()) {
                JsonElement filled = fill(child);
                // A list entry that is just "$(name)" with a list argument splices the list in.
                if (isWholePlaceholder(child) && filled.isJsonArray()) {
                    copy.addAll(filled.getAsJsonArray());
                } else {
                    copy.add(filled);
                }
            }
            return copy;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String text = element.getAsString();
            Matcher whole = PLACEHOLDER.matcher(text);
            if (whole.matches()) {
                return argument(whole.group(1)).deepCopy();
            }
            Matcher matcher = PLACEHOLDER.matcher(text);
            StringBuilder result = new StringBuilder();
            while (matcher.find()) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(argument(matcher.group(1)).getAsString()));
            }
            matcher.appendTail(result);
            return new JsonPrimitive(result.toString());
        }
        return element.deepCopy();
    }

    private static boolean isWholePlaceholder(JsonElement element) {
        return element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()
                && PLACEHOLDER.matcher(element.getAsString()).matches();
    }

    private JsonElement argument(String name) {
        if (!args.has(name)) {
            throw new IllegalArgumentException("no argument \"" + name + "\" given");
        }
        return args.get(name);
    }

    public static void register() {
        ActionRegistry.register(CreRaces.id("template"), json -> {
            ResourceLocation id = ResourceLocation.tryParse(GsonHelper.getAsString(json, "template"));
            if (id == null) {
                throw new IllegalArgumentException("malformed template id");
            }
            return new TemplateAction(id, json.has("args") ? json.getAsJsonObject("args") : new JsonObject());
        });
    }
}
