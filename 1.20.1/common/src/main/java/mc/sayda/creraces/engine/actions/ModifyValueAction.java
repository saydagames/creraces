package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.EntitySelector;
import mc.sayda.creraces.engine.IdRef;
import mc.sayda.creraces.engine.Resources;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Changes a stored number of the caster (or of the target with "entity": "target"). The number is
 * named by its kind: "resource" (see Resources), "state" or "cooldown" (an id, or "self" for the
 * casting ability; cooldowns are in ticks). "operation" is set (default), add or multiply by "value".
 * With "fail_if_insufficient", a result below 0 fails the action and changes nothing, which is how
 * costs are paid. "persistent" keeps a state across deaths.
 */
@SuppressWarnings("null")
public class ModifyValueAction implements ActionRegistry.RaceAction {
    private enum Operation { SET, ADD, MULTIPLY }

    private enum Kind { RESOURCE, STATE, COOLDOWN }

    private final Kind kind;
    /** The resource name, or the state or cooldown id as written. */
    private final String name;
    @Nullable
    private final Resources.Resource resource;
    private final ScalingValue value;
    private final Operation operation;
    private final boolean persistent;
    private final boolean failIfInsufficient;
    private final EntitySelector entity;

    private ModifyValueAction(Kind kind, String name, @Nullable Resources.Resource resource, ScalingValue value,
            Operation operation, boolean persistent, boolean failIfInsufficient, EntitySelector entity) {
        this.kind = kind;
        this.name = name;
        this.resource = resource;
        this.value = value;
        this.operation = operation;
        this.persistent = persistent;
        this.failIfInsufficient = failIfInsufficient;
        this.entity = entity;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        LivingEntity subject = entity.resolve(player, target);
        if (subject == null) {
            return true;
        }
        double incoming = value.evaluate(player, target, slot, interactPos);

        if (resource != null) {
            double next = apply(resource.read(subject), incoming);
            if (failIfInsufficient && next < 0) {
                return false;
            }
            resource.write(subject, next);
            return true;
        }

        // States and cooldowns belong to players; the id's "self" is the caster's casting ability.
        ResourceLocation id = IdRef.resolve(name, player, slot);
        IPlayerVariables vars = subject instanceof Player subjectPlayer
                ? DataUtils.getVariables(subjectPlayer).orElse(null) : null;
        if (id == null || vars == null) {
            return true;
        }
        double current = kind == Kind.STATE ? vars.getPersistentState(id) : vars.getCooldown(id);
        double next = apply(current, incoming);
        if (failIfInsufficient && next < 0) {
            return false;
        }
        if (kind == Kind.STATE) {
            vars.setPersistentState(id, next);
            if (persistent) {
                vars.setStatePersistent(id, true);
            }
        } else {
            vars.setCooldown(id, (int) Math.max(0, next));
        }
        BoundaryHandler.resyncVariables((Player) subject, (Player) subject);
        return true;
    }

    private double apply(double current, double incoming) {
        return switch (operation) {
            case SET -> incoming;
            case ADD -> current + incoming;
            case MULTIPLY -> current * incoming;
        };
    }

    public static void register() {
        ActionRegistry.register(CreRaces.id("modify_value"), json -> {
            Kind kind = kind(json);
            String name = GsonHelper.getAsString(json, kind.name().toLowerCase());
            Resources.Resource resource = null;
            if (kind == Kind.RESOURCE) {
                resource = Resources.get(name);
                if (resource == null) {
                    throw new IllegalArgumentException("unknown resource '" + name + "'; known: " + Resources.knownNames());
                }
            } else if (!name.equals("self") && IdRef.parse(name) == null) {
                throw new IllegalArgumentException("malformed id '" + name + "'");
            }
            String operationName = GsonHelper.getAsString(json, "operation", "set");
            Operation operation = switch (operationName) {
                case "set" -> Operation.SET;
                case "add" -> Operation.ADD;
                case "multiply" -> Operation.MULTIPLY;
                default -> throw new IllegalArgumentException("unknown operation '" + operationName + "' (set, add, multiply)");
            };
            return new ModifyValueAction(kind, name, resource,
                    ScalingValue.fromJson(json, "value", 0.0),
                    operation,
                    GsonHelper.getAsBoolean(json, "persistent", false),
                    GsonHelper.getAsBoolean(json, "fail_if_insufficient", false),
                    EntitySelector.fromJson(json, EntitySelector.SELF));
        });
    }

    /** Exactly one of "resource", "state" and "cooldown" names the number. */
    private static Kind kind(JsonObject json) {
        Kind found = null;
        for (Kind kind : Kind.values()) {
            if (json.has(kind.name().toLowerCase())) {
                if (found != null) {
                    throw new IllegalArgumentException("give only one of \"resource\", \"state\" and \"cooldown\"");
                }
                found = kind;
            }
        }
        if (found == null) {
            throw new IllegalArgumentException("needs \"resource\", \"state\" or \"cooldown\"");
        }
        return found;
    }
}
