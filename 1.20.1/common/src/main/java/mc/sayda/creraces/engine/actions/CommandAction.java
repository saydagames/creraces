package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.util.GsonHelper;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.commands.CommandSourceStack;

/**
 * Executes a server-side command as if the player (or server) ran it.
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
    public boolean execute(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        if (player.level().isClientSide() || commandTemplate.isEmpty())
            return true;

        // Use UUID strings instead of player name to prevent selector injection
        // (a player named "@a" or "@e[...]" could manipulate the command otherwise)
        String command = commandTemplate.replace("@s", player.getUUID().toString());
        if (target != null) {
            command = command.replace("@t", target.getUUID().toString());
        }

        CommandSourceStack source = player.createCommandSourceStack();
        if (runAsOp) {
            // Only escalate to level 4 if the player is already an operator.
            // Non-OP players get no permission escalation from run_as_op.
            int grantedLevel = source.hasPermission(4) ? 4 : 0;
            source = source.withPermission(grantedLevel);
        }

        if (runAtEntity) {
            source = source.withPosition(player.position())
                    .withRotation(player.getRotationVector());
        }

        player.getServer().getCommands().performPrefixedCommand(source, command);
        return true;
    }
}
