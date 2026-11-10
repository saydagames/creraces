package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.EntitySelector;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.IllegalFormatException;
import java.util.List;

/**
 * Shows a player (the caster by default) a message in chat or on the action bar. Text without
 * spaces or '&' is a translation key; anything else is literal text with '&' colour codes. An
 * optional "values" list (scaling values) fills the translation's arguments, is formatted into
 * the literal text with %s / %d, or is appended to it. Values are rounded to whole numbers unless
 * "decimals" is set.
 */
public class MessageAction implements ActionRegistry.RaceAction {
    public static final ResourceLocation ID = CreRaces.id("message");

    private final String text;
    private final boolean actionbar;
    private final List<ScalingValue> values;
    private final boolean decimals;
    private final EntitySelector entity;

    public MessageAction(String text, boolean actionbar, List<ScalingValue> values, boolean decimals,
            EntitySelector entity) {
        this.text = text;
        this.actionbar = actionbar;
        this.values = values;
        this.decimals = decimals;
        this.entity = entity;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (!text.isEmpty() && entity.resolve(player, target) instanceof Player receiver) {
            receiver.displayClientMessage(buildMessage(player, target, slot, interactPos), actionbar);
        }
        return true;
    }

    private Component buildMessage(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        boolean translatable = !text.contains(" ") && !text.contains("&");
        Object[] args = new Object[values.size()];
        for (int i = 0; i < args.length; i++) {
            args[i] = boxed(values.get(i).evaluate(player, target, slot, interactPos), decimals);
        }
        if (translatable) {
            return Component.translatable(text, args);
        }

        String literal = text.replace('&', ChatFormatting.PREFIX_CODE);
        if (args.length == 0) {
            return Component.literal(literal);
        }
        if (literal.contains("%")) {
            try {
                literal = String.format(literal, args);
            } catch (IllegalFormatException e) {
                // A format that doesn't fit the arguments is shown as written.
            }
        } else {
            for (Object arg : args) {
                literal += arg;
            }
        }
        return Component.literal(literal);
    }

    /**
     * A Long prints as "3" where the Double would print "3.0". Kept out of a ternary on purpose:
     * mixing double and long there promotes the long straight back to a double.
     */
    private static Object boxed(double value, boolean asDouble) {
        if (asDouble) {
            return value;
        }
        return Math.round(value);
    }

    public static void register() {
        ActionRegistry.register(ID, json -> {
            List<ScalingValue> values = new ArrayList<>();
            if (json.has("values")) {
                JsonArray array = json.getAsJsonArray("values");
                for (int i = 0; i < array.size(); i++) {
                    JsonObject holder = new JsonObject();
                    holder.add("value", array.get(i));
                    values.add(ScalingValue.fromJson(holder, "value", 0.0));
                }
            }
            return new MessageAction(GsonHelper.getAsString(json, "text"),
                    GsonHelper.getAsBoolean(json, "actionbar", false),
                    values,
                    GsonHelper.getAsBoolean(json, "decimals", false),
                    EntitySelector.fromJson(json, EntitySelector.SELF));
        });
    }
}
