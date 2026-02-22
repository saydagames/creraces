package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.commands.CommandSourceStack;

/**
 * Executes a server-side command as if the player (or server) ran it.
 * Highly configurable via JSON.
 */
public class CommandAction implements ActionRegistry.RaceAction {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "command");

    private final String commandTemplate;
    private final boolean runAsOp;
    private final boolean runAtEntity;

    public CommandAction(String commandTemplate, boolean runAsOp, boolean runAtEntity) {
        this.commandTemplate = commandTemplate;
        this.runAsOp = runAsOp;
        this.runAtEntity = runAtEntity;
    }

    public static void register() {
        ActionRegistry.register(ID, data -> {
            String command = GsonHelper.getAsString(data, "command", "");
            boolean asOp = GsonHelper.getAsBoolean(data, "as_op", false);
            boolean atEntity = GsonHelper.getAsBoolean(data, "at_entity", false);
            return new CommandAction(command, asOp, atEntity);
        });
    }

    @Override
    public void execute(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
        if (player.level().isClientSide() || commandTemplate.isEmpty())
            return;

        String command = commandTemplate.replace("@s", player.getGameProfile().getName());
        if (target != null) {
            command = command.replace("@t", target.getUUID().toString());
        }

        CommandSourceStack source = player.createCommandSourceStack();
        if (runAsOp) {
            source = source.withPermission(4); // Run with high permission
        }

        if (runAtEntity) {
            source = source.withPosition(player.position())
                    .withRotation(player.getRotationVector());
        }

        player.getServer().getCommands().performPrefixedCommand(source, command);
    }
}
