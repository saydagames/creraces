package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Action to send a message to the player, either in chat or the action bar.
 */
public class MessageAction implements ActionRegistry.RaceAction {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "message");

    private final String text;
    private final boolean actionbar;
    private final ScalingValue value;

    public MessageAction(String text, boolean actionbar, ScalingValue value) {
        this.text = text;
        this.actionbar = actionbar;
        this.value = value;
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        if (text == null || text.isEmpty()) {
            return true;
        }

        double evaluated = value != null ? value.evaluate(player, target, slot) : 0;
        Object arg = (evaluated == (int) evaluated) ? (int) evaluated : evaluated;

        Component msg;
        // Detect translation keys: no spaces and no '&' color codes -> translatable
        if (!text.contains(" ") && !text.contains("&")) {
            if (value != null) {
                msg = Component.translatable(text, arg);
            } else {
                msg = Component.translatable(text);
            }
        } else {
            String processed = text.replace("&", "§");
            if (value != null) {
                try {
                    if (processed.contains("%s") || processed.contains("%d") || processed.contains("%.0f")) {
                        processed = String.format(processed, arg);
                    } else {
                        processed += arg.toString();
                    }
                } catch (Exception ignored) {
                }
            }
            msg = Component.literal(processed);
        }

        player.displayClientMessage(msg, actionbar);
        return true;
    }

    public static void register() {
        ActionRegistry.register(ID, json -> {
            String text = GsonHelper.getAsString(json, "text", "");
            boolean actionbar = GsonHelper.getAsBoolean(json, "actionbar", false);
            ScalingValue value = json.has("value") ? ScalingValue.fromJson(json, "value", 0) : null;
            return new MessageAction(text, actionbar, value);
        });
    }
}
