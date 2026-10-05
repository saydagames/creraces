package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Flips a persistent state between on_value and off_value, running on_enable or on_disable. The
 * state is "self" (the casting ability's own state) or a state id.
 */
public class ToggleStateAction implements ActionRegistry.RaceAction {

    private static final double EPSILON = 0.001;

    private final String stateVariable;
    @Nullable
    private final ResourceLocation stateId;
    private final ScalingValue onValue;
    private final ScalingValue offValue;
    private final List<ActionRegistry.RaceAction> onEnable;
    private final List<ActionRegistry.RaceAction> onDisable;

    public ToggleStateAction(String stateVariable, @Nullable ResourceLocation stateId, ScalingValue onValue,
            ScalingValue offValue, List<ActionRegistry.RaceAction> onEnable, List<ActionRegistry.RaceAction> onDisable) {
        this.stateVariable = stateVariable;
        this.stateId = stateId;
        this.onValue = onValue;
        this.offValue = offValue;
        this.onEnable = onEnable;
        this.onDisable = onDisable;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        return DataUtils.getVariables(player)
                .map(vars -> toggle(vars, player, target, slot, interactPos))
                .orElse(true);
    }

    private boolean toggle(IPlayerVariables vars, Player player, @Nullable LivingEntity target,
            @Nullable AbilitySlot slot, @Nullable BlockPos interactPos) {
        ResourceLocation id = stateId;
        if (id == null && "self".equalsIgnoreCase(stateVariable) && slot != null) {
            id = vars.getAbilityInSlot(slot);
        } else if (id == null) {
            CreRaces.LOGGER.warn("ToggleStateAction: could not resolve state '{}': no slot context and no valid "
                    + "resource location", stateVariable);
        }
        if (id == null) {
            return true;
        }

        double on = onValue.evaluate(player, target, slot);
        double off = offValue.evaluate(player, target, slot);
        // Only an exact "on" counts as on, so anything in between (e.g. left over from a crash) turns it on.
        boolean currentlyOn = Math.abs(vars.getPersistentState(id) - on) < EPSILON;
        vars.setPersistentState(id, currentlyOn ? off : on);
        boolean success = ActionRegistry.runChain(currentlyOn ? onDisable : onEnable, player, target, slot,
                interactPos);
        BoundaryHandler.resyncVariables(player, player);
        return success;
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "toggle_state"), json -> {
            String state = GsonHelper.getAsString(json, "state", "self");
            ResourceLocation stateId = null;
            if (!"self".equalsIgnoreCase(state)) {
                String id = state.startsWith("state:") ? state.substring("state:".length()) : state;
                stateId = ResourceLocation.tryParse(id.contains(":") ? id : CreRaces.MODID + ":" + id);
            }
            return new ToggleStateAction(state, stateId,
                    ScalingValue.fromJson(json, "on_value", 1.0),
                    ScalingValue.fromJson(json, "off_value", 0.0),
                    ActionRegistry.listFromJson(json, "on_enable"),
                    ActionRegistry.listFromJson(json, "on_disable"));
        });
    }
}
