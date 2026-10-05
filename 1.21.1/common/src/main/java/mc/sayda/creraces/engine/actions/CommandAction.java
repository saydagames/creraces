package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Runs a server command as the caster. "@s" and "@t" in the template are replaced with the UUIDs
 * of the caster and the target.
 */
public class CommandAction implements ActionRegistry.RaceAction {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "command");

    private static final int OPERATOR_PERMISSION_LEVEL = 4;

    private final String commandTemplate;
    private final boolean runAsOp;
    private final boolean runAtEntity;

    public CommandAction(String commandTemplate, boolean runAsOp, boolean runAtEntity) {
        this.commandTemplate = commandTemplate;
        this.runAsOp = runAsOp;
        this.runAtEntity = runAtEntity;
    }

    public static void register() {
        ActionRegistry.register(ID, json -> new CommandAction(
                GsonHelper.getAsString(json, "command", ""),
                GsonHelper.getAsBoolean(json, "as_op", false),
                GsonHelper.getAsBoolean(json, "at_entity", false)));
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (player.level().isClientSide() || commandTemplate.isEmpty()) {
            return true;
        }

        // UUIDs rather than names, so a player named "@a" or "@e[...]" can't inject a selector.
        String command = commandTemplate.replace("@s", player.getUUID().toString());
        if (target != null) {
            command = command.replace("@t", target.getUUID().toString());
        }

        CommandSourceStack source = player.createCommandSourceStack();
        if (runAsOp) {
            // as_op never escalates: operators keep level 4, everyone else drops to 0.
            int grantedLevel = source.hasPermission(OPERATOR_PERMISSION_LEVEL) ? OPERATOR_PERMISSION_LEVEL : 0;
            source = source.withPermission(grantedLevel);
        }
        if (runAtEntity) {
            source = source.withPosition(player.position()).withRotation(player.getRotationVector());
        }

        player.getServer().getCommands().performPrefixedCommand(source, command);
        return true;
    }
}
