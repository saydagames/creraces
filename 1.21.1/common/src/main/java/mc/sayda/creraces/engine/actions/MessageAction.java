package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.IllegalFormatException;

/**
 * Shows the caster a message in chat or on the action bar. Text without spaces or '&' is treated as
 * a translation key; anything else is literal text with '&' colour codes. An optional value is
 * passed as the translation argument, formatted into the literal text, or appended to it. It is
 * rounded to a whole number unless "decimals" is set.
 */
public class MessageAction implements ActionRegistry.RaceAction {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "message");

    private final String text;
    private final boolean actionbar;
    @Nullable
    private final ScalingValue value;
    private final boolean decimals;

    public MessageAction(String text, boolean actionbar, @Nullable ScalingValue value, boolean decimals) {
        this.text = text;
        this.actionbar = actionbar;
        this.value = value;
        this.decimals = decimals;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (text.isEmpty()) {
            return true;
        }
        player.displayClientMessage(buildMessage(player, target, slot), actionbar);
        return true;
    }

    private Component buildMessage(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot) {
        boolean translatable = !text.contains(" ") && !text.contains("&");
        if (value == null) {
            return translatable ? Component.translatable(text)
                    : Component.literal(text.replace('&', ChatFormatting.PREFIX_CODE));
        }

        double evaluated = value.evaluate(player, target, slot);
        if (translatable) {
            return Component.translatable(text, boxed(evaluated, decimals));
        }

        String literal = text.replace('&', ChatFormatting.PREFIX_CODE);
        if (literal.contains("%s") || literal.contains("%d") || literal.contains("%.0f")) {
            // %d only accepts an integer and %.0f only a double, whatever "decimals" says.
            boolean asDouble = !literal.contains("%d") && (literal.contains("%.0f") || decimals);
            try {
                literal = String.format(literal, boxed(evaluated, asDouble));
            } catch (IllegalFormatException e) {
                // A format that doesn't fit the argument is shown as written.
            }
        } else {
            literal += boxed(evaluated, decimals);
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
        ActionRegistry.register(ID, json -> new MessageAction(
                GsonHelper.getAsString(json, "text", ""),
                GsonHelper.getAsBoolean(json, "actionbar", false),
                json.has("value") ? ScalingValue.fromJson(json, "value", 0) : null,
                GsonHelper.getAsBoolean(json, "decimals", false)));
    }
}
