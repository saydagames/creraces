package mc.sayda.creraces.engine.traits;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.List;

/** Runs actions when the player lands after being airborne, with the landing block as the interact position. */
public class OnLandTrait implements TraitRegistry.RaceTrait {

    private final ResourceLocation traitId;
    private final ResourceLocation wasOnGroundStateId;
    private final List<ActionRegistry.RaceAction> actions;
    @Nullable
    private final Condition condition;

    public OnLandTrait(ResourceLocation traitId, List<ActionRegistry.RaceAction> actions, @Nullable Condition condition) {
        this.traitId = traitId;
        this.wasOnGroundStateId = traitId.withSuffix("_was_on_ground");
        this.actions = actions;
        this.condition = condition;
    }

    @Override
    public void tick(Player player) {
        if (player.level().isClientSide()) return;
        DataUtils.getVariables(player).ifPresent(vars -> {
            boolean onGround = player.onGround();

            // A player not tracked yet counts as grounded, so nothing fires on the first tick after spawning.
            boolean wasOnGround = vars.getPersistentState(wasOnGroundStateId) > 0.5
                    || !vars.getTraitTimers().containsKey(traitId);

            if (onGround && !wasOnGround) {
                BlockPos pos = player.blockPosition();
                if (condition == null || condition.evaluate(player, null, null, pos)) {
                    CreRaces.LOGGER.debug("OnLandTrait: Firing {} actions for player {}", actions.size(),
                            player.getName().getString());
                    ActionRegistry.runChain(actions, player, null, null, pos);
                }
            }

            vars.setPersistentState(wasOnGroundStateId, onGround ? 1.0 : 0.0);
            // The trait timer only marks this player as tracked.
            vars.setTraitTimer(traitId, 1);
        });
    }

    public static void register() {
        TraitRegistry.register(new ResourceLocation(CreRaces.MODID, "on_land"), json -> {
            Condition condition = json.has("condition") ? Condition.fromJson(json.getAsJsonObject("condition")) : null;
            return new OnLandTrait(TraitIds.fromJson(json, "on_land_"), ActionRegistry.listFromJson(json, "actions"),
                    condition);
        });
    }
}
