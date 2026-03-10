package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Action to send a message to the player, either in chat or the action bar.
 */
public class MessageAction implements ActionRegistry.RaceAction {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "message");

    private final String text;
    private final boolean actionbar;

    public MessageAction(String text, boolean actionbar) {
        this.text = text;
        this.actionbar = actionbar;
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        if (text == null || text.isEmpty()) {
            return true;
        }

        Component msg;
        // Detect translation keys: no spaces and no '&' color codes → translatable
        // Literal strings (chat messages with colors/spaces) stay as literal.
        if (!text.contains(" ") && !text.contains("&")) {
            msg = Component.translatable(text);
        } else {
            msg = Component.literal(text.replace("&", "§"));
        }

        player.displayClientMessage(msg, actionbar);
        return true;
    }

    public static void register() {
        ActionRegistry.register(ID, json -> {
            String text = GsonHelper.getAsString(json, "text", "");
            boolean actionbar = GsonHelper.getAsBoolean(json, "actionbar", false);
            return new MessageAction(text, actionbar);
        });
    }
}
