package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.IdRef;
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
 * state is "self" (the casting ability's own state, the default) or a state id.
 */
public class ToggleStateAction implements ActionRegistry.RaceAction {

    private static final double EPSILON = 0.001;

    private final String state;
    private final ScalingValue onValue;
    private final ScalingValue offValue;
    private final List<ActionRegistry.RaceAction> onEnable;
    private final List<ActionRegistry.RaceAction> onDisable;

    public ToggleStateAction(String state, ScalingValue onValue, ScalingValue offValue,
            List<ActionRegistry.RaceAction> onEnable, List<ActionRegistry.RaceAction> onDisable) {
        this.state = state;
        this.onValue = onValue;
        this.offValue = offValue;
        this.onEnable = onEnable;
        this.onDisable = onDisable;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        IPlayerVariables vars = DataUtils.getVariables(player).orElse(null);
        ResourceLocation id = IdRef.resolve(state, player, slot);
        if (vars == null || id == null) {
            return true;
        }

        double on = onValue.evaluate(player, target, slot, interactPos);
        double off = offValue.evaluate(player, target, slot, interactPos);
        // Only an exact "on" counts as on, so anything in between (e.g. left over from a crash) turns it on.
        boolean currentlyOn = Math.abs(vars.getPersistentState(id) - on) < EPSILON;
        vars.setPersistentState(id, currentlyOn ? off : on);
        boolean success = ActionRegistry.runChain(currentlyOn ? onDisable : onEnable, player, target, slot,
                interactPos);
        BoundaryHandler.resyncVariables(player, player);
        return success;
    }

    public static void register() {
        ActionRegistry.register(CreRaces.id("toggle_state"), json -> {
            String state = GsonHelper.getAsString(json, "state", "self");
            if (!state.equals("self") && IdRef.parse(state) == null) {
                throw new IllegalArgumentException("malformed state id '" + state + "'");
            }
            return new ToggleStateAction(state,
                    ScalingValue.fromJson(json, "on_value", 1.0),
                    ScalingValue.fromJson(json, "off_value", 0.0),
                    ActionRegistry.listFromJson(json, "on_enable"),
                    ActionRegistry.listFromJson(json, "on_disable"));
        });
    }
}
