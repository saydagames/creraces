package mc.sayda.creraces.engine.traits;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import mc.sayda.creraces.race.ResourceType;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import mc.sayda.creraces.engine.ScalingValue;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import mc.sayda.creraces.engine.ActionRegistry;

/**
 * Trait that allows flight while draining a resource.
 * Can be conditional (e.g., only while Foxfire is active).
 */
public class FlightTrait implements TraitRegistry.RaceTrait {
    private final ResourceLocation traitId;
    private final ResourceType resource;
    private final ScalingValue drainRate;
    private final boolean forceFly;
    @Nullable
    private final Condition condition;
    private final List<ActionRegistry.RaceAction> onFail;

    public FlightTrait(ResourceLocation traitId, ResourceType resource, ScalingValue drainRate, boolean forceFly,
            @Nullable Condition condition, List<ActionRegistry.RaceAction> onFail) {
        this.traitId = traitId;
        this.resource = resource;
        this.drainRate = drainRate;
        this.forceFly = forceFly;
        this.condition = condition;
        this.onFail = onFail;
    }

    @Override
    public void tick(Player player) {
        boolean conditionMet = condition == null || condition.evaluate(player, null, null, null);

        DataUtils.getVariables(player).ifPresent(vars -> {
            double currentResource = switch (resource) {
                case MANA -> vars.getMana();
                case ENERGY -> vars.getEnergy();
                case GRIT -> vars.getGrit();
                case RAGE -> vars.getRage();
                case SOULS -> vars.getSouls();
                case NONE -> 0.0;
                default -> 100.0;
            };

            double evaluatedDrain = drainRate.evaluate(player);
            boolean canFly = conditionMet && currentResource > evaluatedDrain;
            boolean wasMayfly = player.getAbilities().mayfly;
            boolean wasFlying = player.getAbilities().flying;

            if (canFly) {
                player.getAbilities().mayfly = true;

                // Force flying state if enabled
                if (forceFly) {
                    player.getAbilities().flying = true;
                }

                if (player.getAbilities().flying) {
                    // Drain resource
                    switch (resource) {
                        case MANA -> vars.setMana(Math.max(0, vars.getMana() - evaluatedDrain));
                        case ENERGY -> vars.setEnergy(Math.max(0, vars.getEnergy() - evaluatedDrain));
                        case GRIT -> vars.setGrit(Math.max(0, vars.getGrit() - evaluatedDrain));
                        case RAGE -> vars.setRage(Math.max(0, vars.getRage() - evaluatedDrain));
                        case SOULS -> vars.setSouls(Math.max(0, vars.getSouls() - evaluatedDrain));
                        case NONE -> {
                        } // No resource to drain
                    }
                }
            } else {
                // Fallback case: condition not met or resource exhausted
                if (!player.isCreative() && !player.isSpectator()) {
                    player.getAbilities().mayfly = false;
                    if (player.getAbilities().flying) {
                        player.getAbilities().flying = false;
                    }

                    // On Fail Logic
                    ResourceLocation failId = new ResourceLocation(traitId.getNamespace(),
                            traitId.getPath() + "_failed");
                    boolean alreadyFailed = vars.getAbilityState(failId) > 0;
                    if (conditionMet && currentResource < evaluatedDrain && !alreadyFailed) {
                        vars.setAbilityState(failId, 1.0);
                        for (ActionRegistry.RaceAction action : onFail) {
                            action.execute(player, null, null, null);
                        }
                    }
                }
            }

            if (conditionMet && currentResource >= evaluatedDrain) {
                ResourceLocation failId = new ResourceLocation(traitId.getNamespace(), traitId.getPath() + "_failed");
                vars.setAbilityState(failId, 0.0);
            }

            // Sync if changed
            if (player.getAbilities().mayfly != wasMayfly || player.getAbilities().flying != wasFlying) {
                player.onUpdateAbilities();
            }
        });
    }

    public static void register() {
        TraitRegistry.register(new ResourceLocation(CreRaces.MODID, "flight"), json -> {
            String resStr = GsonHelper.getAsString(json, "resource", "NONE");
            if (resStr.contains(":"))
                resStr = resStr.substring(resStr.indexOf(':') + 1);
            ResourceType resource = ResourceType.valueOf(resStr.toUpperCase());
            ScalingValue drainRate = ScalingValue.fromJson(json, "drain_rate", 0.0);
            boolean forceFly = GsonHelper.getAsBoolean(json, "force_fly", false);

            Condition condition = null;
            if (json.has("condition")) {
                condition = Condition.fromJson(json.getAsJsonObject("condition"));
            }

            List<ActionRegistry.RaceAction> onFail = new ArrayList<>();
            if (json.has("on_fail") && json.get("on_fail").isJsonArray()) {
                for (var actionElem : json.getAsJsonArray("on_fail")) {
                    if (actionElem.isJsonObject()) {
                        ActionRegistry.RaceAction action = ActionRegistry.fromJson(actionElem.getAsJsonObject());
                        if (action != null) {
                            onFail.add(action);
                        }
                    }
                }
            }

            String traitName = json.has("name") ? json.get("name").getAsString()
                    : "flight_" + Math.abs(json.toString().hashCode());
            ResourceLocation traitId = new ResourceLocation(CreRaces.MODID, "trait_" + traitName);

            return new FlightTrait(traitId, resource, drainRate, forceFly, condition, onFail);
        });
    }
}
