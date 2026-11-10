package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.EventContext;
import mc.sayda.creraces.engine.TraitRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Runs when the player lands, with the block landed on as the "block" position and the height
 * fallen as event:fall_distance.
 */
public class OnLandTrait extends EventTrait {
    private final ResourceLocation trackedId;
    private final ResourceLocation wasOnGroundStateId;
    private final ResourceLocation fallStateId;

    OnLandTrait(JsonObject json) {
        super(json, null);
        ResourceLocation id = TraitIds.of(json);
        this.trackedId = id;
        this.wasOnGroundStateId = id.withSuffix("_was_on_ground");
        this.fallStateId = id.withSuffix("_fall_distance");
    }

    @Override
    public void tick(Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        DataUtils.getVariables(player).ifPresent(vars -> {
            boolean onGround = player.onGround();
            // A player not tracked yet counts as grounded, so nothing fires on the first tick after spawning.
            boolean wasOnGround = vars.getPersistentState(wasOnGroundStateId) > 0.5
                    || !vars.getTraitTimers().containsKey(trackedId);

            if (onGround && !wasOnGround) {
                // Vanilla resets the fall distance on the landing tick, so the last airborne value is used.
                float fallen = (float) vars.getPersistentState(fallStateId);
                fire(player, null, player.getOnPos(), EventContext.ofFall(fallen));
            }
            vars.setPersistentState(fallStateId, onGround ? 0.0 : player.fallDistance);
            vars.setPersistentState(wasOnGroundStateId, onGround ? 1.0 : 0.0);
            // The trait timer only marks this player as tracked.
            vars.setTraitTimer(trackedId, 1);
        });
    }

    public static void register() {
        TraitRegistry.register(CreRaces.id("on_land"), OnLandTrait::new);
    }
}
