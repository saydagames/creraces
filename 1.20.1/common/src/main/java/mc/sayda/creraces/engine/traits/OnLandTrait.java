package mc.sayda.creraces.engine.traits;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Trait that executes actions when a player lands on the ground.
 */
public class OnLandTrait implements TraitRegistry.RaceTrait {

    private final ResourceLocation traitId;
    private final List<ActionRegistry.RaceAction> actions;
    private final Condition condition;

    public OnLandTrait(ResourceLocation traitId, List<ActionRegistry.RaceAction> actions, Condition condition) {
        this.traitId = traitId;
        this.actions = actions;
        this.condition = condition;
    }

    @Override
    public void tick(Player player) {
        if (player.level().isClientSide()) return;
        DataUtils.getVariables(player).ifPresent(vars -> {
            boolean onGround = player.onGround();
            ResourceLocation stateId = new ResourceLocation(traitId.getNamespace(),
                    traitId.getPath() + "_was_on_ground");

            // wasOnGround will be true if we haven't tracked this player yet (to avoid
            // firing on first spawn)
            boolean wasOnGround = vars.getPersistentState(stateId) > 0.5 || !vars.getTraitTimers().containsKey(traitId);

            if (onGround && !wasOnGround) {
                BlockPos pos = player.blockPosition();
                if (condition == null || condition.evaluate(player, null, null, pos)) {
                    CreRaces.LOGGER.debug("OnLandTrait: Firing {} actions for player {}", actions.size(),
                            player.getName().getString());
                    for (ActionRegistry.RaceAction action : actions) {
                        if (!action.execute(player, null, null, pos)) {
                            break;
                        }
                    }
                }
            }

            vars.setPersistentState(stateId, onGround ? 1.0 : 0.0);
            // Mark that we've seen this player
            vars.setTraitTimer(traitId, 1);
        });
    }

    public static void register() {
        TraitRegistry.register(new ResourceLocation(CreRaces.MODID, "on_land"), json -> {
            Condition condition = json.has("condition") ? Condition.fromJson(json.getAsJsonObject("condition")) : null;

            List<ActionRegistry.RaceAction> actions = new ArrayList<>();
            if (json.has("actions") && json.get("actions").isJsonArray()) {
                for (var actionElem : json.getAsJsonArray("actions")) {
                    if (actionElem.isJsonObject()) {
                        ActionRegistry.RaceAction action = ActionRegistry.fromJson(actionElem.getAsJsonObject());
                        if (action != null) {
                            actions.add(action);
                        }
                    }
                }
            }

            String traitName = json.has("name") ? json.get("name").getAsString()
                    : "on_land_" + Math.abs(json.toString().hashCode());
            ResourceLocation traitId = new ResourceLocation(CreRaces.MODID, traitName);

            return new OnLandTrait(traitId, actions, condition);
        });
    }
}
