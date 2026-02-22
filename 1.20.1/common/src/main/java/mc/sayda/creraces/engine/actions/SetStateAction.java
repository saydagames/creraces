package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Action to set race state variables (a1, a2, a3, a4).
 */
public class SetStateAction implements ActionRegistry.RaceAction {

    private final String stateVariable;
    private final double value;
    private final @javax.annotation.Nullable ResourceLocation abilityId;

    public SetStateAction(String stateVariable, double value, @javax.annotation.Nullable ResourceLocation abilityId) {
        this.stateVariable = stateVariable;
        this.value = value;
        this.abilityId = abilityId;
    }

    @Override
    public void execute(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            ResourceLocation targetAbilityId = abilityId;

            if (targetAbilityId == null && "slot".equalsIgnoreCase(stateVariable) && slot != null) {
                targetAbilityId = vars.getAbilityInSlot(slot);
            }

            if (targetAbilityId != null) {
                vars.setAbilityState(targetAbilityId, value);
            }
            mc.sayda.creraces.network.BoundaryHandler.resyncVariables(player, player);
        });
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "set_state"), json -> {
            String state = GsonHelper.getAsString(json, "state", "slot");
            double value = GsonHelper.getAsDouble(json, "value", 0.0);
            String ability = GsonHelper.getAsString(json, "ability", null);
            ResourceLocation abilityLoc = ability != null ? new ResourceLocation(ability) : null;
            return new SetStateAction(state, value, abilityLoc);
        });
    }
}
