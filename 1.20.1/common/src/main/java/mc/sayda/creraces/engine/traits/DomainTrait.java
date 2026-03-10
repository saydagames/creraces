package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonArray;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import mc.sayda.creraces.capability.IPlayerVariables;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Trait that applies actions in a domain around the player.
 */
public class DomainTrait extends PeriodicTrait {

    private final mc.sayda.creraces.engine.ScalingValue radius;
    private final List<ActionRegistry.RaceAction> actions;
    private final Condition condition;

    public DomainTrait(ResourceLocation traitId, mc.sayda.creraces.engine.ScalingValue radius,
            List<ActionRegistry.RaceAction> actions,
            Condition condition, mc.sayda.creraces.engine.ScalingValue interval) {
        super(traitId, interval);
        this.radius = radius;
        this.actions = actions;
        this.condition = condition;
    }

    @Override
    protected boolean shouldExecute(Player player, IPlayerVariables vars) {
        if (player.level().isClientSide())
            return false;
        return condition == null || condition.evaluate(player, null, null, null);
    }

    @Override
    protected void execute(Player player, IPlayerVariables vars) {
        // Apply actions to the player while in their own domain
        for (ActionRegistry.RaceAction action : actions) {
            action.execute(player, null, null, null);
        }

        // Regional effects to OTHERS in the domain
        double r = radius.evaluate(player, null);
        int maxAoeRadius = 100;
        if (maxAoeRadius > 0)
            r = Math.min(r, maxAoeRadius);

        List<Player> others = player.level().getEntitiesOfClass(Player.class,
                Objects.requireNonNull(player.getBoundingBox().inflate(r)),
                p -> p != player);

        for (Player other : others) {
            for (ActionRegistry.RaceAction action : actions) {
                action.execute(player, other, null, null);
            }
        }
    }

    public static void register() {
        TraitRegistry.register(new ResourceLocation(CreRaces.MODID, "domain"), json -> {
            mc.sayda.creraces.engine.ScalingValue radius = mc.sayda.creraces.engine.ScalingValue.fromJson(json,
                    "radius", 10.0);
            mc.sayda.creraces.engine.ScalingValue interval = mc.sayda.creraces.engine.ScalingValue.fromJson(json,
                    "interval", 20.0);
            List<ActionRegistry.RaceAction> actions = new ArrayList<>();
            if (json.has("actions")) {
                JsonArray array = json.getAsJsonArray("actions");
                for (int i = 0; i < array.size(); i++) {
                    actions.add(ActionRegistry.fromJson(array.get(i).getAsJsonObject()));
                }
            }
            Condition condition = null;
            if (json.has("condition")) {
                condition = Condition.fromJson(json.getAsJsonObject("condition"));
            }
            String traitName = json.has("name") ? json.get("name").getAsString()
                    : "domain_" + Math.abs(json.toString().hashCode());
            ResourceLocation traitId = new ResourceLocation(CreRaces.MODID, "trait_" + traitName);

            return new DomainTrait(traitId, radius, actions, condition, interval);
        });
    }

}
